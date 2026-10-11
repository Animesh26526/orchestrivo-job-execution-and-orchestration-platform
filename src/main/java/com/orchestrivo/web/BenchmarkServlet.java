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

@WebServlet({"/benchmarks", "/benchmarks/run", "/benchmarks/view", "/benchmarks/delete"})
public class BenchmarkServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        User user = (User) req.getSession().getAttribute("user");

        if ("/benchmarks/run".equals(path)) {
            req.getRequestDispatcher("/WEB-INF/views/benchmarks/new.jsp").forward(req, resp);
        } else if ("/benchmarks/view".equals(path)) {
            handleViewGet(req, resp, user);
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
        }
    }

    private void handleRunPost(HttpServletRequest req, HttpServletResponse resp, User user) throws ServletException, IOException {
        BenchmarkService.BenchmarkConfig config = new BenchmarkService.BenchmarkConfig();
        try {
            config.setAlgorithmName(req.getParameter("algorithmName"));

            String q = req.getParameter("quantum");
            if (q != null && !q.trim().isEmpty()) config.setQuantum(Integer.parseInt(q.trim()));

            String pc = req.getParameter("processCount");
            if (pc != null && !pc.trim().isEmpty()) config.setProcessCount(Integer.parseInt(pc.trim()));

            String minA = req.getParameter("minArrival");
            if (minA != null && !minA.trim().isEmpty()) config.setMinArrival(Integer.parseInt(minA.trim()));

            String maxA = req.getParameter("maxArrival");
            if (maxA != null && !maxA.trim().isEmpty()) config.setMaxArrival(Integer.parseInt(maxA.trim()));

            String minB = req.getParameter("minBurst");
            if (minB != null && !minB.trim().isEmpty()) config.setMinBurst(Integer.parseInt(minB.trim()));

            String maxB = req.getParameter("maxBurst");
            if (maxB != null && !maxB.trim().isEmpty()) config.setMaxBurst(Integer.parseInt(maxB.trim()));

            String s = req.getParameter("seed");
            if (s != null && !s.trim().isEmpty()) config.setSeed(Long.parseLong(s.trim()));

            String w = req.getParameter("warmupIterations");
            if (w != null && !w.trim().isEmpty()) config.setWarmupIterations(Integer.parseInt(w.trim()));

            String m = req.getParameter("measuredIterations");
            if (m != null && !m.trim().isEmpty()) config.setMeasuredIterations(Integer.parseInt(m.trim()));

            BenchmarkRunRecord run = ServiceRegistry.benchmarkService.runBenchmark(user.getId(), config);
            if (ServiceRegistry.auditService != null) {
                ServiceRegistry.auditService.log(user.getId(), "BENCHMARK_RUN", "BENCHMARK_RUN", String.valueOf(run.getId()), Map.of("algorithm", run.getAlgorithmName(), "processCount", run.getProcessCount(), "medianMicros", run.getMedianMicros()));
            }
            resp.sendRedirect(req.getContextPath() + "/benchmarks/view?id=" + run.getId());
        } catch (Exception e) {
            req.setAttribute("error", e.getMessage());
            req.setAttribute("config", config);
            req.getRequestDispatcher("/WEB-INF/views/benchmarks/new.jsp").forward(req, resp);
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
