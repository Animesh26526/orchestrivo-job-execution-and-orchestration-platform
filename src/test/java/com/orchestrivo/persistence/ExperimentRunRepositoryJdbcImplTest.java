package com.orchestrivo.persistence;

import com.orchestrivo.domain.AggregateMetrics;
import com.orchestrivo.domain.ExecutionSegment;
import com.orchestrivo.domain.ProcessMetrics;
import com.orchestrivo.domain.ScheduleResult;
import com.orchestrivo.model.ExperimentRunRecord;
import com.orchestrivo.model.User;
import com.orchestrivo.model.WorkloadRecord;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ExperimentRunRepositoryJdbcImplTest {

    private static ExperimentRunRepository repository;
    private static WorkloadRepository workloadRepository;
    private static UserRepository userRepository;

    @BeforeAll
    static void setUp() {
        try (Connection conn = DataSourceManager.getDataSource().getConnection()) {
            Assumptions.assumeTrue(conn.isValid(2), "Database connection is not available");
            repository = new ExperimentRunRepositoryJdbcImpl();
            workloadRepository = new WorkloadRepositoryJdbcImpl();
            userRepository = new UserRepositoryJdbcImpl();
        } catch (Exception e) {
            Assumptions.abort("Database connection failed, skipping tests");
        }
    }

    @Test
    void testSaveAndFindById() {
        User user = new User();
        user.setEmail("experiment_test" + System.currentTimeMillis() + "@example.com");
        user.setPasswordHash("hash");
        user.setRole("USER");
        user.setActive(true);
        user = userRepository.save(user);

        WorkloadRecord wl = new WorkloadRecord();
        wl.setOwnerUserId(user.getId());
        wl.setName("WL1");
        wl.setSourceType("MANUAL");
        wl = workloadRepository.save(wl);

        ExperimentRunRecord run = new ExperimentRunRecord();
        run.setOwnerUserId(user.getId());
        run.setWorkloadId(wl.getId());
        run.setAlgorithmName("FCFS");
        run.setRunStatus("COMPLETED");
        
        AggregateMetrics am = new AggregateMetrics(1.0, 2.0, 3.0, 10, 8, 2, 0.5, 5);
        ProcessMetrics pm = new ProcessMetrics("P1", 0, 5, 5, 0, 0);
        ExecutionSegment seg = new ExecutionSegment("P1", 0, 5);
        
        ScheduleResult result = new ScheduleResult(
            Collections.singletonList(seg),
            Collections.singletonList(pm),
            am,
            "FCFS"
        );
        run.setResult(result);

        ExperimentRunRecord saved = repository.save(run);
        assertTrue(saved.getId() > 0);

        Optional<ExperimentRunRecord> found = repository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("FCFS", found.get().getAlgorithmName());
        
        ScheduleResult loadedResult = found.get().getResult();
        assertNotNull(loadedResult);
        assertNotNull(loadedResult.getAggregateMetrics());
        assertEquals(1, loadedResult.getExecutionTimeline().size());
        assertEquals(1, loadedResult.getProcessMetrics().size());
        
        repository.deleteById(saved.getId());
        workloadRepository.deleteById(wl.getId());
        userRepository.deleteById(user.getId());
    }
}
