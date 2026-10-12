package com.orchestrivo.service;

import com.orchestrivo.model.BenchmarkComparisonResult;
import com.orchestrivo.model.QueueMetrics;
import com.orchestrivo.persistence.*;
import com.orchestrivo.web.ServiceRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class DemoDataSeederTest {

    @BeforeAll
    static void setUpAll() {
        try (Connection conn = DataSourceManager.getDataSource().getConnection()) {
            Assumptions.assumeTrue(conn.isValid(2), "Database connection is not available");
            UserRepository ur = new UserRepositoryJdbcImpl();
            WorkloadRepository wr = new WorkloadRepositoryJdbcImpl();
            ExperimentRunRepository er = new ExperimentRunRepositoryJdbcImpl();
            BenchmarkRepository br = new BenchmarkRepositoryJdbcImpl();
            AuditLogRepository ar = new AuditLogRepositoryJdbcImpl();
            ExecutionJobRepository jr = new ExecutionJobRepositoryJdbcImpl();
            SystemSettingsRepository sr = new SystemSettingsRepositoryJdbcImpl();

            ServiceRegistry.initialize(ur, wr, er, br, ar, jr, sr);
        } catch (Exception e) {
            Assumptions.abort("Database not reachable: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("DemoDataSeeder populates real workloads, ~40 jobs through genuine pipeline, benchmarks, and pins comparison")
    void testDemoDataSeederExecution() {
        DemoDataSeeder.DemoSeedResult result = DemoDataSeeder.seed(true);
        assertEquals("SUCCESS", result.status, "Seeder must complete with SUCCESS status");
        assertTrue(result.demoUserId > 0, "Demo user ID must be valid");
        assertEquals(5, result.workloadsCreated, "Must create exactly 5 distinct realistic workloads");
        assertTrue(result.jobsCompleted >= 25, "Must complete at least 25 execution jobs through the worker pipeline");
        assertTrue(result.jobsFailed >= 3, "Must produce failed jobs modeling validation/execution errors");
        assertTrue(result.jobsCancelled >= 2, "Must produce cancelled jobs");
        assertTrue(result.jobsQueued >= 2, "Must leave queued jobs for live queue state");
        assertTrue(result.benchmarksRun >= 4, "Must execute at least 4 micro-benchmarks");
        assertTrue(result.featuredBaselineId > 0, "Must set canonical baseline run");
        assertTrue(result.featuredCandidateId > 0, "Must set canonical candidate run");

        // Verify durable pinned comparison
        Optional<BenchmarkComparisonResult> featuredOpt = ServiceRegistry.benchmarkService.getFeaturedComparison();
        assertTrue(featuredOpt.isPresent(), "Featured comparison must be durably pinned in system_settings");
        BenchmarkComparisonResult comparison = featuredOpt.get();
        assertEquals(result.featuredBaselineId, comparison.getBaseline().getId());
        assertEquals(result.featuredCandidateId, comparison.getCandidate().getId());
        assertTrue(comparison.getSpeedupFactor() > 0, "Speedup factor must be computed");
        assertTrue(comparison.getBaselineActualBatchThroughput() > 0, "Baseline actual throughput must be positive");
        assertTrue(comparison.getCandidateActualBatchThroughput() > 0, "Candidate actual throughput must be positive");

        // Verify queue metrics reflect all statuses
        QueueMetrics qMetrics = ServiceRegistry.executionJobService.getQueueMetrics(result.demoUserId);
        assertNotNull(qMetrics);
        assertTrue(qMetrics.getCompletedCount() >= 25);
        assertTrue(qMetrics.getFailedCount() >= 3);
        assertTrue(qMetrics.getCancelledCount() >= 2);
        assertTrue((qMetrics.getQueuedCount() + qMetrics.getCompletedCount()) >= 28,
                "Queued jobs must either remain queued or have been completed by active background engine");
    }

    @AfterAll
    static void cleanUpDemoJobs() {
        try (Connection conn = DataSourceManager.getDataSource().getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement("DELETE FROM execution_jobs")) {
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }
}
