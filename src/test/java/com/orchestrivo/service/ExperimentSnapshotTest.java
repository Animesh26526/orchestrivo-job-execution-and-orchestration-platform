package com.orchestrivo.service;

import com.orchestrivo.domain.ProcessSpec;
import com.orchestrivo.model.ExperimentRunRecord;
import com.orchestrivo.model.User;
import com.orchestrivo.model.WorkloadRecord;
import com.orchestrivo.persistence.*;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ExperimentSnapshotTest {

    private static ExperimentService experimentService;
    private static WorkloadService workloadService;
    private static ExperimentRunRepository runRepository;
    private static WorkloadRepository workloadRepository;
    private static UserRepository userRepository;

    @BeforeAll
    static void setUp() {
        try (Connection conn = DataSourceManager.getDataSource().getConnection()) {
            Assumptions.assumeTrue(conn.isValid(2), "Database connection is not available");
            userRepository = new UserRepositoryJdbcImpl();
            workloadRepository = new WorkloadRepositoryJdbcImpl();
            runRepository = new ExperimentRunRepositoryJdbcImpl();
            workloadService = new WorkloadService(workloadRepository);
            experimentService = new ExperimentService(runRepository);
        } catch (Exception e) {
            Assumptions.abort("Database connection failed, skipping tests");
        }
    }

    @Test
    void testExperimentSnapshotPreservedAfterWorkloadMutation() {
        User user = new User();
        user.setEmail("snap_test_" + System.currentTimeMillis() + "@example.com");
        user.setPasswordHash("hash");
        user.setRole("USER");
        user.setActive(true);
        user = userRepository.save(user);

        // 1. Create original workload with 2 processes
        ProcessSpec p1 = new ProcessSpec("P1", 0, 5);
        ProcessSpec p2 = new ProcessSpec("P2", 2, 8);
        WorkloadRecord workload = workloadService.createManualWorkload(user.getId(), "Snapshot Original", "Testing Immutability", Arrays.asList(p1, p2));

        // 2. Execute experiment run
        ExperimentRunRecord run = experimentService.runExperiment(workload, "FCFS", 0, user.getId());
        assertTrue(run.getId() > 0);
        assertNotNull(run.getInputSnapshot());
        List<ProcessSpec> initialSnapshot = run.getSnapshotProcesses();
        assertEquals(2, initialSnapshot.size());
        assertEquals("P1", initialSnapshot.get(0).getProcessId());
        assertEquals(5, initialSnapshot.get(0).getBurstTime());

        // 3. Mutate the original workload (change burst times and add a new process)
        ProcessSpec p1Mutated = new ProcessSpec("P1", 0, 999);
        ProcessSpec p2Mutated = new ProcessSpec("P2", 2, 888);
        ProcessSpec p3New = new ProcessSpec("P3", 4, 777);
        workloadService.updateWorkload(workload.getId(), user.getId(), "Mutated Workload", "Altered", Arrays.asList(p1Mutated, p2Mutated, p3New));

        // 4. Fetch the historical experiment run again from DB
        Optional<ExperimentRunRecord> fetchedRun = experimentService.getExperimentById(run.getId(), user.getId());
        assertTrue(fetchedRun.isPresent());
        List<ProcessSpec> preservedProcesses = fetchedRun.get().getSnapshotProcesses();

        // 5. Verify that experiment snapshot was NOT affected by workload mutations
        assertEquals(2, preservedProcesses.size(), "Experiment snapshot must maintain original 2 processes");
        assertEquals("P1", preservedProcesses.get(0).getProcessId());
        assertEquals(5, preservedProcesses.get(0).getBurstTime(), "Original burst time must be preserved in snapshot");
        assertEquals("P2", preservedProcesses.get(1).getProcessId());
        assertEquals(8, preservedProcesses.get(1).getBurstTime(), "Original burst time must be preserved in snapshot");

        // Cleanup
        runRepository.deleteById(run.getId());
        workloadRepository.deleteById(workload.getId());
        userRepository.deleteById(user.getId());
    }
}
