package com.orchestrivo.service;

import com.orchestrivo.model.BenchmarkComparisonResult;
import com.orchestrivo.model.BenchmarkRunRecord;
import com.orchestrivo.persistence.BenchmarkRepository;
import com.orchestrivo.persistence.BenchmarkRepositoryJdbcImpl;
import com.orchestrivo.persistence.DataSourceManager;
import com.orchestrivo.persistence.SystemSettingsRepository;
import com.orchestrivo.persistence.SystemSettingsRepositoryJdbcImpl;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ActualBatchThroughputAndSettingsTest {

    private static BenchmarkRepository benchmarkRepo;
    private static SystemSettingsRepository settingsRepo;
    private static BenchmarkService benchmarkService;

    @BeforeAll
    static void setUpAll() {
        try (Connection conn = DataSourceManager.getDataSource().getConnection()) {
            Assumptions.assumeTrue(conn.isValid(2), "Database connection is not available");
            benchmarkRepo = new BenchmarkRepositoryJdbcImpl();
            settingsRepo = new SystemSettingsRepositoryJdbcImpl();
            benchmarkService = new BenchmarkService(benchmarkRepo, settingsRepo);
        } catch (Exception e) {
            Assumptions.abort("Database not reachable: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Actual batch throughput measures wall-clock execution independently of median latency reciprocal")
    void testActualBatchThroughputCalculation() {
        BenchmarkRunRecord run = new BenchmarkRunRecord();
        run.setValidRunCount(100);
        run.setMeasuredIterations(100);
        // 200 milliseconds batch elapsed = 0.2 seconds -> 100 / 0.2 = 500.0 runs/sec
        run.setBatchElapsedNanos(200_000_000L);
        // Median latency = 1 millisecond = 0.001 seconds -> 1 / 0.001 = 1000.0 runs/sec estimate
        run.setMedianNanos(1_000_000.0);

        assertEquals(500.0, run.getActualBatchThroughput(), 0.01,
                "Actual batch throughput must equal completed runs divided by elapsed seconds");
        assertEquals(1000.0, run.getLatencyDerivedThroughputEstimate(), 0.01,
                "Latency derived throughput estimate must equal reciprocal of median latency");
        assertNotEquals(run.getActualBatchThroughput(), run.getLatencyDerivedThroughputEstimate(),
                "Actual batch throughput must NOT be conflated with the latency estimate");
    }

    @Test
    @DisplayName("Controlled match strictly enforces identical algorithms, process counts, seeds, and quantum")
    void testControlledMatchVerification() {
        BenchmarkRunRecord base = new BenchmarkRunRecord();
        base.setAlgorithmName("ROUND_ROBIN");
        base.setProcessCount(50);
        base.setSeed(42L);
        base.setQuantum(3);
        base.setWarmupIterations(5);
        base.setMeasuredIterations(15);
        base.setMedianNanos(50_000.0);
        base.setBatchElapsedNanos(10_000_000L);
        base.setValidRunCount(15);

        BenchmarkRunRecord candSame = new BenchmarkRunRecord();
        candSame.setAlgorithmName("ROUND_ROBIN");
        candSame.setProcessCount(50);
        candSame.setSeed(42L);
        candSame.setQuantum(3);
        candSame.setWarmupIterations(5);
        candSame.setMeasuredIterations(15);
        candSame.setMedianNanos(40_000.0);
        candSame.setBatchElapsedNanos(8_000_000L);
        candSame.setValidRunCount(15);

        BenchmarkComparisonResult matchResult = new BenchmarkComparisonResult(base, candSame);
        assertTrue(matchResult.isControlledMatch(), "Identical algorithm, quantum, processes, seed must be controlled match");

        BenchmarkRunRecord candDiffQuantum = new BenchmarkRunRecord();
        candDiffQuantum.setAlgorithmName("ROUND_ROBIN");
        candDiffQuantum.setProcessCount(50);
        candDiffQuantum.setSeed(42L);
        candDiffQuantum.setQuantum(6); // Quantum differs
        candDiffQuantum.setWarmupIterations(5);
        candDiffQuantum.setMeasuredIterations(15);
        candDiffQuantum.setMedianNanos(35_000.0);
        candDiffQuantum.setBatchElapsedNanos(7_000_000L);
        candDiffQuantum.setValidRunCount(15);

        BenchmarkComparisonResult variantResult = new BenchmarkComparisonResult(base, candDiffQuantum);
        assertFalse(variantResult.isControlledMatch(), "Differing quantum must NOT be labeled an identical match");
        assertTrue(variantResult.getMatchNotes().contains("Quantum"), "Match notes must detail quantum variance");
    }

    @Test
    @DisplayName("Durable system settings repository survives get, set, and removal")
    void testDurableSystemSettings() {
        String testKey = "test_featured_baseline_" + System.currentTimeMillis();
        long testValue = 987654L;

        settingsRepo.setLongSetting(testKey, testValue);
        Optional<Long> retrieved = settingsRepo.getLongSetting(testKey);
        assertTrue(retrieved.isPresent());
        assertEquals(testValue, retrieved.get().longValue());

        settingsRepo.removeSetting(testKey);
        Optional<Long> afterRemoval = settingsRepo.getLongSetting(testKey);
        assertTrue(afterRemoval.isEmpty());
    }

    @Test
    @DisplayName("Featured benchmark pinning and unpinning in BenchmarkService")
    void testFeaturedComparisonPinning() {
        long baseId = 1001L;
        long candId = 1002L;

        benchmarkService.setFeaturedComparison(baseId, candId);
        assertTrue(benchmarkService.isFeaturedPair(baseId, candId));
        assertEquals(baseId, benchmarkService.getFeaturedBaselineId().orElse(-1L).longValue());
        assertEquals(candId, benchmarkService.getFeaturedCandidateId().orElse(-1L).longValue());

        benchmarkService.clearFeaturedComparison();
        assertFalse(benchmarkService.isFeaturedPair(baseId, candId));
        assertTrue(benchmarkService.getFeaturedBaselineId().isEmpty());
    }
}
