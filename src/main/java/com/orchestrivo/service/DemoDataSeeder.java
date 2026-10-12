package com.orchestrivo.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.orchestrivo.domain.AlgorithmType;
import com.orchestrivo.domain.ProcessSpec;
import com.orchestrivo.domain.Workload;
import com.orchestrivo.model.*;
import com.orchestrivo.persistence.*;
import com.orchestrivo.util.WorkloadGenerator;
import com.orchestrivo.web.ServiceRegistry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;

public class DemoDataSeeder {

    public static final String DEMO_EMAIL = "demo@orchestrivo.io";
    public static final String DEMO_PASSWORD = "Demo@12345";
    public static final String DEMO_FULL_NAME = "Orchestrivo Demo User";

    public static class DemoSeedResult {
        public long demoUserId;
        public int workloadsCreated = 0;
        public int jobsCompleted = 0;
        public int jobsFailed = 0;
        public int jobsCancelled = 0;
        public int jobsQueued = 0;
        public int benchmarksRun = 0;
        public long featuredBaselineId = 0;
        public long featuredCandidateId = 0;
        public String status = "SUCCESS";
        public String message = "";
    }

    public static DemoSeedResult seed(boolean resetPrevious) {
        DemoSeedResult result = new DemoSeedResult();

        UserRepository userRepo = ServiceRegistry.userRepository;
        WorkloadRepository workloadRepo = ServiceRegistry.workloadRepository;
        ExecutionJobRepository jobRepo = ServiceRegistry.executionJobRepository;
        BenchmarkRepository benchmarkRepo = ServiceRegistry.benchmarkRepository;
        SystemSettingsRepository settingsRepo = ServiceRegistry.systemSettingsRepository;
        ExecutionJobService jobService = ServiceRegistry.executionJobService;
        ExecutionWorkerManager workerManager = ServiceRegistry.workerManager;
        BenchmarkService benchmarkService = ServiceRegistry.benchmarkService;

        if (userRepo == null || jobRepo == null) {
            result.status = "ERROR";
            result.message = "Service registry repositories are not initialized.";
            return result;
        }

        // 1. Ensure Demo User Exists
        Optional<User> existingUser = userRepo.findByEmail(DEMO_EMAIL);
        User demoUser;
        if (existingUser.isPresent()) {
            demoUser = existingUser.get();
        } else {
            String hash = BCrypt.withDefaults().hashToString(12, DEMO_PASSWORD.toCharArray());
            demoUser = new User();
            demoUser.setEmail(DEMO_EMAIL);
            demoUser.setFullName(DEMO_FULL_NAME);
            demoUser.setPasswordHash(hash);
            demoUser.setRole("USER");
            demoUser.setActive(true);
            demoUser = userRepo.save(demoUser);
        }
        result.demoUserId = demoUser.getId();

        // 2. Clean previous demo data if reset requested
        if (resetPrevious) {
            cleanUserData(demoUser.getId());
        }

        // Check if demo user already has enough workloads/jobs and not resetting
        List<WorkloadRecord> existingWorkloads = workloadRepo.findByOwnerId(demoUser.getId());
        if (!resetPrevious && existingWorkloads.size() >= 5) {
            result.status = "SKIPPED";
            result.message = "Demo data already initialized for " + DEMO_EMAIL + ". Use reset=true to overwrite.";
            return result;
        }

        // 3. Create 5 Realistic Workloads
        List<WorkloadRecord> createdWorkloads = new ArrayList<>();

        // Workload 1: Convoy Effect Workload
        List<ProcessSpec> convoyProcesses = List.of(
                new ProcessSpec("P1", 0, 32),
                new ProcessSpec("P2", 1, 2),
                new ProcessSpec("P3", 2, 3),
                new ProcessSpec("P4", 3, 2),
                new ProcessSpec("P5", 4, 1)
        );
        createdWorkloads.add(createWorkload(workloadRepo, demoUser.getId(),
                "Convoy Effect Scenario",
                "Demonstrates the convoy effect where a CPU-heavy process arrives first, stalling subsequent short processes.",
                convoyProcesses));

        // Workload 2: High Contention & Interactive Workload
        List<ProcessSpec> interactiveProcesses = new ArrayList<>();
        int[] arrivals = {0, 0, 1, 1, 2, 2, 3, 3, 4, 4};
        int[] bursts =   {3, 4, 2, 5, 3, 2, 4, 3, 2, 5};
        for (int i = 0; i < 10; i++) {
            interactiveProcesses.add(new ProcessSpec("P" + (i + 1), arrivals[i], bursts[i]));
        }
        createdWorkloads.add(createWorkload(workloadRepo, demoUser.getId(),
                "Interactive High Contention",
                "Multiple interactive tasks with overlapping arrival times and modest bursts for testing preemption and response time.",
                interactiveProcesses));

        // Workload 3: Uniform Equal Burst Workload
        List<ProcessSpec> uniformProcesses = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            uniformProcesses.add(new ProcessSpec("P" + (i + 1), i * 2, 6));
        }
        createdWorkloads.add(createWorkload(workloadRepo, demoUser.getId(),
                "Uniform Equal Burst Workload",
                "Eight identical tasks arriving at uniform intervals of 2 units, ideal for algorithm fairness comparison.",
                uniformProcesses));

