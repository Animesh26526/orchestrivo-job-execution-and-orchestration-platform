package com.orchestrivo.benchmark;

import com.orchestrivo.domain.*;
import com.orchestrivo.model.*;
import com.orchestrivo.persistence.*;
import com.orchestrivo.scheduling.SchedulingStrategy;
import com.orchestrivo.scheduling.factory.SchedulingStrategyFactory;
import com.orchestrivo.service.AuditService;
import com.orchestrivo.service.ExecutionJobService;
import com.orchestrivo.service.ExecutionWorkerManager;
import com.orchestrivo.util.WorkloadGenerator;
import com.orchestrivo.validation.ScheduleValidator;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class BenchmarkCampaignRunnerTest {

    private static DataSource dataSource;
    private static ExecutionJobRepository jobRepository;
    private static ExperimentRunRepository experimentRunRepository;
    private static UserRepository userRepository;
    private static WorkloadRepository workloadRepository;
    private static AuditLogRepository auditRepository;
    private static AuditService auditService;
    private static User testUser;
    private static WorkloadRecord testWorkload;

    @BeforeAll
    static void setUp() {
        try {
            dataSource = DataSourceManager.getDataSource();
            try (Connection conn = dataSource.getConnection()) {
                Assumptions.assumeTrue(conn.isValid(2), "Database connection is not available");
                jobRepository = new ExecutionJobRepositoryJdbcImpl();
                experimentRunRepository = new ExperimentRunRepositoryJdbcImpl();
                userRepository = new UserRepositoryJdbcImpl();
                workloadRepository = new WorkloadRepositoryJdbcImpl();
                auditRepository = new AuditLogRepositoryJdbcImpl();
                auditService = new AuditService(auditRepository);

                User u = new User();
                u.setEmail("campaign_user_" + System.currentTimeMillis() + "@example.com");
                u.setPasswordHash("hash123");
                u.setRole("USER");
                u.setActive(true);
                testUser = userRepository.save(u);

                WorkloadRecord w = new WorkloadRecord();
                w.setOwnerUserId(testUser.getId());
                w.setName("Campaign Workload");
                w.setSourceType("MANUAL");
                w.setWorkload(new Workload(List.of(
                        new ProcessSpec("P1", 0, 8),
                        new ProcessSpec("P2", 1, 4),
                        new ProcessSpec("P3", 2, 9),
                        new ProcessSpec("P4", 3, 5)
                )));
                testWorkload = workloadRepository.save(w);
            }
        } catch (Exception e) {
            System.err.println("Setup warning: DB not available for queue benchmarks: " + e.getMessage());
        }
    }

    @Test
    void runFullSchedulingBenchmarkCampaign() {
        System.out.println("==========================================================================");
        System.out.println("          ORCHESTRIVO STAGE 4.1 EMPIRICAL BENCHMARK CAMPAIGN              ");
        System.out.println("==========================================================================");

        // Hardware & Runtime Specs
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        StringBuilder gcNames = new StringBuilder();
        for (GarbageCollectorMXBean gc : gcBeans) {
            if (gcNames.length() > 0) gcNames.append(", ");
            gcNames.append(gc.getName());
        }

        System.out.println("ENVIRONMENT METADATA:");
        System.out.println("  OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version") + " (" + System.getProperty("os.arch") + ")");
        System.out.println("  Logical Processors: " + Runtime.getRuntime().availableProcessors());
        System.out.println("  Java Runtime: " + System.getProperty("java.runtime.version") + " (" + System.getProperty("java.vendor") + ")");
        System.out.println("  Java VM: " + System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version"));
        System.out.println("  Garbage Collector: " + gcNames);
        System.out.println("  Max Heap: " + (Runtime.getRuntime().maxMemory() / (1024 * 1024)) + " MB");
        System.out.println("==========================================================================\n");

        SchedulingStrategyFactory factory = new SchedulingStrategyFactory();
        ScheduleValidator validator = new ScheduleValidator();

        int[] processScales = {10, 50, 100, 250, 500};
        String[] algorithms = {"FCFS", "SJF", "SRTF", "ROUND_ROBIN_Q2", "ROUND_ROBIN_Q5"};
        int warmupCount = 15;
        int measuredCount = 40;
        long seed = 42L;

        System.out.printf("%-18s | %-6s | %-10s | %-10s | %-10s | %-10s | %-10s | %-10s | %-7s | %-6s%n",
                "Algorithm", "N", "Min(us)", "Med/P50(us)", "P95(us)", "Max(us)", "Mean(us)", "StdDev(us)", "CV(%)", "Valid");
        System.out.println("-------------------+--------+------------+------------+------------+------------+------------+------------+---------+-------");

        Map<String, Map<Integer, Double>> p50Results = new LinkedHashMap<>();
        for (String algo : algorithms) {
            p50Results.put(algo, new LinkedHashMap<>());
        }

        // Track warmup progression for JIT analysis (SRTF at N=500)
        List<Long> srtfWarmupDurations = new ArrayList<>();

        for (int n : processScales) {
            List<ProcessSpec> processes = WorkloadGenerator.generateProcesses(n, 0, 20, 1, 15, seed);
            Workload workload = new Workload(processes);

            for (String algoKey : algorithms) {
                AlgorithmType type;
                int quantum = 3;
                if ("ROUND_ROBIN_Q2".equals(algoKey)) {
                    type = AlgorithmType.ROUND_ROBIN;
                    quantum = 2;
                } else if ("ROUND_ROBIN_Q5".equals(algoKey)) {
                    type = AlgorithmType.ROUND_ROBIN;
                    quantum = 5;
                } else {
                    type = AlgorithmType.valueOf(algoKey);
                }

                SchedulingStrategy strategy = factory.getStrategy(type);
                SchedulingConfig config = new SchedulingConfig(type, quantum);

                // Warmup
                for (int w = 0; w < warmupCount; w++) {
                    long wStart = System.nanoTime();
                    strategy.schedule(workload, config);
                    long wElapsed = System.nanoTime() - wStart;
                    if ("SRTF".equals(algoKey) && n == 500) {
                        srtfWarmupDurations.add(wElapsed);
                    }
                }

                // Measured
                List<Long> measurementsNanos = new ArrayList<>();
                boolean allValid = true;

                for (int m = 0; m < measuredCount; m++) {
                    long start = System.nanoTime();
                    ScheduleResult res = strategy.schedule(workload, config);
                    long elapsed = System.nanoTime() - start;
                    measurementsNanos.add(elapsed);

                    ValidationResult val = validator.validate(workload, res);
                    if (!val.isValid()) {
                        allValid = false;
                    }
                }

                assertTrue(allValid, "Schedule validation must pass for " + algoKey + " N=" + n);

                Collections.sort(measurementsNanos);
                double minUs = measurementsNanos.get(0) / 1000.0;
                double maxUs = measurementsNanos.get(measurementsNanos.size() - 1) / 1000.0;
                double medianUs = computeMedian(measurementsNanos) / 1000.0;
                double p95Us = computePercentile(measurementsNanos, 0.95) / 1000.0;
                double meanUs = computeMean(measurementsNanos) / 1000.0;
                double stdDevUs = computeStdDev(measurementsNanos) / 1000.0;
                double cvPercent = (meanUs > 0) ? (stdDevUs / meanUs) * 100.0 : 0.0;

                p50Results.get(algoKey).put(n, medianUs);

                System.out.printf("%-18s | %-6d | %-10.2f | %-10.2f | %-10.2f | %-10.2f | %-10.2f | %-10.2f | %-6.2f%% | %-6s%n",
                        algoKey, n, minUs, medianUs, p95Us, maxUs, meanUs, stdDevUs, cvPercent, allValid ? "PASS" : "FAIL");
            }
            System.out.println("-------------------+--------+------------+------------+------------+------------+------------+------------+---------+-------");
        }

        // Print JIT Warmup Analysis
        System.out.println("\nJIT COMPILATION WARMUP PROGRESSION (SRTF, N=500, first 15 untimed iterations in microseconds):");
        for (int i = 0; i < srtfWarmupDurations.size(); i++) {
            System.out.printf("  Warmup Iteration %2d: %8.2f us%n", (i + 1), srtfWarmupDurations.get(i) / 1000.0);
        }
        System.out.println("==========================================================================\n");
    }

    @Test
    void runQueuePerformanceAndStressMeasurements() throws Exception {
        Assumptions.assumeTrue(dataSource != null, "Database must be available for queue load test");

        System.out.println("==========================================================================");
        System.out.println("     ORCHESTRIVO ASYNCHRONOUS QUEUE STRESS & CONCURRENCY BENCHMARKS       ");
        System.out.println("==========================================================================");

        ExecutionJobService service = new ExecutionJobService(jobRepository);

        // 1. Enqueue Burst Measurements: 50, 100, 200 jobs
        int[] burstSizes = {50, 100, 200};
        System.out.println("\n1. ENQUEUE BURST BENCHMARK:");
        System.out.printf("%-12s | %-15s | %-15s | %-15s | %-15s | %-18s%n",
                "Burst Size", "Total Time(ms)", "Avg Latency(ms)", "P50 Latency(ms)", "P95 Latency(ms)", "Throughput(jobs/s)");
        System.out.println("-------------+-----------------+-----------------+-----------------+-----------------+-------------------");

        for (int burst : burstSizes) {
            List<Long> latencies = new ArrayList<>();
            long burstStart = System.currentTimeMillis();

            for (int i = 0; i < burst; i++) {
                String idempotencyKey = "burst_" + burst + "_" + i + "_" + System.nanoTime();
                long start = System.currentTimeMillis();
                service.enqueueExperiment(
                        testWorkload,
                        "FCFS",
                        3,
                        testUser.getId(),
                        idempotencyKey
                );
                long latency = System.currentTimeMillis() - start;
                latencies.add(latency);
            }
            long burstDuration = System.currentTimeMillis() - burstStart;
            Collections.sort(latencies);

            double avgLat = computeMeanLongs(latencies);
            double p50Lat = computeMedianLongs(latencies);
            double p95Lat = computePercentileLongs(latencies, 0.95);
            double throughput = (burst / (double) Math.max(1, burstDuration)) * 1000.0;

            System.out.printf("%-12d | %-15d | %-15.2f | %-15.2f | %-15.2f | %-18.2f%n",
                    burst, burstDuration, avgLat, p50Lat, p95Lat, throughput);
        }

        // 2. Worker Pool Scalability & Processing Drain Throughput: 2, 4, 8 workers
        System.out.println("\n2. WORKER POOL SCALABILITY (DRAIN 30 JOBS):");
        System.out.printf("%-14s | %-12s | %-15s | %-18s | %-24s%n",
                "Worker Count", "Jobs Drained", "Drain Time(ms)", "Throughput(jobs/s)", "Wait / Exec (ms)");
        System.out.println("---------------+--------------+-----------------+--------------------+-------------------------");

        int[] workerCounts = {2, 4, 8};
        for (int workers : workerCounts) {
            int jobsToDrain = 30;
            for (int i = 0; i < jobsToDrain; i++) {
                service.enqueueExperiment(
                        testWorkload,
                        "FCFS",
                        3,
                        testUser.getId(),
                        "drain_" + workers + "_" + i + "_" + System.nanoTime()
                );
            }

            ExecutionWorkerManager workerManager = new ExecutionWorkerManager(
                    jobRepository,
                    experimentRunRepository,
                    auditService,
                    workers
            );

            long drainStart = System.currentTimeMillis();
            workerManager.start();

            // Wait until queue is drained or timeout (max 15s)
            long timeout = System.currentTimeMillis() + 15000;
            while (System.currentTimeMillis() < timeout) {
                QueueMetrics m = jobRepository.getQueueMetrics(testUser.getId());
                if (m.getQueuedCount() == 0 && m.getRunningCount() == 0) {
                    break;
                }
                Thread.sleep(100);
            }
            long drainDuration = System.currentTimeMillis() - drainStart;
            workerManager.shutdown();

            QueueMetrics metrics = jobRepository.getQueueMetrics(testUser.getId());
            double drainThroughput = (jobsToDrain / (double) Math.max(1, drainDuration)) * 1000.0;

            System.out.printf("%-14d | %-12d | %-15d | %-18.2f | Wait: %-4.1fms / Exec: %-4.1fms%n",
                    workers, jobsToDrain, drainDuration, drainThroughput,
                    metrics.getAvgQueueWaitMs(), metrics.getAvgExecutionDurationMs());
        }

        // 3. Failure Mode & Correctness Verifications
        System.out.println("\n3. FAILURE MODE & CONCURRENCY VERIFICATIONS:");

        // Test 3a: Concurrent Idempotency Deduplication under 100-thread burst
        int concurrentDups = 100;
        String sharedIdempotencyKey = "strict_dedup_" + System.currentTimeMillis();
        ExecutorService dedupPool = Executors.newFixedThreadPool(16);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(concurrentDups);
        Set<Long> returnedJobIds = Collections.synchronizedSet(new HashSet<>());

        for (int i = 0; i < concurrentDups; i++) {
            dedupPool.submit(() -> {
                try {
                    startLatch.await();
                    ExecutionJobRecord job = service.enqueueExperiment(
                            testWorkload,
                            "SJF",
                            3,
                            testUser.getId(),
                            sharedIdempotencyKey
                    );
                    returnedJobIds.add(job.getId());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    finishLatch.countDown();
                }
            });
        }
        startLatch.countDown();
        finishLatch.await(10, TimeUnit.SECONDS);
        dedupPool.shutdown();

        assertEquals(1, returnedJobIds.size(), "All 100 concurrent requests must resolve to the identical single Job ID");
        System.out.printf("  [PASS] Concurrent Idempotency Deduplication: 100 concurrent requests resolved to 1 unique job (100%% deduplication rate)%n");

        // Test 3b: Stale Job Recovery Duration
        ExecutionJobRecord staleJob = service.enqueueExperiment(
                testWorkload,
                "ROUND_ROBIN",
                2,
                testUser.getId(),
                "stale_test_" + System.nanoTime()
        );
        // Claim and simulate expired lease in the past
        Optional<ExecutionJobRecord> claimedStale = jobRepository.claimNextQueuedJob("dead-worker", -60);
        assertTrue(claimedStale.isPresent());

        long recoverStart = System.currentTimeMillis();
        int recoveredCount = jobRepository.recoverStaleJobs(0);
        long recoverDuration = System.currentTimeMillis() - recoverStart;

        assertTrue(recoveredCount >= 1, "At least 1 stale job should be recovered");
        Optional<ExecutionJobRecord> reloaded = jobRepository.findById(claimedStale.get().getId());
        assertTrue(reloaded.isPresent());
        assertEquals(ExecutionJobStatus.QUEUED, reloaded.get().getStatus(), "Stale job must be reset to QUEUED");
        System.out.printf("  [PASS] Stale-Job Recovery: Recovered expired job in %d ms back to QUEUED state%n", recoverDuration);

        // Test 3c: Worker Fencing with Claim Token Mismatch
        ExecutionJobRecord fencedJob = service.enqueueExperiment(
                testWorkload,
                "FCFS",
                3,
                testUser.getId(),
                "fenced_test_" + System.nanoTime()
        );
        Optional<ExecutionJobRecord> claimedFenced = jobRepository.claimNextQueuedJob("worker-alpha", 30);
        assertTrue(claimedFenced.isPresent());

        // Attempt completion using an invalid/stale claim token
        ExperimentRunRecord runRecord = new ExperimentRunRecord();
        runRecord.setOwnerUserId(testUser.getId());
        runRecord.setWorkloadId(testWorkload.getId());
        runRecord.setAlgorithmName("FCFS");
        runRecord.setInputSnapshot("{}");
        runRecord.setRunStatus("SUCCEEDED");
        runRecord.setValidationStatus("VALID");

        boolean unauthorizedComplete = jobRepository.completeJobWithExperimentRun(
                claimedFenced.get().getId(),
                "stale-expired-token-uuid",
                runRecord,
                150
        );
        assertFalse(unauthorizedComplete, "Completion attempt with mismatched claimToken must return false");

        // Legitimate worker completes
        boolean legitimateComplete = jobRepository.completeJobWithExperimentRun(
                claimedFenced.get().getId(),
                claimedFenced.get().getClaimToken(),
                runRecord,
                150
        );
        assertTrue(legitimateComplete, "Legitimate worker with active claimToken must succeed");
        System.out.printf("  [PASS] Worker Fencing: Mismatched token rejected, legitimate claim completed atomically%n");

        System.out.println("==========================================================================\n");
    }

    private static double computeMedian(List<Long> values) {
        int size = values.size();
        if (size == 0) return 0.0;
        if (size % 2 == 1) {
            return values.get(size / 2);
        } else {
            return (values.get((size / 2) - 1) + values.get(size / 2)) / 2.0;
        }
    }

    private static double computePercentile(List<Long> values, double percentile) {
        if (values.isEmpty()) return 0.0;
        int idx = (int) Math.ceil(percentile * values.size()) - 1;
        return values.get(Math.max(0, Math.min(idx, values.size() - 1)));
    }

    private static double computeMean(List<Long> values) {
        if (values.isEmpty()) return 0.0;
        double sum = 0.0;
        for (long v : values) sum += v;
        return sum / values.size();
    }

    private static double computeStdDev(List<Long> values) {
        if (values.size() <= 1) return 0.0;
        double mean = computeMean(values);
        double sumSq = 0.0;
        for (long v : values) {
            double diff = v - mean;
            sumSq += diff * diff;
        }
        return Math.sqrt(sumSq / (values.size() - 1));
    }

    private static double computeMeanLongs(List<Long> values) {
        return computeMean(values);
    }

    private static double computeMedianLongs(List<Long> values) {
        return computeMedian(values);
    }

    private static double computePercentileLongs(List<Long> values, double percentile) {
        return computePercentile(values, percentile);
    }
}
