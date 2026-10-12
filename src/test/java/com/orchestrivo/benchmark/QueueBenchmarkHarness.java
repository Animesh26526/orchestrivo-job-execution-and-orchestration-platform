package com.orchestrivo.benchmark;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.orchestrivo.domain.ProcessSpec;
import com.orchestrivo.domain.Workload;
import com.orchestrivo.model.ExecutionJobRecord;
import com.orchestrivo.model.ExecutionJobStatus;
import com.orchestrivo.model.User;
import com.orchestrivo.model.WorkloadRecord;
import com.orchestrivo.persistence.*;
import com.orchestrivo.service.AuditService;
import com.orchestrivo.service.ExecutionJobService;
import com.orchestrivo.service.ExecutionWorkerManager;

import javax.sql.DataSource;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Repeatable, evidence-backed benchmark harness for queue orchestration,
 * enqueue latency/throughput, worker claiming, and batch drain performance.
 */
public class QueueBenchmarkHarness {

    private final DataSource dataSource;
    private final ExecutionJobRepository jobRepository;
    private final ExperimentRunRepository experimentRunRepository;
    private final UserRepository userRepository;
    private final WorkloadRepository workloadRepository;
    private final AuditService auditService;
    private final ExecutionJobService jobService;

    private User benchmarkUser;
    private WorkloadRecord benchmarkWorkload;

    public QueueBenchmarkHarness() {
        this.dataSource = DataSourceManager.getDataSource();
        this.jobRepository = new ExecutionJobRepositoryJdbcImpl(dataSource);
        this.experimentRunRepository = new ExperimentRunRepositoryJdbcImpl();
        this.userRepository = new UserRepositoryJdbcImpl();
        this.workloadRepository = new WorkloadRepositoryJdbcImpl();
        this.auditService = new AuditService(new AuditLogRepositoryJdbcImpl());
        this.jobService = new ExecutionJobService(this.jobRepository);
    }

    public void initialize() {
        try (Connection conn = dataSource.getConnection()) {
            User u = new User();
            u.setEmail("bench_harness_" + System.currentTimeMillis() + "@example.com");
            u.setPasswordHash("hash_secure_bench");
            u.setFullName("Benchmark Harness User");
            u.setRole("ADMIN");
            u.setActive(true);
            this.benchmarkUser = userRepository.save(u);

            WorkloadRecord w = new WorkloadRecord();
            w.setOwnerUserId(benchmarkUser.getId());
            w.setName("Benchmark 5-Process Standard Workload");
            w.setDescription("Controlled reproducible benchmark workload (seed: 42)");
            w.setSourceType("MANUAL");
            w.setGenerationSeed(42L);
            List<ProcessSpec> specs = List.of(
                    new ProcessSpec("P1", 0, 8),
                    new ProcessSpec("P2", 1, 4),
                    new ProcessSpec("P3", 2, 9),
                    new ProcessSpec("P4", 3, 5),
                    new ProcessSpec("P5", 4, 2)
            );
            w.setWorkload(new Workload(specs));
            this.benchmarkWorkload = workloadRepository.save(w);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize benchmark harness fixtures", e);
        }
    }

    public static class EnvironmentMetadata {
        public String javaVersion;
        public String javaVendor;
        public String javaVm;
        public String osName;
        public String osArch;
        public int availableProcessors;
        public long maxHeapMb;
        public String mysqlVersion;
        public String commitHash;
        public String timestamp;

