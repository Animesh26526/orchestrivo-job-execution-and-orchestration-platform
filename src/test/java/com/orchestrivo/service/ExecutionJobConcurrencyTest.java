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

        // Stale recovery reclaims job (threshold 0 force-recovers and resets claim token)
        int rec = jobRepository.recoverStaleJobs(0);
        assertTrue(rec >= 1, "Stale recovery must reclaim the running job");

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

    @Test
    @DisplayName("Scenario 9: Strict claim token fencing rejects null, empty, wrong tokens, and null stored tokens")
    void testStrictClaimFencingAdversarialTokens() {
        ExecutionJobRecord job = createJob(testUser1, null);

        // 1. Unclaimed job in QUEUED state has null stored claim_token
        // All fenced mutations must be rejected because stored token is null / status is not RUNNING
        assertFalse(jobRepository.renewLease(job.getId(), "some-token", 30), "renewLease must reject when stored token is null");
        assertFalse(jobRepository.markCompleted(job.getId(), "some-token", 100L, 50L), "markCompleted must reject when job is QUEUED");
        assertFalse(jobRepository.markCompleted(job.getId(), null, 100L, 50L), "markCompleted must reject null token");
        assertFalse(jobRepository.markFailed(job.getId(), "some-token", "fail", false), "markFailed must reject when job is QUEUED");
        assertFalse(jobRepository.markFailed(job.getId(), null, "fail", false), "markFailed must reject null token");

        ExperimentRunRecord runRecord = new ExperimentRunRecord();
        runRecord.setOwnerUserId(testUser1.getId());
        runRecord.setWorkloadId(testWorkload1.getId());
        runRecord.setAlgorithmName("FCFS");
        runRecord.setInputSnapshot("[]");
        runRecord.setRunStatus("SUCCEEDED");
        runRecord.setValidationStatus("VALID");

        assertFalse(jobRepository.completeJobWithExperimentRun(job.getId(), null, runRecord, 10L), "completeJobWithExperimentRun must reject null token");
        assertFalse(jobRepository.completeJobWithExperimentRun(job.getId(), "", runRecord, 10L), "completeJobWithExperimentRun must reject empty token");
        assertFalse(jobRepository.completeJobWithExperimentRun(job.getId(), "some-token", runRecord, 10L), "completeJobWithExperimentRun must reject wrong token on QUEUED job");

        // Verify database state remains unchanged: still QUEUED, attempt_count 0
        ExecutionJobRecord current = jobRepository.findById(job.getId()).orElseThrow();
        assertEquals(ExecutionJobStatus.QUEUED, current.getStatus());
        assertEquals(0, current.getAttemptCount());

        // 2. Now claim the job (status RUNNING, non-null claim_token stored)
        Optional<ExecutionJobRecord> claimed = jobRepository.claimNextQueuedJob("worker-adv", 30);
        assertTrue(claimed.isPresent());
        String legitToken = claimed.get().getClaimToken();
        assertNotNull(legitToken);

        // Mutations with wrong tokens or null/empty tokens must be rejected
        assertFalse(jobRepository.renewLease(job.getId(), null, 30));
        assertFalse(jobRepository.renewLease(job.getId(), "", 30));
        assertFalse(jobRepository.renewLease(job.getId(), "wrong-token-abc", 30));

        assertFalse(jobRepository.markCompleted(job.getId(), null, 100L, 50L));
        assertFalse(jobRepository.markCompleted(job.getId(), "", 100L, 50L));
        assertFalse(jobRepository.markCompleted(job.getId(), "wrong-token-abc", 100L, 50L));

        assertFalse(jobRepository.markFailed(job.getId(), null, "error", false));
        assertFalse(jobRepository.markFailed(job.getId(), "", "error", false));
        assertFalse(jobRepository.markFailed(job.getId(), "wrong-token-abc", "error", false));

        assertFalse(jobRepository.completeJobWithExperimentRun(job.getId(), null, runRecord, 10L));
        assertFalse(jobRepository.completeJobWithExperimentRun(job.getId(), "wrong-token-abc", runRecord, 10L));

        // State remains RUNNING under legitimate token
        ExecutionJobRecord stillRunning = jobRepository.findById(job.getId()).orElseThrow();
        assertEquals(ExecutionJobStatus.RUNNING, stillRunning.getStatus());
        assertEquals(legitToken, stillRunning.getClaimToken());
    }

    @Test
    @DisplayName("Scenario 10: Reclaimed job prevents old worker from finishing after new attempt starts")
    void testOldWorkerFinishingAfterNewAttemptStarts() {
        ExecutionJobRecord job = createJob(testUser1, null);

        // Worker A claims attempt 1
        Optional<ExecutionJobRecord> claimA = jobRepository.claimNextQueuedJob("worker-A", 10);
        assertTrue(claimA.isPresent());
        String tokenA = claimA.get().getClaimToken();
        assertEquals(1, claimA.get().getAttemptCount());

        // Worker A stalls; lease expires; stale recovery recovers job back to QUEUED
        int recovered = jobRepository.recoverStaleJobs(0);
        assertTrue(recovered >= 1);

        // Worker B claims attempt 2
        Optional<ExecutionJobRecord> claimB = jobRepository.claimNextQueuedJob("worker-B", 30);
        assertTrue(claimB.isPresent());
        String tokenB = claimB.get().getClaimToken();
        assertEquals(2, claimB.get().getAttemptCount());
        assertNotEquals(tokenA, tokenB);

        // Stale Worker A wakes up and tries to complete job using tokenA
        ExperimentRunRecord runRecordA = new ExperimentRunRecord();
        runRecordA.setOwnerUserId(testUser1.getId());
        runRecordA.setWorkloadId(testWorkload1.getId());
        runRecordA.setAlgorithmName("FCFS");
        runRecordA.setInputSnapshot("[]");
        runRecordA.setRunStatus("SUCCEEDED");
        runRecordA.setValidationStatus("VALID");

        boolean completeA = jobRepository.completeJobWithExperimentRun(job.getId(), tokenA, runRecordA, 40L);
        assertFalse(completeA, "Stale worker A must be fenced out and rejected");

        // Verify runRecordA was rolled back and not persisted
        if (runRecordA.getId() > 0) {
            assertTrue(experimentRunRepository.findById(runRecordA.getId()).isEmpty());
        }

        // Legitimate Worker B completes job with tokenB
        ExperimentRunRecord runRecordB = new ExperimentRunRecord();
        runRecordB.setOwnerUserId(testUser1.getId());
        runRecordB.setWorkloadId(testWorkload1.getId());
        runRecordB.setAlgorithmName("FCFS");
        runRecordB.setInputSnapshot("[]");
        runRecordB.setRunStatus("SUCCEEDED");
        runRecordB.setValidationStatus("VALID");

        boolean completeB = jobRepository.completeJobWithExperimentRun(job.getId(), tokenB, runRecordB, 35L);
        assertTrue(completeB, "Legitimate worker B must successfully complete job");

        // Verify final database state: COMPLETED, attempt_count = 2, completed_at not null, experiment_run_id = runRecordB
        ExecutionJobRecord finalRecord = jobRepository.findById(job.getId()).orElseThrow();
        assertEquals(ExecutionJobStatus.COMPLETED, finalRecord.getStatus());
        assertEquals(2, finalRecord.getAttemptCount());
        assertEquals(runRecordB.getId(), finalRecord.getExperimentRunId());
        assertNotNull(finalRecord.getCompletedAt());
        assertNull(finalRecord.getClaimToken());

        experimentRunRepository.deleteById(runRecordB.getId());
    }

    @Test
    @DisplayName("Scenario 11: Concurrent completion and failure race resolves safely with exactly one terminal state")
    void testConcurrentCompletionAndFailureRace() throws Exception {
        ExecutionJobRecord job = createJob(testUser1, null);
        Optional<ExecutionJobRecord> claimed = jobRepository.claimNextQueuedJob("worker-race", 30);
        assertTrue(claimed.isPresent());
        String token = claimed.get().getClaimToken();

        ExperimentRunRecord runRecord = new ExperimentRunRecord();
        runRecord.setOwnerUserId(testUser1.getId());
        runRecord.setWorkloadId(testWorkload1.getId());
        runRecord.setAlgorithmName("FCFS");
        runRecord.setInputSnapshot("[]");
        runRecord.setRunStatus("SUCCEEDED");
        runRecord.setValidationStatus("VALID");

        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Future<Boolean> completionFuture = executor.submit(() -> {
            barrier.await();
            return jobRepository.completeJobWithExperimentRun(job.getId(), token, runRecord, 25L);
        });

        Future<Boolean> failureFuture = executor.submit(() -> {
            barrier.await();
            return jobRepository.markFailed(job.getId(), token, "Hardware fault", false);
        });

        boolean completed = completionFuture.get(5, TimeUnit.SECONDS);
        boolean failed = failureFuture.get(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Exactly one transition must have succeeded
        assertTrue(completed ^ failed, "Exactly one of concurrent completion or failure must succeed");

        ExecutionJobRecord finalState = jobRepository.findById(job.getId()).orElseThrow();
        if (completed) {
            assertEquals(ExecutionJobStatus.COMPLETED, finalState.getStatus());
            assertEquals(runRecord.getId(), finalState.getExperimentRunId());
            experimentRunRepository.deleteById(runRecord.getId());
        } else {
            assertEquals(ExecutionJobStatus.FAILED, finalState.getStatus());
            assertEquals("Hardware fault", finalState.getErrorMessage());
        }
    }

    @Test
    @DisplayName("Scenario 12: Cancellation racing with worker claim: claimed job cannot be cancelled, cancelled job cannot be claimed")
    void testCancellationRacingWithWorkerClaim() {
        // Part A: Worker claims job first -> User cancellation must fail
        ExecutionJobRecord j1 = createJob(testUser1, null);
        Optional<ExecutionJobRecord> claimed1 = jobRepository.claimNextQueuedJob("worker-c1", 30);
        assertTrue(claimed1.isPresent());

        boolean cancelSucceeded = jobRepository.cancelJob(j1.getId(), testUser1.getId());
        assertFalse(cancelSucceeded, "User cannot cancel job that is already RUNNING");

        ExecutionJobRecord state1 = jobRepository.findById(j1.getId()).orElseThrow();
        assertEquals(ExecutionJobStatus.RUNNING, state1.getStatus());

        // Part B: User cancels job first -> Worker claim must not claim cancelled job
        ExecutionJobRecord j2 = createJob(testUser1, null);
        boolean userCancelled = jobRepository.cancelJob(j2.getId(), testUser1.getId());
        assertTrue(userCancelled, "User can cancel QUEUED job");

        ExecutionJobRecord state2 = jobRepository.findById(j2.getId()).orElseThrow();
        assertEquals(ExecutionJobStatus.CANCELLED, state2.getStatus());

        Optional<ExecutionJobRecord> claimed2 = jobRepository.claimNextQueuedJob("worker-c2", 30);
        // j2 was the only queued job, so claimed2 must be empty
        assertTrue(claimed2.isEmpty() || claimed2.get().getId() != j2.getId(), "Worker cannot claim a CANCELLED job");
    }
}
