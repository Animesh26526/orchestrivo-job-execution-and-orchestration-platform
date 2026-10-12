package com.orchestrivo.service;

import com.google.gson.Gson;
import com.orchestrivo.domain.ProcessSpec;
import com.orchestrivo.domain.ScheduleResult;
import com.orchestrivo.domain.Workload;
import com.orchestrivo.model.*;
import com.orchestrivo.persistence.*;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ExecutionJobConcurrencyTest {

    private static ExecutionJobRepository jobRepository;
    private static ExperimentRunRepository experimentRunRepository;
    private static UserRepository userRepository;
    private static WorkloadRepository workloadRepository;
    private static ExecutionJobService jobService;
    private static User testUser1;
    private static User testUser2;
    private static WorkloadRecord testWorkload1;

    @BeforeAll
    static void setUpAll() {
        try (Connection conn = DataSourceManager.getDataSource().getConnection()) {
            Assumptions.assumeTrue(conn.isValid(2), "Database connection is not available");
            jobRepository = new ExecutionJobRepositoryJdbcImpl();
            experimentRunRepository = new ExperimentRunRepositoryJdbcImpl();
            userRepository = new UserRepositoryJdbcImpl();
            workloadRepository = new WorkloadRepositoryJdbcImpl();
            jobService = new ExecutionJobService(jobRepository);

            User u1 = new User();
            u1.setEmail("concur_u1_" + System.currentTimeMillis() + "@example.com");
            u1.setPasswordHash("hash123");
            u1.setRole("USER");
            u1.setActive(true);
            testUser1 = userRepository.save(u1);

            User u2 = new User();
            u2.setEmail("concur_u2_" + System.currentTimeMillis() + "@example.com");
            u2.setPasswordHash("hash123");
            u2.setRole("USER");
            u2.setActive(true);
            testUser2 = userRepository.save(u2);

            WorkloadRecord w = new WorkloadRecord();
            w.setOwnerUserId(testUser1.getId());
            w.setName("Concurrency Test Workload");
            w.setSourceType("MANUAL");
            w.setWorkload(new Workload(List.of(
                    new ProcessSpec("P1", 0, 10),
                    new ProcessSpec("P2", 2, 5)
            )));
            testWorkload1 = workloadRepository.save(w);

        } catch (Exception e) {
            Assumptions.abort("Database connection failed: " + e.getMessage());
        }
    }

    @BeforeEach
    void cleanJobs() {
        cleanJobTable();
    }

    @AfterEach
    void cleanAfter() {
        cleanJobTable();
    }

    @AfterAll
    static void tearDownAll() {
        cleanJobTable();
        if (testWorkload1 != null && workloadRepository != null) {
            try { workloadRepository.deleteById(testWorkload1.getId()); } catch (Exception ignored) {}
        }
        if (testUser1 != null && userRepository != null) {
            try { userRepository.deleteById(testUser1.getId()); } catch (Exception ignored) {}
        }
        if (testUser2 != null && userRepository != null) {
            try { userRepository.deleteById(testUser2.getId()); } catch (Exception ignored) {}
        }
    }

    private static void cleanJobTable() {
        try (Connection conn = DataSourceManager.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM execution_jobs")) {
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    private ExecutionJobRecord createJob(User user, String idempotencyKey) {
        ExecutionJobRecord job = new ExecutionJobRecord();
        job.setOwnerUserId(user.getId());
        job.setWorkloadId(testWorkload1.getId());
        job.setAlgorithmName("FCFS");
        job.setAlgorithmParameters("{}");
        job.setInputSnapshot(new Gson().toJson(List.of(new ProcessSpec("P1", 0, 5))));
        job.setStatus(ExecutionJobStatus.QUEUED);
        job.setAttemptCount(0);
        job.setMaxAttempts(3);
        job.setIdempotencyKey(idempotencyKey);
        return jobRepository.save(job);
    }

    @Test
    @DisplayName("Scenario 1: Concurrent worker claiming guarantees zero duplicates and unique claim tokens")
    void testConcurrentWorkerClaimingNoDuplicates() throws Exception {
        int jobCount = 6;
        List<Long> enqueuedIds = new ArrayList<>();
        for (int i = 0; i < jobCount; i++) {
            ExecutionJobRecord j = createJob(testUser1, null);
            enqueuedIds.add(j.getId());
        }

        int workerThreads = 12;
        ExecutorService executor = Executors.newFixedThreadPool(workerThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(workerThreads);

        Set<Long> claimedJobIds = Collections.synchronizedSet(new HashSet<>());
        Set<String> claimTokens = Collections.synchronizedSet(new HashSet<>());
        AtomicInteger successfulClaims = new AtomicInteger(0);

        for (int i = 0; i < workerThreads; i++) {
            final String workerId = "worker-" + i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    Optional<ExecutionJobRecord> claimed = jobRepository.claimNextQueuedJob(workerId, 30);
                    if (claimed.isPresent()) {
                        ExecutionJobRecord cJob = claimed.get();
                        claimedJobIds.add(cJob.getId());
                        assertNotNull(cJob.getClaimToken(), "Claim token must be generated");
                        claimTokens.add(cJob.getClaimToken());
                        assertNotNull(cJob.getLeaseExpiresAt(), "Lease expiration must be populated");
                        successfulClaims.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(jobCount, successfulClaims.get(), "All 6 jobs should be claimed exactly once");
        assertEquals(jobCount, claimedJobIds.size(), "Each claimed job ID must be unique");
        assertEquals(jobCount, claimTokens.size(), "Each worker claim token must be unique");
    }

    @Test
    @DisplayName("Scenario 2: Concurrent submissions with identical idempotency key return exact same job")
    void testIdempotencyRaceDeduplication() throws Exception {
        String idempotencyKey = "race-idem-" + UUID.randomUUID();
        int threads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        List<ExecutionJobRecord> returnedJobs = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    ExecutionJobRecord res = jobService.enqueueExperiment(testWorkload1, "FCFS", 0, testUser1.getId(), idempotencyKey);
                    returnedJobs.add(res);
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(0, errorCount.get(), "Zero exceptions should occur during idempotency race");
        assertEquals(threads, returnedJobs.size());

        long canonicalId = returnedJobs.get(0).getId();
        for (ExecutionJobRecord rec : returnedJobs) {
            assertEquals(canonicalId, rec.getId(), "All concurrent requests must resolve to the identical job ID");
        }
    }

    @Test
    @DisplayName("Scenario 3: Heartbeat lease renewal extends lease_expires_at")
    void testHeartbeatLeaseRenewal() {
        ExecutionJobRecord job = createJob(testUser1, null);
        Optional<ExecutionJobRecord> claimed = jobRepository.claimNextQueuedJob("worker-hb", 10);
        assertTrue(claimed.isPresent());

        String token = claimed.get().getClaimToken();
        LocalDateTime initialLease = claimed.get().getLeaseExpiresAt();
        assertNotNull(token);
        assertNotNull(initialLease);

        // Renew lease for 60 seconds
        boolean renewed = jobRepository.renewLease(job.getId(), token, 60);
        assertTrue(renewed);

        Optional<ExecutionJobRecord> refreshed = jobRepository.findById(job.getId());
        assertTrue(refreshed.isPresent());
        assertTrue(refreshed.get().getLeaseExpiresAt().isAfter(initialLease) || refreshed.get().getLeaseExpiresAt().isEqual(initialLease));
    }

    @Test
    @DisplayName("Scenario 4: Stale worker completion rejected by fencing when claim token mismatch")
    void testStaleWorkerCompletionRejectedByFencing() {
        ExecutionJobRecord job = createJob(testUser1, null);

        // Worker A claims job
        Optional<ExecutionJobRecord> claimA = jobRepository.claimNextQueuedJob("worker-A", 10);
        assertTrue(claimA.isPresent());
        String tokenA = claimA.get().getClaimToken();

        // Simulate stale job recovery (threshold 0 force-recovers)
        int rec = jobRepository.recoverStaleJobs(0);
        assertTrue(rec >= 1);

        // Worker B claims the recovered job
        Optional<ExecutionJobRecord> claimB = jobRepository.claimNextQueuedJob("worker-B", 30);
        assertTrue(claimB.isPresent());
        String tokenB = claimB.get().getClaimToken();
        assertNotEquals(tokenA, tokenB, "Worker B must receive a different claim token");

        // Save a legitimate experiment run to satisfy foreign key constraint
        ExperimentRunRecord expRun = new ExperimentRunRecord();
        expRun.setOwnerUserId(testUser1.getId());
        expRun.setWorkloadId(testWorkload1.getId());
        expRun.setAlgorithmName("FCFS");
        expRun.setRunStatus("SUCCEEDED");
        expRun.setValidationStatus("VALID");
        ExperimentRunRecord savedRun = experimentRunRepository.save(expRun);

        // Stale Worker A wakes up and attempts to mark job completed with tokenA
        boolean completedByStaleWorker = jobRepository.markCompleted(job.getId(), tokenA, savedRun.getId(), 50L);
        assertFalse(completedByStaleWorker, "Completion by stale Worker A must be fenced and rejected");

        // Legitimate Worker B completes job with tokenB
        boolean completedByLegitWorker = jobRepository.markCompleted(job.getId(), tokenB, savedRun.getId(), 50L);
        assertTrue(completedByLegitWorker, "Completion by valid Worker B must succeed");

        Optional<ExecutionJobRecord> finalState = jobRepository.findById(job.getId());
        assertTrue(finalState.isPresent());
        assertEquals(ExecutionJobStatus.COMPLETED, finalState.get().getStatus());

        experimentRunRepository.deleteById(savedRun.getId());
    }

    @Test
    @DisplayName("Scenario 5: completeJobWithExperimentRun atomically rolls back when claim token is invalid")
    void testAtomicResultRollbackWhenLeaseLost() {
        ExecutionJobRecord job = createJob(testUser1, null);
        Optional<ExecutionJobRecord> claimed = jobRepository.claimNextQueuedJob("worker-rollback", 10);
        assertTrue(claimed.isPresent());

        // Stale recovery reclaims job or marks it failed externally
        jobRepository.markFailed(job.getId(), "Forced failure while worker was running", false);

        // Worker attempts to persist run record and complete job with old claim token
        ExperimentRunRecord runRecord = new ExperimentRunRecord();
        runRecord.setOwnerUserId(testUser1.getId());
        runRecord.setWorkloadId(testWorkload1.getId());
        runRecord.setAlgorithmName("FCFS");
        runRecord.setInputSnapshot("[]");
        runRecord.setRunStatus("SUCCEEDED");
        runRecord.setValidationStatus("VALID");

        boolean persisted = jobRepository.completeJobWithExperimentRun(job.getId(), claimed.get().getClaimToken(), runRecord, 12L);
        assertFalse(persisted, "Atomic completion must fail and rollback because job is no longer RUNNING with the claim token");

        // Verify that runRecord was not saved in experiment_runs
        if (runRecord.getId() > 0) {
            Optional<ExperimentRunRecord> orphan = experimentRunRepository.findById(runRecord.getId());
            assertTrue(orphan.isEmpty(), "No orphan experiment run record must remain in database");
        }
    }

    @Test
    @DisplayName("Scenario 6: Max attempts exhaustion transitions job to FAILED permanently")
    void testMaxAttemptsExhaustion() {
        ExecutionJobRecord job = createJob(testUser1, null);

        // Exhaust all 3 attempts
        for (int i = 0; i < 3; i++) {
            Optional<ExecutionJobRecord> cl = jobRepository.claimNextQueuedJob("worker-try-" + i, 10);
            assertTrue(cl.isPresent());
            if (i < 2) {
                // Recover for next attempt
                jobRepository.recoverStaleJobs(0);
            }
        }

        // 3rd recovery should mark FAILED because attempt_count == max_attempts (3 >= 3)
        int rec = jobRepository.recoverStaleJobs(0);
        assertTrue(rec >= 1);

        Optional<ExecutionJobRecord> failedJob = jobRepository.findById(job.getId());
        assertTrue(failedJob.isPresent());
        assertEquals(ExecutionJobStatus.FAILED, failedJob.get().getStatus());
        assertTrue(failedJob.get().getErrorMessage().contains("Exceeded maximum attempts"));

        // Retrying should not be permitted
        assertFalse(failedJob.get().isEligibleForRetry());
    }

    @Test
    @DisplayName("Scenario 7: Ownership isolation prevents User B from cancelling User A's job")
    void testOwnershipIsolation() {
        ExecutionJobRecord job = createJob(testUser1, null);

        // User 2 cannot cancel User 1's job
        assertThrows(SecurityException.class, () ->
                jobService.cancelJob(job.getId(), testUser2.getId())
        );

        // User 1 can cancel own job
        boolean cancelled = jobService.cancelJob(job.getId(), testUser1.getId());
        assertTrue(cancelled);

        Optional<ExecutionJobRecord> res = jobRepository.findById(job.getId());
        assertTrue(res.isPresent());
        assertEquals(ExecutionJobStatus.CANCELLED, res.get().getStatus());
    }

    @Test
    @DisplayName("Scenario 8: Queue metrics return accurate counts across heterogeneous states")
    void testQueueMetricsAccuracy() {
        ExecutionJobRecord j1 = createJob(testUser1, null);
        ExecutionJobRecord j2 = createJob(testUser1, null);
        ExecutionJobRecord j3 = createJob(testUser1, null);

        // Claim j2 (RUNNING)
        jobRepository.claimNextQueuedJob("worker-m1", 30);
        // Cancel j3 (CANCELLED)
        jobRepository.cancelJob(j3.getId(), testUser1.getId());

        QueueMetrics metrics = jobRepository.getQueueMetrics(testUser1.getId());
        assertEquals(1, metrics.getQueuedCount());
        assertEquals(1, metrics.getRunningCount());
        assertEquals(1, metrics.getCancelledCount());
        assertEquals(0, metrics.getCompletedCount());
        assertEquals(0, metrics.getFailedCount());
        assertEquals(3, metrics.getTotalJobs());
    }
}
