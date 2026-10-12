package com.orchestrivo.web;

import com.orchestrivo.model.BenchmarkComparisonResult;
import com.orchestrivo.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.Optional;

@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = (User) req.getSession().getAttribute("user");

        // Section A: Workloads & Experiments
        if (ServiceRegistry.workloadService != null) {
            req.setAttribute("workloads", ServiceRegistry.workloadService.getWorkloadsByUser(user.getId()));
        }
        if (ServiceRegistry.experimentService != null) {
            req.setAttribute("experiments", ServiceRegistry.experimentService.getExperimentsByUser(user.getId()));
            req.setAttribute("schedulingSummaries", ServiceRegistry.experimentService.getAlgorithmExperimentSummary(user.getId()));
        }

        // Section A & C: Queue Metrics & Recent Jobs
        if (ServiceRegistry.executionJobService != null) {
            req.setAttribute("queueMetrics", ServiceRegistry.executionJobService.getQueueMetrics(user.getId()));
            req.setAttribute("recentJobs", ServiceRegistry.executionJobService.getRecentJobs(user.getId(), 8));
        }

        // Section A: Recent Activity
        if (ServiceRegistry.auditLogRepository != null) {
            req.setAttribute("recentActivity", ServiceRegistry.auditLogRepository.findByActorId(user.getId(), 6));
        }

        // Section B: Durable Featured Baseline/Candidate Comparison
        if (ServiceRegistry.benchmarkService != null) {
            Optional<BenchmarkComparisonResult> pinnedOpt = ServiceRegistry.benchmarkService.getFeaturedComparison();
            boolean isPinned = pinnedOpt.isPresent();
            Optional<BenchmarkComparisonResult> featuredOpt = isPinned
                    ? pinnedOpt
                    : ServiceRegistry.benchmarkService.getFeaturedOrFallbackComparison(user.getId());

            req.setAttribute("featuredComparison", featuredOpt.orElse(null));
            req.setAttribute("isFeaturedPinned", isPinned);

            // Section D: Benchmark Summaries
            req.setAttribute("benchmarkSummaries", ServiceRegistry.benchmarkService.getAlgorithmMetricsSummary(user.getId()));
            req.setAttribute("benchmarks", ServiceRegistry.benchmarkService.getBenchmarksPaged(user.getId(), 1, 5).getItems());
            if (ServiceRegistry.benchmarkRepository != null) {
                req.setAttribute("totalBenchmarkRuns", ServiceRegistry.benchmarkRepository.countByOwnerId(user.getId()));
            }
        }

        req.getRequestDispatcher("/WEB-INF/views/dashboard/index.jsp").forward(req, resp);
    }
}
