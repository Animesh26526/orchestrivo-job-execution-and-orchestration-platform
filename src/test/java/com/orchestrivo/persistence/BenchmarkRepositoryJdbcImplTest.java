package com.orchestrivo.persistence;

import com.orchestrivo.model.BenchmarkMeasurement;
import com.orchestrivo.model.BenchmarkRunRecord;
import com.orchestrivo.model.User;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class BenchmarkRepositoryJdbcImplTest {

    private static BenchmarkRepository benchmarkRepository;
    private static UserRepository userRepository;

    @BeforeAll
    static void setUp() {
        try (Connection conn = DataSourceManager.getDataSource().getConnection()) {
            Assumptions.assumeTrue(conn.isValid(2), "Database connection is not available");
            benchmarkRepository = new BenchmarkRepositoryJdbcImpl();
            userRepository = new UserRepositoryJdbcImpl();
        } catch (Exception e) {
            Assumptions.abort("Database connection failed, skipping tests");
        }
    }

    @Test
    void testSaveFindAndCascadeDeleteBenchmark() {
        User user = new User();
        user.setEmail("bench_test_" + System.currentTimeMillis() + "@example.com");
        user.setPasswordHash("hash");
        user.setRole("USER");
        user.setActive(true);
        user = userRepository.save(user);

        BenchmarkRunRecord run = new BenchmarkRunRecord();
        run.setOwnerUserId(user.getId());
        run.setAlgorithmName("FCFS");
        run.setProcessCount(25);
        run.setSeed(42L);
        run.setWarmupIterations(5);
        run.setMeasuredIterations(3);
        run.setMedianNanos(500000.0);
        run.setMinNanos(450000L);
        run.setMaxNanos(550000L);
        run.setStdDevNanos(50000.0);
        run.setValidationPassed(true);
        run.setEnvironmentInfo("{\"javaVersion\":\"17\"}");

        BenchmarkMeasurement m1 = new BenchmarkMeasurement(1, 450000L);
        BenchmarkMeasurement m2 = new BenchmarkMeasurement(2, 500000L);
        BenchmarkMeasurement m3 = new BenchmarkMeasurement(3, 550000L);
        run.setMeasurements(Arrays.asList(m1, m2, m3));

        BenchmarkRunRecord saved = benchmarkRepository.save(run);
        assertTrue(saved.getId() > 0);

        Optional<BenchmarkRunRecord> found = benchmarkRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        BenchmarkRunRecord r = found.get();
        assertEquals("FCFS", r.getAlgorithmName());
        assertEquals(25, r.getProcessCount());
        assertEquals(42L, r.getSeed());
        assertTrue(r.isValidationPassed());
        assertEquals(3, r.getMeasurements().size());
        assertEquals(1, r.getMeasurements().get(0).getIterationNumber());

        int count = benchmarkRepository.countByOwnerId(user.getId());
        assertEquals(1, count);

        List<BenchmarkRunRecord> list = benchmarkRepository.findWithFilter(user.getId(), 0, 10);
        assertEquals(1, list.size());
        assertEquals(saved.getId(), list.get(0).getId());

        // Delete benchmark
        benchmarkRepository.deleteById(saved.getId());
        Optional<BenchmarkRunRecord> deleted = benchmarkRepository.findById(saved.getId());
        assertFalse(deleted.isPresent());

        userRepository.deleteById(user.getId());
    }
}