        // Workload 4: High-Density Scaling Workload (50 Processes)
        List<ProcessSpec> scaleProcesses = WorkloadGenerator.generateProcesses(50, 0, 40, 1, 20, 42L);
        createdWorkloads.add(createWorkload(workloadRepo, demoUser.getId(),
                "High-Density Scaling Workload (50 Procs)",
                "50 deterministically generated processes to test algorithm scalability and computational throughput.",
                scaleProcesses));

        // Workload 5: Mixed Arrival & Burst Workload
        List<ProcessSpec> mixedProcesses = new ArrayList<>();
        int[] mArrivals = {0, 1, 3, 4, 6, 8, 10, 12, 14, 15, 18, 20};
        int[] mBursts =   {8, 4, 12, 2, 16, 3, 7, 10, 2, 5, 14, 1};
        for (int i = 0; i < mArrivals.length; i++) {
            mixedProcesses.add(new ProcessSpec("P" + (i + 1), mArrivals[i], mBursts[i]));
        }
        createdWorkloads.add(createWorkload(workloadRepo, demoUser.getId(),
                "Mixed Dynamic Workload",
                "Varied arrival and burst times modeling a realistic multi-tenant server workload.",
                mixedProcesses));

        result.workloadsCreated = createdWorkloads.size();

        // 4. Generate ~40 Execution Jobs across all 4 algorithms
        // A. 28 COMPLETED jobs
        String[] algorithms = {"FCFS", "SJF", "SRTF", "ROUND_ROBIN"};
        int[] quanta = {2, 3, 4, 5};

        for (int i = 0; i < 28; i++) {
            WorkloadRecord wr = createdWorkloads.get(i % createdWorkloads.size());
            String algo = algorithms[i % algorithms.length];
            int q = quanta[i % quanta.length];

            ExecutionJobRecord job = jobService.enqueueExperiment(wr, algo, q, demoUser.getId(), "seed-completed-" + i);
            // Execute genuine job pipeline
            Optional<ExecutionJobRecord> claimed = jobRepo.claimNextQueuedJob("seed-worker");
            if (claimed.isPresent()) {
                workerManager.executeJob(claimed.get());
                result.jobsCompleted++;
            }
        }

        // B. 4 FAILED jobs (2 via genuine production pipeline error handling, 2 explicitly labeled simulated fixtures)
        for (int i = 0; i < 2; i++) {
            WorkloadRecord wr = createdWorkloads.get(i % createdWorkloads.size());
            // Create a job with empty process snapshot to trigger genuine execution pipeline validation failure
            ExecutionJobRecord emptySnapshotJob = new ExecutionJobRecord();
            emptySnapshotJob.setOwnerUserId(demoUser.getId());
            emptySnapshotJob.setWorkloadId(wr.getId());
            emptySnapshotJob.setAlgorithmName("FCFS");
            emptySnapshotJob.setInputSnapshot("[]");
            emptySnapshotJob.setStatus(ExecutionJobStatus.QUEUED);
            emptySnapshotJob.setAttemptCount(0);
            emptySnapshotJob.setMaxAttempts(1); // Fails permanently on first attempt
            emptySnapshotJob.setIdempotencyKey("seed-genuine-fail-" + i);
            ExecutionJobRecord savedJob = jobRepo.save(emptySnapshotJob);

            Optional<ExecutionJobRecord> claimed = jobRepo.claimNextQueuedJob("seed-worker");
            if (claimed.isPresent()) {
                // Execute through genuine workerManager production failure pipeline
                workerManager.executeJob(claimed.get());
                result.jobsFailed++;
            }
        }