        public static EnvironmentMetadata capture(DataSource ds, String commitHash) {
            EnvironmentMetadata env = new EnvironmentMetadata();
            env.javaVersion = System.getProperty("java.version", "unknown");
            env.javaVendor = System.getProperty("java.vendor", "unknown");
            env.javaVm = System.getProperty("java.vm.name", "unknown") + " " + System.getProperty("java.vm.version", "");
            env.osName = System.getProperty("os.name", "unknown") + " " + System.getProperty("os.version", "");
            env.osArch = System.getProperty("os.arch", "unknown");
            env.availableProcessors = Runtime.getRuntime().availableProcessors();
            env.maxHeapMb = Runtime.getRuntime().maxMemory() / (1024 * 1024);
            env.commitHash = commitHash;
            env.timestamp = Instant.now().toString();

            try (Connection conn = ds.getConnection()) {
                DatabaseMetaData meta = conn.getMetaData();
                env.mysqlVersion = meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion();
            } catch (Exception e) {
                env.mysqlVersion = "MySQL (version query failed: " + e.getMessage() + ")";
            }
            return env;
        }
    }

    public static class SingleJobSample {
        public long jobId;
        public long enqueueLatencyNanos;
        public long queueWaitMs;
        public long executionDurationNanos;
        public long endToEndDurationNanos;
        public String status;
    }

    public static class BatchTrialResult {
        public String trialId;
        public String cohortBatchId;
        public int workerCount;
        public int batchSize;
        public boolean isWarmup;
        public boolean timedOut;
        public boolean isValid;

        public long enqueueDurationNanos;
        public double enqueueThroughputJobsPerSec;
        public double enqueueP50LatencyMs;
        public double enqueueP95LatencyMs;
        public double enqueueMeanLatencyMs;

        public long drainDurationNanos;
        public double drainThroughputJobsPerSec;
        public double queueWaitP50Ms;
        public double executionDurationP50Us;
        public double endToEndP50Ms;
        public double endToEndP95Ms;

        public int completedCount;
        public int failedCount;
        public int unhandledCount;

        public List<SingleJobSample> samples = new ArrayList<>();
    }

    public static class MatrixConfigurationResult {
        public int workerCount;
        public int batchSize;
        public int warmupTrials;
        public int measuredTrials;

        public double medianEnqueueThroughputJobsPerSec;
        public double medianDrainThroughputJobsPerSec;
        public double medianDrainDurationMs;
        public double p50EndToEndLatencyMs;
        public double p95EndToEndLatencyMs;
        public double p50ExecutionLatencyUs;
        public double p50QueueWaitMs;

        public List<BatchTrialResult> trials = new ArrayList<>();
    }

    public static class BenchmarkSuiteReport {
        public String suiteLabel; // "BASELINE" or "CANDIDATE"
        public EnvironmentMetadata environment;
        public List<MatrixConfigurationResult> configurations = new ArrayList<>();
    }

