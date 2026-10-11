package com.orchestrivo.service;

import com.google.gson.Gson;
import com.orchestrivo.domain.ProcessSpec;
import com.orchestrivo.domain.Workload;
import com.orchestrivo.model.*;
import com.orchestrivo.persistence.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ExecutionWorkerManagerTest {

    private static ExecutionJobRepository jobRepository;
    private static ExperimentRunRepository experimentRunRepository;
    private static UserRepository userRepository;
    private static WorkloadRepository workloadRepository;
    private static User testUser;
    private static WorkloadRecord testWorkload;

    @BeforeAll
    static void setUp() {
        try (Connection conn = DataSourceManager.getDataSource().getConnection()) {
            Assumptions.assumeTrue(conn.isValid(2), "Database connection is not available");
            jobRepository = new ExecutionJobRepositoryJdbcImpl();
            experimentRunRepository = new ExperimentRunRepositoryJdbcImpl();
            userRepository = new UserRepositoryJdbcImpl();
            workloadRepository = new WorkloadRepositoryJdbcImpl();

            User u = new User();
            u.setEmail("workertest_" + System.currentTimeMillis() + "@example.com");
            u.setPasswordHash("hash123");
            u.setRole("USER");
            u.setActive(true);
            testUser = userRepository.save(u);

            WorkloadRecord w = new WorkloadRecord();
            w.setOwnerUserId(testUser.getId());
            w.setName("Worker Test Workload");
            w.setSourceType("MANUAL");
            List<ProcessSpec> procs = List.of(
                    new ProcessSpec("P1", 0, 4),
                    new ProcessSpec("P2", 1, 2)
            );
            w.setWorkload(new Workload(procs));
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

    @Test
    void testExecuteJobSuccessfulFcfs() {
        ExecutionWorkerManager manager = new ExecutionWorkerManager(jobRepository, experimentRunRepository, null, 1);

        ExecutionJobRecord job = new ExecutionJobRecord();
        job.setOwnerUserId(testUser.getId());
        job.setWorkloadId(testWorkload.getId());
        job.setAlgorithmName("FCFS");
        job.setAlgorithmParameters("{}");
        job.setInputSnapshot(new Gson().toJson(List.of(
                new ProcessSpec("P1", 0, 4),
                new ProcessSpec("P2", 1, 2)
        )));
        job.setStatus(ExecutionJobStatus.QUEUED);
        job.setMaxAttempts(3);
        ExecutionJobRecord savedJob = jobRepository.save(job);

        // Claim
        Optional<ExecutionJobRecord> claimed = jobRepository.claimNextQueuedJob("test-worker");
        assertTrue(claimed.isPresent());

        // Execute directly
        manager.executeJob(claimed.get());

        // Verify job state
        Optional<ExecutionJobRecord> completedJob = jobRepository.findById(savedJob.getId());
        assertTrue(completedJob.isPresent());
        assertEquals(ExecutionJobStatus.COMPLETED, completedJob.get().getStatus());
        assertNotNull(completedJob.get().getExperimentRunId());
        assertTrue(completedJob.get().getExecutionDurationMs() >= 0);

        // Verify experiment record persisted
        Optional<ExperimentRunRecord> run = experimentRunRepository.findById(completedJob.get().getExperimentRunId());
        assertTrue(run.isPresent());
        assertEquals("SUCCEEDED", run.get().getRunStatus());
        assertEquals("VALID", run.get().getValidationStatus());
        assertNotNull(run.get().getResult());
        assertEquals(2, run.get().getResult().getProcessMetrics().size());

        experimentRunRepository.deleteById(run.get().getId());
        jobRepository.deleteById(savedJob.getId());
    }

    @Test
    void testExecuteJobRoundRobinWithQuantum() {
        ExecutionWorkerManager manager = new ExecutionWorkerManager(jobRepository, experimentRunRepository, null, 1);

        ExecutionJobRecord job = new ExecutionJobRecord();
        job.setOwnerUserId(testUser.getId());
        job.setWorkloadId(testWorkload.getId());
        job.setAlgorithmName("ROUND_ROBIN");
        job.setAlgorithmParameters("{\"quantum\":2}");
        job.setInputSnapshot(new Gson().toJson(List.of(
                new ProcessSpec("P1", 0, 5),
                new ProcessSpec("P2", 0, 3)
        )));
        job.setStatus(ExecutionJobStatus.QUEUED);
        job.setMaxAttempts(3);
        ExecutionJobRecord savedJob = jobRepository.save(job);

        Optional<ExecutionJobRecord> claimed = jobRepository.claimNextQueuedJob("test-worker");
        assertTrue(claimed.isPresent());

        manager.executeJob(claimed.get());

        Optional<ExecutionJobRecord> completedJob = jobRepository.findById(savedJob.getId());
        assertTrue(completedJob.isPresent());
        assertEquals(ExecutionJobStatus.COMPLETED, completedJob.get().getStatus());
        assertNotNull(completedJob.get().getExperimentRunId());

        experimentRunRepository.deleteById(completedJob.get().getExperimentRunId());
        jobRepository.deleteById(savedJob.getId());
    }

    @Test
    void testExecuteJobWithInvalidSnapshotFailsPermanently() {
        ExecutionWorkerManager manager = new ExecutionWorkerManager(jobRepository, experimentRunRepository, null, 1);

        ExecutionJobRecord job = new ExecutionJobRecord();
        job.setOwnerUserId(testUser.getId());
        job.setWorkloadId(testWorkload.getId());
        job.setAlgorithmName("FCFS");
        job.setAlgorithmParameters("{}");
        job.setInputSnapshot("[]"); // Empty snapshot
        job.setStatus(ExecutionJobStatus.QUEUED);
        job.setAttemptCount(2);
        job.setMaxAttempts(3);
        ExecutionJobRecord savedJob = jobRepository.save(job);

        Optional<ExecutionJobRecord> claimed = jobRepository.claimNextQueuedJob("test-worker");
        assertTrue(claimed.isPresent());

        manager.executeJob(claimed.get());

        Optional<ExecutionJobRecord> failedJob = jobRepository.findById(savedJob.getId());
        assertTrue(failedJob.isPresent());
        assertNotNull(failedJob.get().getErrorMessage());

        jobRepository.deleteById(savedJob.getId());
    }

    @Test
    void testWorkerManagerLifecycle() {
        ExecutionWorkerManager manager = new ExecutionWorkerManager(jobRepository, experimentRunRepository, null, 2);
        assertFalse(manager.isRunning());

        manager.start();
        assertTrue(manager.isRunning());

        manager.triggerImmediatePoll();

        manager.shutdown();
        assertFalse(manager.isRunning());
    }
}
