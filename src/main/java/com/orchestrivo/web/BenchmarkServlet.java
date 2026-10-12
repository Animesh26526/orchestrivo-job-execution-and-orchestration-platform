package com.orchestrivo.web;

import com.orchestrivo.model.BenchmarkRunRecord;
import com.orchestrivo.model.PagedResult;
import com.orchestrivo.model.User;
import com.orchestrivo.service.BenchmarkService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@WebServlet({"/benchmarks", "/benchmarks/run", "/benchmarks/view", "/benchmarks/compare", "/benchmarks/delete", "/benchmarks/feature", "/benchmarks/guide"})
public class BenchmarkServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        User user = (User) req.getSession().getAttribute("user");

        if ("/benchmarks/guide".equals(path)) {
            req.getRequestDispatcher("/WEB-INF/views/benchmarks/guide.jsp").forward(req, resp);
        } else if ("/benchmarks/run".equals(path)) {
            req.setAttribute("baselines", ServiceRegistry.benchmarkService.getBaselineRuns(user.getId()));
            req.getRequestDispatcher("/WEB-INF/views/benchmarks/new.jsp").forward(req, resp);
        } else if ("/benchmarks/view".equals(path)) {
            handleViewGet(req, resp, user);
        } else if ("/benchmarks/compare".equals(path)) {
            handleCompareGet(req, resp, user);
        } else {
            handleListGet(req, resp, user);
        }
    }

    private void handleListGet(HttpServletRequest req, HttpServletResponse resp, User user) throws ServletException, IOException {
        int page = 1;
        int pageSize = 10;
        try {
            String p = req.getParameter("page");
            if (p != null && !p.trim().isEmpty()) page = Integer.parseInt(p.trim());
        } catch (NumberFormatException ignored) {}

        try {
            String ps = req.getParameter("pageSize");
            if (ps != null && !ps.trim().isEmpty()) pageSize = Integer.parseInt(ps.trim());
        } catch (NumberFormatException ignored) {}

        PagedResult<BenchmarkRunRecord> pagedResult = ServiceRegistry.benchmarkService.getBenchmarksPaged(user.getId(), page, pageSize);
        req.setAttribute("pagedResult", pagedResult);
        req.setAttribute("benchmarks", pagedResult.getItems());
        req.getRequestDispatcher("/WEB-INF/views/benchmarks/list.jsp").forward(req, resp);
    }

    private void handleViewGet(HttpServletRequest req, HttpServletResponse resp, User user) throws ServletException, IOException {
        try {
            long id = Long.parseLong(req.getParameter("id"));
            Optional<BenchmarkRunRecord> run = ServiceRegistry.benchmarkService.getBenchmarkById(id, user.getId());
            if (run.isPresent()) {
                req.setAttribute("benchmark", run.get());
                req.getRequestDispatcher("/WEB-INF/views/benchmarks/view.jsp").forward(req, resp);
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Benchmark not found");
            }
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid benchmark ID");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        User user = (User) req.getSession().getAttribute("user");

        if ("/benchmarks/run".equals(path)) {
            handleRunPost(req, resp, user);
        } else if ("/benchmarks/delete".equals(path)) {
            handleDeletePost(req, resp, user);
        } else if ("/benchmarks/feature".equals(path)) {
            handleFeaturePost(req, resp, user);
        }
    }

    private void handleRunPost(HttpServletRequest req, HttpServletResponse resp, User user) throws ServletException, IOException {
        BenchmarkService.BenchmarkConfig config = new BenchmarkService.BenchmarkConfig();
        try {
            config.setAlgorithmName(req.getParameter("algorithmName"));

            String q = req.getParameter("quantum");
            if (q != null && !q.trim().isEmpty()) {
                try {
                    config.setQuantum(Integer.parseInt(q.trim()));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Time quantum must be a valid integer between 1 and 100.");
                }
            }

            String pc = req.getParameter("processCount");
            if (pc == null || pc.trim().isEmpty()) {
                throw new IllegalArgumentException("Process count is required (between 1 and 500).");
            }
            try {
                config.setProcessCount(Integer.parseInt(pc.trim()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Process count must be a valid integer between 1 and 500.");
            }

            String minA = req.getParameter("minArrival");
            if (minA == null || minA.trim().isEmpty()) {
                throw new IllegalArgumentException("Minimum arrival time is required.");
            }
            try {
                config.setMinArrival(Integer.parseInt(minA.trim()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Minimum arrival time must be a valid non-negative integer.");
            }

            String maxA = req.getParameter("maxArrival");
            if (maxA == null || maxA.trim().isEmpty()) {
                throw new IllegalArgumentException("Maximum arrival time is required.");
            }
            try {
                config.setMaxArrival(Integer.parseInt(maxA.trim()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Maximum arrival time must be a valid non-negative integer.");
            }

            String minB = req.getParameter("minBurst");
            if (minB == null || minB.trim().isEmpty()) {
                throw new IllegalArgumentException("Minimum burst time is required.");
            }
            try {
                config.setMinBurst(Integer.parseInt(minB.trim()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Minimum burst time must be a valid positive integer.");
            }

            String maxB = req.getParameter("maxBurst");
            if (maxB == null || maxB.trim().isEmpty()) {
                throw new IllegalArgumentException("Maximum burst time is required.");
            }
            try {
                config.setMaxBurst(Integer.parseInt(maxB.trim()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Maximum burst time must be a valid positive integer.");
            }

            String s = req.getParameter("seed");
            if (s != null && !s.trim().isEmpty()) {
                try {
                    config.setSeed(Long.parseLong(s.trim()));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Reproducibility seed must be a valid integer.");
                }
            }

            String w = req.getParameter("warmupIterations");
            if (w == null || w.trim().isEmpty()) {
                throw new IllegalArgumentException("Warm-up iterations is required (between 1 and 100).");
            }
            try {
                config.setWarmupIterations(Integer.parseInt(w.trim()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Warm-up iterations must be a valid integer between 1 and 100.");
            }

            String m = req.getParameter("measuredIterations");
            if (m == null || m.trim().isEmpty()) {
                throw new IllegalArgumentException("Measured iterations is required (between 1 and 1000).");
            }
            try {
                config.setMeasuredIterations(Integer.parseInt(m.trim()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Measured iterations must be a valid integer between 1 and 1000.");
            }

            String rt = req.getParameter("runType");
            if (rt != null && !rt.trim().isEmpty()) {
                config.setRunType(rt.trim().toUpperCase());
            }

            String bg = req.getParameter("benchmarkGroup");
            if (bg != null && !bg.trim().isEmpty()) {
                config.setBenchmarkGroup(bg.trim());
            }

            BenchmarkRunRecord run = ServiceRegistry.benchmarkService.runBenchmark(user.getId(), config);
            if (ServiceRegistry.auditService != null) {
                ServiceRegistry.auditService.log(user.getId(), "BENCHMARK_RUN", "BENCHMARK_RUN", String.valueOf(run.getId()),
                        Map.of("algorithm", run.getAlgorithmName(), "processCount", run.getProcessCount(), "medianMicros", run.getMedianMicros(), "runType", run.getRunType()));
            }
            resp.sendRedirect(req.getContextPath() + "/benchmarks/view?id=" + run.getId());
        } catch (IllegalArgumentException e) {
            req.setAttribute("error", e.getMessage());
            req.setAttribute("config", config);
            req.setAttribute("baselines", ServiceRegistry.benchmarkService.getBaselineRuns(user.getId()));
            req.getRequestDispatcher("/WEB-INF/views/benchmarks/new.jsp").forward(req, resp);
        } catch (Exception e) {
            req.setAttribute("error", "An error occurred while executing the benchmark: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
            req.setAttribute("config", config);
            req.setAttribute("baselines", ServiceRegistry.benchmarkService.getBaselineRuns(user.getId()));
            req.getRequestDispatcher("/WEB-INF/views/benchmarks/new.jsp").forward(req, resp);
        }
    }

    private void handleCompareGet(HttpServletRequest req, HttpServletResponse resp, User user) throws ServletException, IOException {
        String baseParam = req.getParameter("baselineId");
        String candParam = req.getParameter("candidateId");

        if (baseParam == null || candParam == null || baseParam.trim().isEmpty() || candParam.trim().isEmpty()) {
            java.util.Optional<com.orchestrivo.model.BenchmarkComparisonResult> featuredOpt = ServiceRegistry.benchmarkService.getFeaturedComparison();
            if (featuredOpt.isPresent()) {
                com.orchestrivo.model.BenchmarkComparisonResult featured = featuredOpt.get();
                baseParam = String.valueOf(featured.getBaseline().getId());
                candParam = String.valueOf(featured.getCandidate().getId());
            } else {
                PagedResult<BenchmarkRunRecord> all = ServiceRegistry.benchmarkService.getBenchmarksPaged(user.getId(), 1, 100);
                req.setAttribute("benchmarks", all.getItems());
                req.setAttribute("baselines", ServiceRegistry.benchmarkService.getBaselineRuns(user.getId()));
                req.getRequestDispatcher("/WEB-INF/views/benchmarks/compare.jsp").forward(req, resp);
                return;
            }
        }

        try {
            long baselineId = Long.parseLong(baseParam.trim());
            long candidateId = Long.parseLong(candParam.trim());

            com.orchestrivo.model.BenchmarkComparisonResult comparison = ServiceRegistry.benchmarkService.compareRuns(baselineId, candidateId, user.getId());

            String export = req.getParameter("export");
            if ("csv".equalsIgnoreCase(export)) {
                resp.setContentType("text/csv; charset=UTF-8");
                resp.setHeader("Content-Disposition", "attachment; filename=\"benchmark-comparison-" + baselineId + "-vs-" + candidateId + ".csv\"");
                java.io.PrintWriter writer = resp.getWriter();
                writer.println("Metric,Baseline (Run #" + baselineId + "),Candidate (Run #" + candidateId + "),Absolute Delta,Percentage Delta");
                writer.printf(java.util.Locale.US, "Controlled Match Status,%s,%s,%s,%s\n",
                        comparison.isControlledMatch() ? "Identical Match" : "Variant",
                        comparison.isControlledMatch() ? "Identical Match" : "Variant",
                        "N/A", "N/A");
                writer.printf(java.util.Locale.US, "Median Duration (µs),%.3f,%.3f,%.3f,%.2f%%\n",
                        comparison.getBaseline().getMedianMicros(), comparison.getCandidate().getMedianMicros(),
                        comparison.getDeltaMedianNanos() / 1000.0, comparison.getDeltaMedianPct());
                writer.printf(java.util.Locale.US, "Mean Duration (µs),%.3f,%.3f,%.3f,%.2f%%\n",
                        comparison.getBaseline().getMeanMicros(), comparison.getCandidate().getMeanMicros(),
                        comparison.getDeltaMeanNanos() / 1000.0, comparison.getDeltaMeanPct());
                writer.printf(java.util.Locale.US, "Min Duration (µs),%.3f,%.3f,%.3f,N/A\n",
                        comparison.getBaseline().getMinMicros(), comparison.getCandidate().getMinMicros(),
                        comparison.getDeltaMinNanos() / 1000.0);
                writer.printf(java.util.Locale.US, "Max Duration (µs),%.3f,%.3f,%.3f,N/A\n",
                        comparison.getBaseline().getMaxMicros(), comparison.getCandidate().getMaxMicros(),
                        comparison.getDeltaMaxNanos() / 1000.0);
                writer.printf(java.util.Locale.US, "Std Deviation (µs),%.3f,%.3f,%.3f,%.2f%%\n",
                        comparison.getBaseline().getStdDevMicros(), comparison.getCandidate().getStdDevMicros(),
                        comparison.getDeltaStdDevNanos() / 1000.0, comparison.getDeltaStdDevPct());
                writer.printf(java.util.Locale.US, "Actual Batch Throughput (runs/sec),%.1f,%.1f,%.1f,%.2f%%\n",
                        comparison.getBaselineActualBatchThroughput(), comparison.getCandidateActualBatchThroughput(),
                        comparison.getDeltaActualBatchThroughput(), comparison.getDeltaActualBatchThroughputPct());
                writer.printf(java.util.Locale.US, "Latency-derived Throughput Estimate (runs/sec),%.1f,%.1f,%.1f,%.2f%%\n",
                        comparison.getBaselineLatencyDerivedThroughputEstimate(), comparison.getCandidateLatencyDerivedThroughputEstimate(),
                        comparison.getCandidateLatencyDerivedThroughputEstimate() - comparison.getBaselineLatencyDerivedThroughputEstimate(),
                        comparison.getDeltaLatencyDerivedThroughputEstimatePct());
                writer.printf(java.util.Locale.US, "Speedup Factor,1.0x,%.2fx,N/A,N/A\n",
                        comparison.getSpeedupFactor());
                writer.flush();
                return;
            }

            boolean isFeatured = ServiceRegistry.benchmarkService.isFeaturedPair(baselineId, candidateId);
            req.setAttribute("comparison", comparison);
            req.setAttribute("isFeatured", isFeatured);
            req.getRequestDispatcher("/WEB-INF/views/benchmarks/compare.jsp").forward(req, resp);

        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid benchmark comparison ID.");
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (SecurityException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        }
    }

    private void handleFeaturePost(HttpServletRequest req, HttpServletResponse resp, User user) throws IOException {
        String action = req.getParameter("action");
        String baseParam = req.getParameter("baselineId");
        String candParam = req.getParameter("candidateId");

        if ("unpin".equalsIgnoreCase(action)) {
            ServiceRegistry.benchmarkService.clearFeaturedComparison();
            if (ServiceRegistry.auditService != null) {
                ServiceRegistry.auditService.log(user.getId(), "BENCHMARK_UNPIN", "BENCHMARK_COMPARISON", "global", Collections.emptyMap());
            }
        } else if ("pin".equalsIgnoreCase(action) && baseParam != null && candParam != null) {
            try {
                long baselineId = Long.parseLong(baseParam.trim());
                long candidateId = Long.parseLong(candParam.trim());
                ServiceRegistry.benchmarkService.setFeaturedComparison(baselineId, candidateId);
                if (ServiceRegistry.auditService != null) {
                    ServiceRegistry.auditService.log(user.getId(), "BENCHMARK_PIN", "BENCHMARK_COMPARISON", baselineId + ":" + candidateId,
                            Map.of("baselineId", baselineId, "candidateId", candidateId));
                }
            } catch (Exception ignored) {}
        }

        String redirect = req.getParameter("redirect");
        if (redirect != null && !redirect.trim().isEmpty()) {
            resp.sendRedirect(req.getContextPath() + redirect);
        } else if (baseParam != null && candParam != null) {
            resp.sendRedirect(req.getContextPath() + "/benchmarks/compare?baselineId=" + baseParam + "&candidateId=" + candParam);
        } else {
            resp.sendRedirect(req.getContextPath() + "/dashboard");
        }
    }

    private void handleDeletePost(HttpServletRequest req, HttpServletResponse resp, User user) throws IOException {
        try {
            long id = Long.parseLong(req.getParameter("id"));
            Optional<BenchmarkRunRecord> run = ServiceRegistry.benchmarkService.getBenchmarkById(id, user.getId());
            if (run.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Benchmark not found");
                return;
            }
            ServiceRegistry.benchmarkRepository.deleteById(id);
            if (ServiceRegistry.auditService != null) {
                ServiceRegistry.auditService.log(user.getId(), "BENCHMARK_DELETE", "BENCHMARK_RUN", String.valueOf(id), Collections.emptyMap());
            }
            resp.sendRedirect(req.getContextPath() + "/benchmarks");
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid benchmark ID");
        }
    }
}