    /**
     * Executes a single batch drain trial with explicit cohort isolation.
     */
    public BatchTrialResult runTrial(int workerCount, int batchSize, boolean isWarmup, long timeoutSeconds) {
        BatchTrialResult result = new BatchTrialResult();
        result.trialId = UUID.randomUUID().toString();
        result.cohortBatchId = "cohort-" + (isWarmup ? "warmup-" : "meas-") + UUID.randomUUID().toString().substring(0, 8);
        result.workerCount = workerCount;
        result.batchSize = batchSize;
        result.isWarmup = isWarmup;

        List<Long> cohortJobIds = new ArrayList<>();
        List<Long> enqueueDurationsNanos = new ArrayList<>();

        // 1. Enqueue cohort jobs
        long enqueueStart = System.nanoTime();
        for (int i = 0; i < batchSize; i++) {
            long jobStart = System.nanoTime();
            String idemKey = result.cohortBatchId + "-job-" + i;

            ExecutionJobRecord record = new ExecutionJobRecord();
            record.setOwnerUserId(benchmarkUser.getId());
            record.setWorkloadId(benchmarkWorkload.getId());
            record.setAlgorithmName("FCFS");
            record.setAlgorithmParameters("{}");
            record.setInputSnapshot(com.orchestrivo.model.WorkloadSnapshot.serialize(benchmarkWorkload.getWorkload(), 42L));
            record.setStatus(ExecutionJobStatus.QUEUED);
            record.setAttemptCount(0);
            record.setMaxAttempts(3);
            record.setBatchId(result.cohortBatchId);
            record.setIdempotencyKey(idemKey);

            ExecutionJobRecord saved = jobRepository.save(record);
            long jobDuration = System.nanoTime() - jobStart;

            cohortJobIds.add(saved.getId());
            enqueueDurationsNanos.add(jobDuration);
        }
        long enqueueEnd = System.nanoTime();
        result.enqueueDurationNanos = enqueueEnd - enqueueStart;
        result.enqueueThroughputJobsPerSec = (batchSize / (double) result.enqueueDurationNanos) * 1_000_000_000.0;

        Collections.sort(enqueueDurationsNanos);
        result.enqueueP50LatencyMs = computeMedian(enqueueDurationsNanos) / 1_000_000.0;
        result.enqueueP95LatencyMs = computePercentile(enqueueDurationsNanos, 0.95) / 1_000_000.0;
        result.enqueueMeanLatencyMs = computeMean(enqueueDurationsNanos) / 1_000_000.0;

        // 2. Start worker manager and drain cohort
        ExecutionWorkerManager manager = new ExecutionWorkerManager(
                jobRepository,
                experimentRunRepository,
                auditService,
                workerCount
        );

        long drainStart = System.nanoTime();
        manager.start();

        long timeoutNanos = TimeUnit.SECONDS.toNanos(timeoutSeconds);
        long deadlineNanos = drainStart + timeoutNanos;

        // Wait until all cohort jobs reach terminal state
        boolean allDrained = false;
        while (System.nanoTime() < deadlineNanos) {
            int pending = countPendingCohortJobs(result.cohortBatchId);
            if (pending == 0) {
                allDrained = true;
                break;
            }
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        long drainEnd = System.nanoTime();
        manager.shutdown();

        result.drainDurationNanos = drainEnd - drainStart;
        result.timedOut = !allDrained;
        result.isValid = allDrained;

        // 3. Inspect individual cohort records from MySQL
        List<SingleJobSample> samples = loadCohortSamples(result.cohortBatchId, enqueueStart);
        result.samples = samples;

        int completed = 0;
        int failed = 0;
        int unhandled = 0;
        List<Long> e2eNanos = new ArrayList<>();
        List<Long> execNanos = new ArrayList<>();
        List<Long> waitMsList = new ArrayList<>();

        for (SingleJobSample s : samples) {
            if ("COMPLETED".equalsIgnoreCase(s.status)) {
                completed++;
                e2eNanos.add(s.endToEndDurationNanos);
                execNanos.add(s.executionDurationNanos);
                waitMsList.add(s.queueWaitMs);
            } else if ("FAILED".equalsIgnoreCase(s.status)) {
                failed++;
            } else {
                unhandled++;
            }
        }

        result.completedCount = completed;
        result.failedCount = failed;
        result.unhandledCount = unhandled;

        if (allDrained && completed > 0) {
            result.drainThroughputJobsPerSec = (completed / (double) result.drainDurationNanos) * 1_000_000_000.0;
            Collections.sort(e2eNanos);
            Collections.sort(execNanos);
            Collections.sort(waitMsList);

            result.endToEndP50Ms = computeMedian(e2eNanos) / 1_000_000.0;
            result.endToEndP95Ms = computePercentile(e2eNanos, 0.95) / 1_000_000.0;
            result.executionDurationP50Us = computeMedian(execNanos) / 1_000.0;
            result.queueWaitP50Ms = computeMedian(waitMsList);
        } else {
            result.drainThroughputJobsPerSec = 0.0;
            result.isValid = false;
        }

        return result;
    }

    private int countPendingCohortJobs(String batchId) {
        String sql = "SELECT COUNT(*) FROM execution_jobs WHERE batch_id = ? AND status IN ('QUEUED', 'RUNNING')";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, batchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error counting cohort jobs", e);
        }
        return 0;
    }

