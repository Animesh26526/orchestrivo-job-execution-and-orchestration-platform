package com.orchestrivo.service;

import com.orchestrivo.model.BenchmarkComparisonResult;
import com.orchestrivo.model.BenchmarkMeasurement;
import com.orchestrivo.model.BenchmarkRunRecord;
import com.orchestrivo.persistence.BenchmarkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BenchmarkBeforeAfterComparisonTest {

    private BenchmarkRepository benchmarkRepository;
    private BenchmarkService benchmarkService;

    @BeforeEach
    void setUp() {
        benchmarkRepository = mock(BenchmarkRepository.class);
        benchmarkService = new BenchmarkService(benchmarkRepository);
    }

    private BenchmarkRunRecord createRun(long id, long userId, String algo, int procs, long seed,
                                         String runType, double medianNanos, double stdDevNanos,
                                         List<Long> durations) {
        BenchmarkRunRecord r = new BenchmarkRunRecord();
        r.setId(id);
        r.setOwnerUserId(userId);
        r.setAlgorithmName(algo);
        r.setProcessCount(procs);
        r.setSeed(seed);
        r.setWarmupIterations(5);
        r.setMeasuredIterations(durations.size());
        r.setMedianNanos(medianNanos);
        r.setStdDevNanos(stdDevNanos);
        r.setRunType(runType);
        r.setValidationPassed(true);
        r.setCreatedAt(LocalDateTime.now());

        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        for (int i = 0; i < durations.size(); i++) {
            long d = durations.get(i);
            if (d < min) min = d;
            if (d > max) max = d;
            r.getMeasurements().add(new BenchmarkMeasurement(i + 1, d));
        }
        r.setMinNanos(min == Long.MAX_VALUE ? (long) medianNanos : min);
        r.setMaxNanos(max == Long.MIN_VALUE ? (long) medianNanos : max);
        return r;
    }

    @Test
    @DisplayName("Controlled matched runs calculate correct speedup, latency reduction, and throughput delta")
    void testControlledMatchWithImprovement() {
        // Baseline: 200 µs (200,000 ns) median
        BenchmarkRunRecord baseline = createRun(1L, 10L, "FCFS", 50, 42L, "BASELINE",
                200_000.0, 15_000.0, List.of(190_000L, 200_000L, 210_000L));

        // Candidate: 160 µs (160,000 ns) median -> 1.25x speedup (-20% latency)
        BenchmarkRunRecord candidate = createRun(2L, 10L, "FCFS", 50, 42L, "CANDIDATE",
                160_000.0, 10_000.0, List.of(150_000L, 160_000L, 170_000L));

        when(benchmarkRepository.findById(1L)).thenReturn(Optional.of(baseline));
        when(benchmarkRepository.findById(2L)).thenReturn(Optional.of(candidate));

        BenchmarkComparisonResult result = benchmarkService.compareRuns(1L, 2L, 10L);

        assertTrue(result.isWorkloadMatched(), "Identical algorithm, process count, and seed must be matched");
        assertTrue(result.getMatchNotes().contains("Controlled Workload Matched"));
        assertTrue(result.isImprovement(), "Speedup 1.25x must be classified as improvement");
        assertFalse(result.isRegression());
        assertFalse(result.isParity());

        assertEquals(1.25, result.getSpeedupFactor(), 0.001);
        assertEquals("1.25x", result.getFormattedSpeedup());

        assertEquals(-40_000.0, result.getDeltaMedianNanos(), 0.001);
        assertEquals(-20.0, result.getDeltaMedianPct(), 0.001);
        assertEquals(-20.0, result.getDeltaMeanPct(), 0.001);
        assertEquals(-33.33, result.getDeltaStdDevPct(), 0.1);

        // Throughput comparison: 1e9 / 200_000 = 5000 runs/sec vs 1e9 / 160_000 = 6250 runs/sec (+25%)
        assertEquals(5000.0, result.getBaselineThroughput(), 0.1);
        assertEquals(6250.0, result.getCandidateThroughput(), 0.1);
        assertEquals(25.0, result.getDeltaThroughputPct(), 0.1);
    }

    @Test
    @DisplayName("Workload variation across algorithms or process counts is explicitly flagged")
    void testWorkloadMismatchFlagged() {
        BenchmarkRunRecord baseline = createRun(1L, 10L, "FCFS", 50, 42L, "BASELINE",
                200_000.0, 10_000.0, List.of(200_000L));
        BenchmarkRunRecord candidate = createRun(2L, 10L, "SRTF", 50, 42L, "CANDIDATE",
                150_000.0, 8_000.0, List.of(150_000L));

        BenchmarkComparisonResult result = benchmarkService.compareRuns(baseline, candidate);

        assertFalse(result.isWorkloadMatched());
        assertTrue(result.getMatchNotes().contains("Algorithm: FCFS vs SRTF"));
    }

    @Test
    @DisplayName("Zero-division protection prevents NaN and Infinite values")
    void testZeroDivisionProtection() {
        BenchmarkRunRecord zeroBase = createRun(1L, 10L, "FCFS", 50, 42L, "BASELINE",
                0.0, 0.0, List.of(0L));
        BenchmarkRunRecord zeroCand = createRun(2L, 10L, "FCFS", 50, 42L, "CANDIDATE",
                0.0, 0.0, List.of(0L));

        BenchmarkComparisonResult result = benchmarkService.compareRuns(zeroBase, zeroCand);

        assertFalse(Double.isNaN(result.getSpeedupFactor()));
        assertFalse(Double.isInfinite(result.getSpeedupFactor()));
        assertEquals(1.0, result.getSpeedupFactor(), 0.001);
        assertEquals(0.0, result.getDeltaMedianPct(), 0.001);
        assertEquals(0.0, result.getDeltaThroughputPct(), 0.001);
    }

    @Test
    @DisplayName("Parity is correctly identified when within +-0.5% threshold")
    void testParityWithinThreshold() {
        BenchmarkRunRecord baseline = createRun(1L, 10L, "SJF", 100, 777L, "BASELINE",
                100_000.0, 5_000.0, List.of(100_000L));
        BenchmarkRunRecord candidate = createRun(2L, 10L, "SJF", 100, 777L, "CANDIDATE",
                100_200.0, 5_000.0, List.of(100_200L)); // +0.2% change

        BenchmarkComparisonResult result = benchmarkService.compareRuns(baseline, candidate);

        assertTrue(result.isParity());
        assertFalse(result.isImprovement());
        assertFalse(result.isRegression());
    }

    @Test
    @DisplayName("Unauthorized run access or missing run throws IllegalArgumentException")
    void testUnauthorizedRunComparison() {
        BenchmarkRunRecord run1 = createRun(1L, 999L, "FCFS", 50, 42L, "BASELINE", 100.0, 5.0, List.of(100L));
        when(benchmarkRepository.findById(1L)).thenReturn(Optional.of(run1));

        // Attempting to access run belonging to user 999 as user 10
        assertThrows(IllegalArgumentException.class, () -> benchmarkService.compareRuns(1L, 2L, 10L));
    }
}
