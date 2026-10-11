package com.orchestrivo.service;

import com.orchestrivo.domain.ProcessSpec;
import com.orchestrivo.model.PagedResult;
import com.orchestrivo.model.User;
import com.orchestrivo.model.WorkloadRecord;
import com.orchestrivo.persistence.DataSourceManager;
import com.orchestrivo.persistence.UserRepository;
import com.orchestrivo.persistence.UserRepositoryJdbcImpl;
import com.orchestrivo.persistence.WorkloadRepository;
import com.orchestrivo.persistence.WorkloadRepositoryJdbcImpl;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class WorkloadLifecycleTest {

    private static WorkloadService workloadService;
    private static WorkloadRepository workloadRepository;
    private static UserRepository userRepository;

    @BeforeAll
    static void setUp() {
        try (Connection conn = DataSourceManager.getDataSource().getConnection()) {
            Assumptions.assumeTrue(conn.isValid(2), "Database connection is not available");
            workloadRepository = new WorkloadRepositoryJdbcImpl();
            userRepository = new UserRepositoryJdbcImpl();
            workloadService = new WorkloadService(workloadRepository);
        } catch (Exception e) {
            Assumptions.abort("Database connection failed, skipping tests");
        }
    }

    @Test
    void testWorkloadUpdateLifecycle() {
        User user = new User();
        user.setEmail("lifecycle_upd_" + System.currentTimeMillis() + "@example.com");
        user.setPasswordHash("hash");
        user.setRole("USER");
        user.setActive(true);
        user = userRepository.save(user);

        // 1. Create workload
        WorkloadRecord created = workloadService.createManualWorkload(
                user.getId(), "Initial Name", "Initial Desc",
                List.of(new ProcessSpec("P1", 0, 5))
        );
        assertTrue(created.getId() > 0);

        // 2. Update workload
        List<ProcessSpec> updatedProcesses = Arrays.asList(
                new ProcessSpec("P1", 0, 3),
                new ProcessSpec("P2", 1, 8)
        );
        WorkloadRecord updated = workloadService.updateWorkload(
                created.getId(), user.getId(), "Updated Name", "Updated Desc", updatedProcesses
        );

        assertEquals("Updated Name", updated.getName());
        assertEquals("Updated Desc", updated.getDescription());
        assertEquals(2, updated.getWorkload().getProcesses().size());

        // Verify from DB
        Optional<WorkloadRecord> fetched = workloadService.getWorkloadById(created.getId(), user.getId());
        assertTrue(fetched.isPresent());
        assertEquals("Updated Name", fetched.get().getName());
        assertEquals(2, fetched.get().getWorkload().getProcesses().size());

        // Cleanup
        workloadRepository.deleteById(created.getId());
        userRepository.deleteById(user.getId());
    }

    @Test
    void testWorkloadDuplicationLifecycle() {
        User user = new User();
        user.setEmail("lifecycle_dup_" + System.currentTimeMillis() + "@example.com");
        user.setPasswordHash("hash");
        user.setRole("USER");
        user.setActive(true);
        user = userRepository.save(user);

        WorkloadRecord original = workloadService.createManualWorkload(
                user.getId(), "Source Workload", "Source Desc",
                List.of(new ProcessSpec("P1", 0, 10), new ProcessSpec("P2", 2, 4))
        );

        // Duplicate with custom name
        WorkloadRecord copy = workloadService.duplicateWorkload(original.getId(), user.getId(), "Cloned Workload");

        assertNotNull(copy);
        assertNotEquals(original.getId(), copy.getId());
        assertEquals("Cloned Workload", copy.getName());
        assertEquals("DUPLICATE", copy.getSourceType());
        assertEquals(2, copy.getWorkload().getProcesses().size());
        assertEquals("P1", copy.getWorkload().getProcesses().get(0).getProcessId());

        // Cleanup
        workloadRepository.deleteById(copy.getId());
        workloadRepository.deleteById(original.getId());
        userRepository.deleteById(user.getId());
    }

    @Test
    void testWorkloadFilteringAndPagination() {
        User user = new User();
        user.setEmail("lifecycle_page_" + System.currentTimeMillis() + "@example.com");
        user.setPasswordHash("hash");
        user.setRole("USER");
        user.setActive(true);
        user = userRepository.save(user);

        WorkloadRecord w1 = workloadService.createManualWorkload(user.getId(), "Alpha Batch", "Desc A", List.of(new ProcessSpec("P1", 0, 5)));
        WorkloadRecord w2 = workloadService.createManualWorkload(user.getId(), "Beta Batch", "Desc B", List.of(new ProcessSpec("P1", 0, 5)));
        WorkloadRecord w3 = workloadService.createManualWorkload(user.getId(), "Gamma System", "Desc G", List.of(new ProcessSpec("P1", 0, 5)));

        // Test search filter
        PagedResult<WorkloadRecord> searchBatch = workloadService.getWorkloadsPaged(user.getId(), "Batch", "name", "ASC", 1, 10);
        assertEquals(2, searchBatch.getTotalItems());
        assertEquals(2, searchBatch.getItems().size());
        assertEquals("Alpha Batch", searchBatch.getItems().get(0).getName());
        assertEquals("Beta Batch", searchBatch.getItems().get(1).getName());

        // Test pagination page size 1
        PagedResult<WorkloadRecord> page1 = workloadService.getWorkloadsPaged(user.getId(), null, "name", "ASC", 1, 1);
        assertEquals(3, page1.getTotalItems());
        assertEquals(3, page1.getTotalPages());
        assertEquals(1, page1.getItems().size());
        assertEquals("Alpha Batch", page1.getItems().get(0).getName());

        PagedResult<WorkloadRecord> page2 = workloadService.getWorkloadsPaged(user.getId(), null, "name", "ASC", 2, 1);
        assertEquals(1, page2.getItems().size());
        assertEquals("Beta Batch", page2.getItems().get(0).getName());

        // Cleanup
        workloadRepository.deleteById(w1.getId());
        workloadRepository.deleteById(w2.getId());
        workloadRepository.deleteById(w3.getId());
        userRepository.deleteById(user.getId());
    }
}
