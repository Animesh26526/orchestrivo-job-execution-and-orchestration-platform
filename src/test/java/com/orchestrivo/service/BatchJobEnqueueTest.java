package com.orchestrivo.service;

import com.orchestrivo.domain.ProcessSpec;
import com.orchestrivo.domain.Workload;
import com.orchestrivo.model.AlgorithmConfigSpec;
import com.orchestrivo.model.BatchJobResult;
import com.orchestrivo.model.ExecutionJobRecord;
import com.orchestrivo.model.ExecutionJobStatus;
import com.orchestrivo.model.WorkloadRecord;
import com.orchestrivo.persistence.DataSourceManager;
import com.orchestrivo.persistence.ExecutionJobRepositoryJdbcImpl;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BatchJobEnqueueTest {

    private static ExecutionJobService jobService;
    private static ExecutionJobRepositoryJdbcImpl jobRepository;
    private static com.orchestrivo.persistence.UserRepository userRepository;
    private static com.orchestrivo.persistence.WorkloadRepository workloadRepository;
    private static com.orchestrivo.model.User testUser;
    private static WorkloadRecord testWorkload;

    @BeforeAll
    static void setUp() {
        try (Connection conn = DataSourceManager.getDataSource().getConnection()) {
            Assumptions.assumeTrue(conn != null && conn.isValid(2), "MySQL not available, skipping test");
            jobRepository = new ExecutionJobRepositoryJdbcImpl();
            jobService = new ExecutionJobService(jobRepository);
            userRepository = new com.orchestrivo.persistence.UserRepositoryJdbcImpl();
            workloadRepository = new com.orchestrivo.persistence.WorkloadRepositoryJdbcImpl();

            com.orchestrivo.model.User u = new com.orchestrivo.model.User();
            u.setEmail("batchtest_" + System.currentTimeMillis() + "@example.com");
            u.setPasswordHash("hash123");
            u.setFullName("Batch Tester");
            u.setRole("USER");
            u.setActive(true);
            testUser = userRepository.save(u);

            WorkloadRecord w = new WorkloadRecord();
            w.setOwnerUserId(testUser.getId());
            w.setName("Batch Test Workload");
            w.setDescription("Workload for batch test");
            w.setSourceType("MANUAL");
            List<ProcessSpec> procs = List.of(
                    new ProcessSpec("P1", 0, 5),
                    new ProcessSpec("P2", 1, 3),
                    new ProcessSpec("P3", 2, 8)
            );
            w.setWorkload(new Workload(procs));
            testWorkload = workloadRepository.save(w);
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "MySQL not available, skipping test: " + e.getMessage());
        }
    }

    @Test
    void testEnqueueBatchSuccess() {
        long userId = testUser.getId();
        WorkloadRecord wr = testWorkload;
        long workloadId = wr.getId();

        List<AlgorithmConfigSpec> configs = Arrays.asList(
                new AlgorithmConfigSpec("FCFS"),
                new AlgorithmConfigSpec("SJF"),
                new AlgorithmConfigSpec("SRTF"),
                new AlgorithmConfigSpec("ROUND_ROBIN", 4)
        );

        BatchJobResult result = jobService.enqueueBatch(wr, configs, userId);

        assertNotNull(result);
        assertNotNull(result.getBatchId());
        assertTrue(result.getBatchId().startsWith("batch-"));
        assertEquals(4, result.getJobCount());
        assertEquals(workloadId, result.getWorkloadId());

        List<ExecutionJobRecord> createdJobs = result.getCreatedJobs();
        assertEquals(4, createdJobs.size());

        for (ExecutionJobRecord job : createdJobs) {
            assertTrue(job.getId() > 0);
            assertEquals(ExecutionJobStatus.QUEUED, job.getStatus());
            assertEquals(userId, job.getOwnerUserId());
            assertEquals(workloadId, job.getWorkloadId());
            assertEquals(result.getBatchId(), job.getBatchId());
        }

        // Verify retrieval by batch ID
        List<ExecutionJobRecord> fetched = jobService.getJobsByBatchId(result.getBatchId(), userId);
        assertEquals(4, fetched.size());
    }

    @Test
    void testBatchSizeLimitEnforced() {
        long userId = testUser.getId();
        WorkloadRecord wr = testWorkload;

        List<AlgorithmConfigSpec> configs = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            configs.add(new AlgorithmConfigSpec("FCFS"));
        }

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                jobService.enqueueBatch(wr, configs, userId));
        assertTrue(ex.getMessage().contains("Maximum 20 jobs allowed"));
    }

    @Test
    void testAtomicValidationRejection() {
        long userId = testUser.getId();
        WorkloadRecord wr = testWorkload;

        // Mix valid with invalid algorithm
        List<AlgorithmConfigSpec> invalidAlgoConfigs = Arrays.asList(
                new AlgorithmConfigSpec("FCFS"),
                new AlgorithmConfigSpec("INVALID_ALGO")
        );
        assertThrows(IllegalArgumentException.class, () ->
                jobService.enqueueBatch(wr, invalidAlgoConfigs, userId));

        // Invalid RR quantum (< 1)
        List<AlgorithmConfigSpec> invalidQuantumConfigs = Arrays.asList(
                new AlgorithmConfigSpec("FCFS"),
                new AlgorithmConfigSpec("ROUND_ROBIN", 0)
        );
        assertThrows(IllegalArgumentException.class, () ->
                jobService.enqueueBatch(wr, invalidQuantumConfigs, userId));
    }

    @Test
    void testOwnershipEnforcement() {
        long attackerUserId = testUser.getId() + 9999L;
        WorkloadRecord wr = testWorkload;

        List<AlgorithmConfigSpec> configs = List.of(new AlgorithmConfigSpec("FCFS"));

        assertThrows(SecurityException.class, () ->
                jobService.enqueueBatch(wr, configs, attackerUserId));
    }

    @Test
    void testFormattedDurationMicrosecondFidelity() {
        ExecutionJobRecord job = new ExecutionJobRecord();

        // 85.4 microseconds = 85,400 nanos
        job.setExecutionDurationNanos(85_400L);
        String formattedMicro = job.getFormattedDuration();
        assertNotNull(formattedMicro);
        assertTrue(formattedMicro.contains("µs"), "Sub-millisecond should show µs unit, was: " + formattedMicro);
        assertTrue(formattedMicro.contains("85.4"), "Expected 85.4 µs, was: " + formattedMicro);

        // 2.35 milliseconds = 2,350,000 nanos
        job.setExecutionDurationNanos(2_350_000L);
        String formattedMilli = job.getFormattedDuration();
        assertTrue(formattedMilli.contains("ms"));
        assertTrue(formattedMilli.contains("2.35"));

        // 1.5 seconds = 1,500,000,000 nanos
        job.setExecutionDurationNanos(1_500_000_000L);
        String formattedSec = job.getFormattedDuration();
        assertTrue(formattedSec.contains("s"));
    }
}