        for (int i = 0; i < 2; i++) {
            WorkloadRecord wr = createdWorkloads.get(i % createdWorkloads.size());
            ExecutionJobRecord job = jobService.enqueueExperiment(wr, "ROUND_ROBIN", 3, demoUser.getId(), "seed-sim-fail-" + i);
            Optional<ExecutionJobRecord> claimed = jobRepo.claimNextQueuedJob("seed-worker");
            if (claimed.isPresent()) {
                jobRepo.markFailed(claimed.get().getId(), claimed.get().getClaimToken(),
                        "[Simulated Failure Fixture] Deterministic edge fixture: worker process terminated on attempt " + claimed.get().getAttemptCount(), false);
                result.jobsFailed++;
            }
        }

        // C. 3 CANCELLED jobs
        for (int i = 0; i < 3; i++) {
            WorkloadRecord wr = createdWorkloads.get(i % createdWorkloads.size());
            ExecutionJobRecord job = new ExecutionJobRecord();
            job.setOwnerUserId(demoUser.getId());
            job.setWorkloadId(wr.getId());
            job.setAlgorithmName("FCFS");
            job.setAlgorithmParameters("{}");
            job.setInputSnapshot(WorkloadSnapshot.serialize(wr.getWorkload(), wr.getGenerationSeed()));
            job.setStatus(ExecutionJobStatus.QUEUED);
            job.setAttemptCount(0);
            job.setMaxAttempts(3);
            job.setIdempotencyKey("seed-cancel-" + i);
            ExecutionJobRecord saved = jobRepo.save(job);
            boolean cancelled = jobService.cancelJob(saved.getId(), demoUser.getId());
            if (cancelled) {
                result.jobsCancelled++;
            }
        }

        // D. 3 QUEUED jobs (stay in queue for real live queue status)
        for (int i = 0; i < 3; i++) {
            WorkloadRecord wr = createdWorkloads.get(i % createdWorkloads.size());
            String algo = algorithms[(i + 1) % algorithms.length];
            ExecutionJobRecord qJob = new ExecutionJobRecord();
            qJob.setOwnerUserId(demoUser.getId());
            qJob.setWorkloadId(wr.getId());
            qJob.setAlgorithmName(algo);
            Map<String, Object> params = new HashMap<>();
            if ("ROUND_ROBIN".equalsIgnoreCase(algo)) {
                params.put("quantum", 3);
            }
            qJob.setAlgorithmParameters(new com.google.gson.Gson().toJson(params));
            qJob.setInputSnapshot(WorkloadSnapshot.serialize(wr.getWorkload(), wr.getGenerationSeed()));
            qJob.setStatus(ExecutionJobStatus.QUEUED);
            qJob.setAttemptCount(0);
            qJob.setMaxAttempts(3);
            qJob.setIdempotencyKey("seed-queued-" + i);
            jobRepo.save(qJob);
            result.jobsQueued++;
        }

        // 5. Run Real Micro-Benchmarks
        BenchmarkService.BenchmarkConfig baseConfig = new BenchmarkService.BenchmarkConfig();
        baseConfig.setAlgorithmName("ROUND_ROBIN");
        baseConfig.setProcessCount(50);
        baseConfig.setQuantum(3);
        baseConfig.setSeed(42L);
        baseConfig.setWarmupIterations(5);
        baseConfig.setMeasuredIterations(15);
        baseConfig.setRunType("BASELINE");
        baseConfig.setBenchmarkGroup("RR-Tuning");
        BenchmarkRunRecord baseRun = benchmarkService.runBenchmark(demoUser.getId(), baseConfig);
        result.benchmarksRun++;
        result.featuredBaselineId = baseRun.getId();

