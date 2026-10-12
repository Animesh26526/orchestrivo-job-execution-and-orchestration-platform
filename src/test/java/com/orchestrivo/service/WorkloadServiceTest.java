package com.orchestrivo.service;

import com.orchestrivo.domain.ProcessSpec;
import com.orchestrivo.model.User;
import com.orchestrivo.model.WorkloadRecord;
import com.orchestrivo.persistence.DataSourceManager;
import com.orchestrivo.persistence.UserRepositoryJdbcImpl;
import com.orchestrivo.persistence.WorkloadRepositoryJdbcImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WorkloadServiceTest {
    private static WorkloadService workloadService;
    private static AuthenticationService authService;
    private static DataSourceManager dsm;

    @BeforeAll
    static void setUp() {
        try (Connection conn = DataSourceManager.getDataSource().getConnection()) {
            Assumptions.assumeTrue(conn != null && conn.isValid(2), "MySQL not available, skipping test");
            workloadService = new WorkloadService(new WorkloadRepositoryJdbcImpl());
            authService = new AuthenticationService(new UserRepositoryJdbcImpl());
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "MySQL not available, skipping test");
        }
    }

    @Test
    void testWorkloadOwnership() {
        String email1 = "wtest1" + UUID.randomUUID() + "@example.com";
        String email2 = "wtest2" + UUID.randomUUID() + "@example.com";
        User u1 = authService.register(email1, "pass", "USER");
        User u2 = authService.register(email2, "pass", "USER");

        List<ProcessSpec> processes = Arrays.asList(new ProcessSpec("P1", 0, 5));
        WorkloadRecord wr = workloadService.createManualWorkload(u1.getId(), "Test WL", "Desc", processes);

        assertTrue(wr.getId() > 0);

        // Owner can access
        Optional<WorkloadRecord> opt1 = workloadService.getWorkloadById(wr.getId(), u1.getId());
        assertTrue(opt1.isPresent());

        // Non-owner cannot access
        Optional<WorkloadRecord> opt2 = workloadService.getWorkloadById(wr.getId(), u2.getId());
        assertFalse(opt2.isPresent());

        // Non-owner cannot delete
        assertThrows(SecurityException.class, () -> workloadService.deleteWorkload(wr.getId(), u2.getId()));

        // Owner can delete
        workloadService.deleteWorkload(wr.getId(), u1.getId());
        assertFalse(workloadService.getWorkloadById(wr.getId(), u1.getId()).isPresent());

        // Deleting nonexistent workload throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> workloadService.deleteWorkload(wr.getId(), u1.getId()));
    }
}