    private List<SingleJobSample> loadCohortSamples(String batchId, long batchStartNanos) {
        String sql = "SELECT id, status, queue_wait_ms, execution_duration_nanos, execution_duration_ms, " +
                "TIMESTAMPDIFF(MICROSECOND, created_at, IFNULL(completed_at, NOW())) AS elapsed_micros " +
                "FROM execution_jobs WHERE batch_id = ? ORDER BY id ASC";

        List<SingleJobSample> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, batchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SingleJobSample s = new SingleJobSample();
                    s.jobId = rs.getLong("id");
                    s.status = rs.getString("status");
                    s.queueWaitMs = rs.getLong("queue_wait_ms");
                    long nanos = rs.getLong("execution_duration_nanos");
                    if (nanos <= 0) {
                        nanos = rs.getLong("execution_duration_ms") * 1_000_000L;
                    }
                    s.executionDurationNanos = nanos;
                    long elapsedMicros = rs.getLong("elapsed_micros");
                    s.endToEndDurationNanos = elapsedMicros * 1_000L;
                    list.add(s);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error loading cohort samples", e);
        }
        return list;
    }

    /**
     * Executes the comprehensive benchmark matrix:
     * Combinations of workerCounts and batchSizes, with warmup and repeated trials.
     */
    public BenchmarkSuiteReport runSuite(String suiteLabel, int[] workerCounts, int[] batchSizes,
                                         int warmupTrials, int measuredTrials, String commitHash) {
        BenchmarkSuiteReport report = new BenchmarkSuiteReport();
        report.suiteLabel = suiteLabel;
        report.environment = EnvironmentMetadata.capture(dataSource, commitHash);

        System.out.println("==========================================================================");
        System.out.println("       RUNNING ORCHESTRIVO BENCHMARK SUITE: " + suiteLabel);
        System.out.println("==========================================================================");
        System.out.println("  Java: " + report.environment.javaVersion + " (" + report.environment.javaVendor + ")");
        System.out.println("  OS: " + report.environment.osName + " (" + report.environment.osArch + ")");
        System.out.println("  Processors: " + report.environment.availableProcessors);
        System.out.println("  MySQL: " + report.environment.mysqlVersion);
        System.out.println("  Commit: " + report.environment.commitHash);
        System.out.println("==========================================================================\n");

        for (int workers : workerCounts) {
            for (int batchSize : batchSizes) {
                System.out.printf(">>> Benchmarking [Workers=%d, BatchSize=%d] (%d warmup, %d measured)...%n",
                        workers, batchSize, warmupTrials, measuredTrials);

                MatrixConfigurationResult configResult = new MatrixConfigurationResult();
                configResult.workerCount = workers;
                configResult.batchSize = batchSize;
                configResult.warmupTrials = warmupTrials;
                configResult.measuredTrials = measuredTrials;

                // 1. Warm-up trials
                for (int w = 1; w <= warmupTrials; w++) {
                    BatchTrialResult warmResult = runTrial(workers, batchSize, true, 45);
                    System.out.printf("    [Warmup %d/%d] Drain: %.2f ms | Throughput: %.2f jobs/s | Valid: %b%n",
                            w, warmupTrials, warmResult.drainDurationNanos / 1_000_000.0, warmResult.drainThroughputJobsPerSec, warmResult.isValid);
                }

                // 2. Measured trials
                List<Double> enqThroughputs = new ArrayList<>();
                List<Double> drainThroughputs = new ArrayList<>();
                List<Double> drainDurationsMs = new ArrayList<>();
                List<Double> e2eP50s = new ArrayList<>();
                List<Double> e2eP95s = new ArrayList<>();
                List<Double> execP50s = new ArrayList<>();
                List<Double> waitP50s = new ArrayList<>();

                for (int m = 1; m <= measuredTrials; m++) {
                    BatchTrialResult trial = runTrial(workers, batchSize, false, 45);
                    configResult.trials.add(trial);

                    if (trial.isValid && trial.completedCount == batchSize) {
                        enqThroughputs.add(trial.enqueueThroughputJobsPerSec);
                        drainThroughputs.add(trial.drainThroughputJobsPerSec);
                        drainDurationsMs.add(trial.drainDurationNanos / 1_000_000.0);
                        e2eP50s.add(trial.endToEndP50Ms);
                        e2eP95s.add(trial.endToEndP95Ms);
                        execP50s.add(trial.executionDurationP50Us);
                        waitP50s.add(trial.queueWaitP50Ms);
                    }

                    System.out.printf("    [Trial %d/%d] Drain: %.2f ms | Throughput: %.2f jobs/s | E2E P50: %.2f ms | Valid: %b%n",
                            m, measuredTrials, trial.drainDurationNanos / 1_000_000.0, trial.drainThroughputJobsPerSec,
                            trial.endToEndP50Ms, trial.isValid);
                }

                Collections.sort(enqThroughputs);
                Collections.sort(drainThroughputs);
                Collections.sort(drainDurationsMs);
                Collections.sort(e2eP50s);
                Collections.sort(e2eP95s);
                Collections.sort(execP50s);
                Collections.sort(waitP50s);

                configResult.medianEnqueueThroughputJobsPerSec = computeMedianDoubles(enqThroughputs);
                configResult.medianDrainThroughputJobsPerSec = computeMedianDoubles(drainThroughputs);
                configResult.medianDrainDurationMs = computeMedianDoubles(drainDurationsMs);
                configResult.p50EndToEndLatencyMs = computeMedianDoubles(e2eP50s);
                configResult.p95EndToEndLatencyMs = computeMedianDoubles(e2eP95s);
                configResult.p50ExecutionLatencyUs = computeMedianDoubles(execP50s);
                configResult.p50QueueWaitMs = computeMedianDoubles(waitP50s);

                System.out.printf("    ==> RESULT [W=%d, B=%d]: Median Drain Throughput: %.2f jobs/s | Median Drain Time: %.2f ms | E2E P50: %.2f ms%n%n",
                        workers, batchSize, configResult.medianDrainThroughputJobsPerSec, configResult.medianDrainDurationMs, configResult.p50EndToEndLatencyMs);

                report.configurations.add(configResult);
            }
        }
        return report;
    }

    public static void saveReportToJson(BenchmarkSuiteReport report, Path filePath) throws IOException {
        Files.createDirectories(filePath.getParent());
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Files.writeString(filePath, gson.toJson(report));
    }

    // Statistical Percentile Utilities
    public static double computeMedian(List<Long> sortedValues) {
        if (sortedValues == null || sortedValues.isEmpty()) return 0.0;
        int size = sortedValues.size();
        if (size % 2 == 1) {
            return sortedValues.get(size / 2);
        } else {
            return (sortedValues.get((size / 2) - 1) + sortedValues.get(size / 2)) / 2.0;
        }
    }

    public static double computeMedianDoubles(List<Double> sortedValues) {
        if (sortedValues == null || sortedValues.isEmpty()) return 0.0;
        int size = sortedValues.size();
        if (size % 2 == 1) {
            return sortedValues.get(size / 2);
        } else {
            return (sortedValues.get((size / 2) - 1) + sortedValues.get(size / 2)) / 2.0;
        }
    }

    /**
     * Nearest-rank percentile calculation:
     * index = ceil(p * N) - 1, clamped to [0, N-1].
     */
    public static double computePercentile(List<Long> sortedValues, double percentile) {
        if (sortedValues == null || sortedValues.isEmpty()) return 0.0;
        int idx = (int) Math.ceil(percentile * sortedValues.size()) - 1;
        return sortedValues.get(Math.max(0, Math.min(idx, sortedValues.size() - 1)));
    }

    public static double computeMean(List<Long> values) {
        if (values == null || values.isEmpty()) return 0.0;
        double sum = 0.0;
        for (long v : values) sum += v;
        return sum / values.size();
    }
}
