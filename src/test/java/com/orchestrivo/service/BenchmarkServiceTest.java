package com.orchestrivo.service;

import com.orchestrivo.model.BenchmarkRunRecord;
import com.orchestrivo.persistence.BenchmarkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class BenchmarkServiceTest {

    private BenchmarkRepository benchmarkRepository;
    private BenchmarkService benchmarkService;

    @BeforeEach
    void setUp() {
        benchmarkRepository = mock(BenchmarkRepository.class);
        benchmarkService = new BenchmarkService(benchmarkRepository);
    }

    @Test
    void testComputeMedianOddCount() {
        List<Long> values = new ArrayList<>(Arrays.asList(100L, 200L, 300L, 400L, 500L));
        double median = BenchmarkService.computeMedian(values);
        assertEquals(300.0, median, 0.001);
    }

    @Test
    void testComputeMedianEvenCount() {
        List<Long> values = new ArrayList<>(Arrays.asList(100L, 200L, 300L, 400L));
        double median = BenchmarkService.computeMedian(values);
        assertEquals(250.0, median, 0.001);
    }

    @Test
    void testComputeStdDev() {
        // Values with sample variance: [10, 20, 30]
        // Mean = 20, Diff sq = 100 + 0 + 100 = 200
        // Variance = 200 / (3 - 1) = 100
        // Std dev = 10
        List<Long> values = Arrays.asList(10L, 20L, 30L);
        double stdDev = BenchmarkService.computeStdDev(values);
        assertEquals(10.0, stdDev, 0.001);
    }

    @Test
    void testRunBenchmarkSuccess() {
        BenchmarkService.BenchmarkConfig config = new BenchmarkService.BenchmarkConfig();
        config.setAlgorithmName("FCFS");
        config.setProcessCount(10);
        config.setWarmupIterations(2);
        config.setMeasuredIterations(5);
        config.setSeed(999L);

        when(benchmarkRepository.save(any(BenchmarkRunRecord.class))).thenAnswer(invocation -> {
            BenchmarkRunRecord record = invocation.getArgument(0);
            record.setId(101L);
            return record;
        });

        BenchmarkRunRecord run = benchmarkService.runBenchmark(1L, config);

        assertNotNull(run);
        assertEquals(101L, run.getId());
        assertEquals("FCFS", run.getAlgorithmName());
        assertEquals(10, run.getProcessCount());
        assertEquals(5, run.getMeasurements().size());
        assertTrue(run.getMedianNanos() > 0);
        assertTrue(run.getMinNanos() > 0);
        assertTrue(run.getMaxNanos() >= run.getMinNanos());
        assertTrue(run.isValidationPassed(), "Simulation schedules should pass validation");
        assertNotNull(run.getEnvironmentInfo());
        verify(benchmarkRepository).save(any(BenchmarkRunRecord.class));
    }

    @Test
    void testValidateConfigRejections() {
        // Null config
        assertThrows(IllegalArgumentException.class, () -> benchmarkService.validateConfig(null));

        // Missing algorithm
        BenchmarkService.BenchmarkConfig c1 = new BenchmarkService.BenchmarkConfig();
        c1.setAlgorithmName("");
        assertThrows(IllegalArgumentException.class, () -> benchmarkService.validateConfig(c1));

        // Invalid algorithm
        c1.setAlgorithmName("UNKNOWN_ALGO");
        assertThrows(IllegalArgumentException.class, () -> benchmarkService.validateConfig(c1));

        // Process count out of bounds
        BenchmarkService.BenchmarkConfig c2 = new BenchmarkService.BenchmarkConfig();
        c2.setAlgorithmName("FCFS");
        c2.setProcessCount(1); // Below min of 5
        assertThrows(IllegalArgumentException.class, () -> benchmarkService.validateConfig(c2));

        c2.setProcessCount(501); // Above max 500
        assertThrows(IllegalArgumentException.class, () -> benchmarkService.validateConfig(c2));

        // Warmup iterations out of bounds
        BenchmarkService.BenchmarkConfig c3 = new BenchmarkService.BenchmarkConfig();
        c3.setAlgorithmName("FCFS");
        c3.setWarmupIterations(0);
        assertThrows(IllegalArgumentException.class, () -> benchmarkService.validateConfig(c3));

        // Repetitions out of bounds
        BenchmarkService.BenchmarkConfig c4 = new BenchmarkService.BenchmarkConfig();
        c4.setAlgorithmName("FCFS");
        c4.setMeasuredIterations(2); // below min 3
        assertThrows(IllegalArgumentException.class, () -> benchmarkService.validateConfig(c4));
    }
}
