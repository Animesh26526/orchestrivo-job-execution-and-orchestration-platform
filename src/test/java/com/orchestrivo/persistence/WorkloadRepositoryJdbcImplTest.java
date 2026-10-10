package com.orchestrivo.persistence;

import com.orchestrivo.domain.ProcessSpec;
import com.orchestrivo.domain.Workload;
import com.orchestrivo.model.User;
import com.orchestrivo.model.WorkloadRecord;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class WorkloadRepositoryJdbcImplTest {

    private static WorkloadRepository repository;
    private static UserRepository userRepository;

    @BeforeAll
    static void setUp() {
        try (Connection conn = DataSourceManager.getDataSource().getConnection()) {
            Assumptions.assumeTrue(conn.isValid(2), "Database connection is not available");
            repository = new WorkloadRepositoryJdbcImpl();
            userRepository = new UserRepositoryJdbcImpl();
        } catch (Exception e) {
            Assumptions.abort("Database connection failed, skipping tests");
        }
    }

    @Test
    void testSaveAndFindById() {
        // Create user first due to FK
        User user = new User();
        user.setEmail("workload_test" + System.currentTimeMillis() + "@example.com");
        user.setPasswordHash("hash");
        user.setRole("USER");
        user.setActive(true);
        user = userRepository.save(user);

        WorkloadRecord record = new WorkloadRecord();
        record.setOwnerUserId(user.getId());
        record.setName("Test Workload");
        record.setSourceType("MANUAL");

        ProcessSpec p1 = new ProcessSpec("P1", 0, 10);
        ProcessSpec p2 = new ProcessSpec("P2", 2, 5);
        record.setWorkload(new Workload(Arrays.asList(p1, p2)));

        WorkloadRecord saved = repository.save(record);
        assertTrue(saved.getId() > 0);

        Optional<WorkloadRecord> found = repository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Test Workload", found.get().getName());
        assertNotNull(found.get().getWorkload());
        assertEquals(2, found.get().getWorkload().getProcesses().size());
        
        repository.deleteById(saved.getId());
        userRepository.deleteById(user.getId());
    }
}
