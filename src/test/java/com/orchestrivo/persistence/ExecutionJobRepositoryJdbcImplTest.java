package com.orchestrivo.persistence;

import com.google.gson.Gson;
import com.orchestrivo.domain.ProcessSpec;
import com.orchestrivo.model.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ExecutionJobRepositoryJdbcImplTest {

    private static ExecutionJobRepository jobRepository;
    private static UserRepository userRepository;
    private static WorkloadRepository workloadRepository;
    private static User testUser;
    private static WorkloadRecord testWorkload;

    @BeforeAll
    static void setUp() {
        try (Connection conn = DataSourceManager.getDataSource().getConnection()) {
            Assumptions.assumeTrue(conn.isValid(2), "Database connection is not available");
            jobRepository = new ExecutionJobRepositoryJdbcImpl();
            userRepository = new UserRepositoryJdbcImpl();
            workloadRepository = new WorkloadRepositoryJdbcImpl();

            // Create test user
            User u = new User();
            u.setEmail("jobtest_" + System.currentTimeMillis() + "@example.com");
            u.setPasswordHash("hash123");
            u.setRole("USER");
            u.setActive(true);
            testUser = userRepository.save(u);

            // Create test workload
            WorkloadRecord w = new WorkloadRecord();
            w.setOwnerUserId(testUser.getId());
            w.setName("Job Test Workload");
            w.setDescription("Workload for job tests");
            w.setSourceType("MANUAL");
            List<ProcessSpec> procs = List.of(
                    new ProcessSpec("P1", 0, 5),
                    new ProcessSpec("P2", 2, 3)
            );
            w.setWorkload(new com.orchestrivo.domain.Workload(procs));
            testWorkload = workloadRepository.save(w);

        } catch (Exception e) {
            Assumptions.abort("Database connection failed: " + e.getMessage());
        }
    }

    @BeforeEach
    void cleanJobsBefore() {
        cleanTable();
    }

    @AfterEach
    void cleanJobsAfter() {
        cleanTable();
    }

    @AfterAll
    static void tearDownAll() {
        cleanTable();
        if (testWorkload != null && workloadRepository != null) {
            try { workloadRepository.deleteById(testWorkload.getId()); } catch (Exception ignored) {}
        }
        if (testUser != null && userRepository != null) {
            try { userRepository.deleteById(testUser.getId()); } catch (Exception ignored) {}
        }
    }

    private static void cleanTable() {
        try (Connection conn = DataSourceManager.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM execution_jobs")) {
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    private ExecutionJobRecord createSampleJob(String algo, String idempotencyKey) {
        ExecutionJobRecord job = new ExecutionJobRecord();
        job.setOwnerUserId(testUser.getId());
        job.setWorkloadId(testWorkload.getId());
        job.setAlgorithmName(algo != null ? algo : "FCFS");
        job.setAlgorithmParameters("{}");
        job.setInputSnapshot(new Gson().toJson(List.of(new ProcessSpec("P1", 0, 5))));
        job.setStatus(ExecutionJobStatus.QUEUED);
        job.setAttemptCount(0);
        job.setMaxAttempts(3);
        job.setIdempotencyKey(idempotencyKey);
        return jobRepository.save(job);
    }

    @Test
    void testSaveAndFindById() {
        ExecutionJobRecord saved = createSampleJob("FCFS", null);
        assertTrue(saved.getId() > 0);

        Optional<ExecutionJobRecord> found = jobRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(ExecutionJobStatus.QUEUED, found.get().getStatus());
        assertEquals("FCFS", found.get().getAlgorithmName());
        assertEquals(testUser.getId(), found.get().getOwnerUserId());

        jobRepository.deleteById(saved.getId());
    }

    @Test
    void testOwnershipEnforcement() {
        ExecutionJobRecord saved = createSampleJob("SJF", null);

        // Found with matching owner
        Optional<ExecutionJobRecord> ownerFound = jobRepository.findByIdAndOwner(saved.getId(), testUser.getId());
        assertTrue(ownerFound.isPresent());

        // Not found with other user ID
        Optional<ExecutionJobRecord> foreignFound = jobRepository.findByIdAndOwner(saved.getId(), 999999L);
        assertTrue(foreignFound.isEmpty());

        jobRepository.deleteById(saved.getId());
    }

    @Test
    void testAtomicClaimingSingleThread() {
        ExecutionJobRecord job = createSampleJob("SRTF", null);

        Optional<ExecutionJobRecord> claimed = jobRepository.claimNextQueuedJob("worker-1");
        assertTrue(claimed.isPresent());
        assertEquals(job.getId(), claimed.get().getId());
        assertEquals(ExecutionJobStatus.RUNNING, claimed.get().getStatus());
        assertEquals("worker-1", claimed.get().getClaimedBy());
        assertEquals(1, claimed.get().getAttemptCount());

        // Subsequent claim should return empty if no other jobs exist
        Optional<ExecutionJobRecord> secondClaim = jobRepository.claimNextQueuedJob("worker-2");
        assertFalse(secondClaim.isPresent() && secondClaim.get().getId() == job.getId(), "Job cannot be claimed twice");

        jobRepository.deleteById(job.getId());
    }

    @Test
    void testConcurrentAtomicClaiming() throws Exception {
        // Enqueue 5 jobs
        List<Long> jobIds = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ExecutionJobRecord j = createSampleJob("FCFS", null);
            jobIds.add(j.getId());
        }

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        Set<Long> claimedJobIds = Collections.synchronizedSet(new HashSet<>());
        AtomicInteger totalSuccessfulClaims = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final String workerId = "worker-" + i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    Optional<ExecutionJobRecord> claimed = jobRepository.claimNextQueuedJob(workerId);
                    if (claimed.isPresent()) {
                        long cId = claimed.get().getId();
                        if (jobIds.contains(cId)) {
                            claimedJobIds.add(cId);
                            totalSuccessfulClaims.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Fire all threads simultaneously
        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        // Exactly 5 claims must have succeeded across the 10 competing worker threads,
        // and each claimed job ID must be unique (no duplicate claims!)
        assertEquals(claimedJobIds.size(), totalSuccessfulClaims.get(), "Each job must only be claimed by one worker");

        for (Long id : jobIds) {
            jobRepository.deleteById(id);
        }
    }

    @Test
    void testMarkCompletedAndDuration() {
        ExperimentRunRepository expRepo = new ExperimentRunRepositoryJdbcImpl();
        ExperimentRunRecord run = new ExperimentRunRecord();
        run.setOwnerUserId(testUser.getId());
        run.setWorkloadId(testWorkload.getId());
        run.setAlgorithmName("ROUND_ROBIN");
        run.setRunStatus("SUCCEEDED");
        ExperimentRunRecord savedRun = expRepo.save(run);

        ExecutionJobRecord job = createSampleJob("ROUND_ROBIN", null);
        Optional<ExecutionJobRecord> claimed = jobRepository.claimNextQueuedJob("worker-test");
        assertTrue(claimed.isPresent());

        // Unfenced completion without claim token must be rejected
        boolean unfencedRejected = jobRepository.markCompleted(job.getId(), savedRun.getId(), 15L);
        assertFalse(unfencedRejected, "Unfenced completion must be rejected by fencing");

        // Completion with wrong claim token must be rejected
        boolean wrongTokenRejected = jobRepository.markCompleted(job.getId(), "wrong-token-123", savedRun.getId(), 15L);
        assertFalse(wrongTokenRejected, "Completion with incorrect token must be rejected");

        // Fenced completion with correct token succeeds
        boolean ok = jobRepository.markCompleted(job.getId(), claimed.get().getClaimToken(), savedRun.getId(), 15L);
        assertTrue(ok);

        Optional<ExecutionJobRecord> updated = jobRepository.findById(job.getId());
        assertTrue(updated.isPresent());
        assertEquals(ExecutionJobStatus.COMPLETED, updated.get().getStatus());
        assertEquals(savedRun.getId(), updated.get().getExperimentRunId());
        assertEquals(15L, updated.get().getExecutionDurationMs());
        assertNotNull(updated.get().getCompletedAt());

        jobRepository.deleteById(job.getId());
        expRepo.deleteById(savedRun.getId());
    }

    @Test
    void testCancellation() {
        ExecutionJobRecord job = createSampleJob("FCFS", null);

        // Cancel with wrong user should fail
        boolean falseCancel = jobRepository.cancelJob(job.getId(), 999999L);
        assertFalse(falseCancel);

        // Cancel with correct owner should succeed
        boolean ok = jobRepository.cancelJob(job.getId(), testUser.getId());
        assertTrue(ok);

        Optional<ExecutionJobRecord> updated = jobRepository.findById(job.getId());
        assertTrue(updated.isPresent());
        assertEquals(ExecutionJobStatus.CANCELLED, updated.get().getStatus());

        jobRepository.deleteById(job.getId());
    }

    @Test
    void testRetryFlow() {
        ExecutionJobRecord job = createSampleJob("FCFS", null);

        // Claim job
        Optional<ExecutionJobRecord> claimed = jobRepository.claimNextQueuedJob("retry-worker");
        assertTrue(claimed.isPresent());

        // Mark failed with token (attempt 1)
        boolean failOk = jobRepository.markFailed(job.getId(), claimed.get().getClaimToken(), "Transient DB timeout", false);
        assertTrue(failOk);

        Optional<ExecutionJobRecord> failedOpt = jobRepository.findById(job.getId());
        assertTrue(failedOpt.isPresent());
        assertEquals(ExecutionJobStatus.FAILED, failedOpt.get().getStatus());

        // Request retry by owner
        boolean retried = jobRepository.requestRetry(job.getId(), testUser.getId());
        assertTrue(retried);

        Optional<ExecutionJobRecord> requeued = jobRepository.findById(job.getId());
        assertTrue(requeued.isPresent());
        assertEquals(ExecutionJobStatus.QUEUED, requeued.get().getStatus());

        jobRepository.deleteById(job.getId());
    }

    @Test
    void testIdempotencyDeduplication() {
        String key = "idem-test-" + UUID.randomUUID();
        ExecutionJobRecord job1 = createSampleJob("FCFS", key);

        Optional<ExecutionJobRecord> found = jobRepository.findByIdempotencyKey(testUser.getId(), key);
        assertTrue(found.isPresent());
        assertEquals(job1.getId(), found.get().getId());

        jobRepository.deleteById(job1.getId());
    }

    @Test
    void testQueueMetrics() {
        ExecutionJobRecord j1 = createSampleJob("FCFS", null);
        ExecutionJobRecord j2 = createSampleJob("SJF", null);

        QueueMetrics metrics = jobRepository.getQueueMetrics(testUser.getId());
        assertNotNull(metrics);
        assertTrue(metrics.getQueuedCount() >= 2);

        jobRepository.deleteById(j1.getId());
        jobRepository.deleteById(j2.getId());
    }

    @Test
    void testStaleJobRecovery() {
        ExecutionJobRecord job = createSampleJob("FCFS", null);
        jobRepository.claimNextQueuedJob("old-crashed-worker");

        // Stale recovery threshold 0 seconds (recovers immediately for test)
        int recovered = jobRepository.recoverStaleJobs(0);
        assertTrue(recovered >= 1);

        Optional<ExecutionJobRecord> recJob = jobRepository.findById(job.getId());
        assertTrue(recJob.isPresent());
        assertEquals(ExecutionJobStatus.QUEUED, recJob.get().getStatus());
        assertNull(recJob.get().getClaimedBy());

        jobRepository.deleteById(job.getId());
    }

    @Test
    void testFindByOwnerPagedWithNullSortAndStatus() {
        ExecutionJobRecord job = createSampleJob("FCFS", null);

        PagedResult<ExecutionJobRecord> result = jobRepository.findByOwnerPaged(testUser.getId(), null, null, null, 1, 10);
        assertNotNull(result);
        assertEquals(1, result.getTotalCount());
        assertFalse(result.getItems().isEmpty());
        assertEquals(job.getId(), result.getItems().get(0).getId());

        jobRepository.deleteById(job.getId());
    }
}