        BenchmarkService.BenchmarkConfig candConfig = new BenchmarkService.BenchmarkConfig();
        candConfig.setAlgorithmName("ROUND_ROBIN");
        candConfig.setProcessCount(50);
        candConfig.setQuantum(6);
        candConfig.setSeed(42L);
        candConfig.setWarmupIterations(5);
        candConfig.setMeasuredIterations(15);
        candConfig.setRunType("CANDIDATE");
        candConfig.setBenchmarkGroup("RR-Tuning");
        BenchmarkRunRecord candRun = benchmarkService.runBenchmark(demoUser.getId(), candConfig);
        result.benchmarksRun++;
        result.featuredCandidateId = candRun.getId();

        // Run other standard benchmarks for algorithm-wise insights
        for (String algo : List.of("FCFS", "SJF", "SRTF")) {
            BenchmarkService.BenchmarkConfig cfg = new BenchmarkService.BenchmarkConfig();
            cfg.setAlgorithmName(algo);
            cfg.setProcessCount(50);
            cfg.setSeed(42L);
            cfg.setWarmupIterations(5);
            cfg.setMeasuredIterations(15);
            cfg.setRunType("STANDARD");
            cfg.setBenchmarkGroup("Standard-Suite");
            benchmarkService.runBenchmark(demoUser.getId(), cfg);
            result.benchmarksRun++;
        }

        // 6. Durable Pinning of the Featured Baseline/Candidate Pair
        benchmarkService.setFeaturedComparison(baseRun.getId(), candRun.getId());

        result.status = "SUCCESS";
        result.message = "Seeded demo data: 5 workloads, " + (result.jobsCompleted + result.jobsFailed + result.jobsCancelled + result.jobsQueued) +
                " execution jobs (" + result.jobsCompleted + " completed, " + result.jobsFailed + " failed, " + result.jobsCancelled + " cancelled, " + result.jobsQueued + " queued), " +
                result.benchmarksRun + " benchmarks with canonical pinned comparison (#" + baseRun.getId() + " vs #" + candRun.getId() + ").";
        return result;
    }

    private static WorkloadRecord createWorkload(WorkloadRepository repo, long userId, String name, String desc, List<ProcessSpec> procs) {
        WorkloadRecord wr = new WorkloadRecord();
        wr.setOwnerUserId(userId);
        wr.setName(name);
        wr.setDescription(desc);
        wr.setSourceType("MANUAL");
        wr.setWorkload(new Workload(procs));
        return repo.save(wr);
    }

    private static void cleanUserData(long userId) {
        try (Connection conn = DataSourceManager.getDataSource().getConnection()) {
            // Delete jobs
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM execution_jobs WHERE owner_user_id = ?")) {
                ps.setLong(1, userId);
                ps.executeUpdate();
            }
            // Delete experiment runs
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM experiment_runs WHERE owner_user_id = ?")) {
                ps.setLong(1, userId);
                ps.executeUpdate();
            }
            // Delete benchmark runs
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM benchmark_runs WHERE owner_user_id = ?")) {
                ps.setLong(1, userId);
                ps.executeUpdate();
            }
            // Delete workloads
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM workloads WHERE owner_user_id = ?")) {
                ps.setLong(1, userId);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            System.err.println("Warning during demo user cleanup: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Orchestrivo Demo Data Seeder ===");
        try {
            DataSourceManager.getDataSource();
            UserRepository ur = new UserRepositoryJdbcImpl();
            WorkloadRepository wr = new WorkloadRepositoryJdbcImpl();
            ExperimentRunRepository er = new ExperimentRunRepositoryJdbcImpl();
            BenchmarkRepository br = new BenchmarkRepositoryJdbcImpl();
            AuditLogRepository ar = new AuditLogRepositoryJdbcImpl();
            ExecutionJobRepository jr = new ExecutionJobRepositoryJdbcImpl();
            SystemSettingsRepository sr = new SystemSettingsRepositoryJdbcImpl();

            ServiceRegistry.initialize(ur, wr, er, br, ar, jr, sr);

            DemoSeedResult result = seed(true);
            System.out.println("Result: " + result.status);
            System.out.println(result.message);
        } catch (Exception e) {
            System.err.println("Fatal: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DataSourceManager.closeDataSource();
        }
    }
}
