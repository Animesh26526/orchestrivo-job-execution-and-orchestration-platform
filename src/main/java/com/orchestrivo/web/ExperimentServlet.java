package com.orchestrivo.web;

import com.orchestrivo.model.ComparisonResult;
import com.orchestrivo.model.ExperimentRunRecord;
import com.orchestrivo.model.PagedResult;
import com.orchestrivo.model.User;
import com.orchestrivo.model.WorkloadRecord;
import com.orchestrivo.util.CsvExporter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.*;

@WebServlet({"/experiments", "/experiments/run", "/experiments/view", "/experiments/compare", "/experiments/export", "/experiments/delete"})
public class ExperimentServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        User user = (User) req.getSession().getAttribute("user");

        if ("/experiments/view".equals(path)) {
            handleViewGet(req, resp, user);
        } else if ("/experiments/export".equals(path)) {
            handleExportGet(req, resp, user);
        } else if ("/experiments/compare".equals(path)) {
            handleCompareGet(req, resp, user);
        } else {
            handleListGet(req, resp, user);
        }
    }

    private void handleListGet(HttpServletRequest req, HttpServletResponse resp, User user) throws ServletException, IOException {
        Long workloadId = null;
        try {
            String wIdStr = req.getParameter("workloadId");
            if (wIdStr != null && !wIdStr.trim().isEmpty()) {
                workloadId = Long.parseLong(wIdStr.trim());
            }
        } catch (NumberFormatException ignored) {}

        String algorithm = req.getParameter("algorithm");
        String sortBy = req.getParameter("sortBy");
        String sortDir = req.getParameter("sortDir");

        int page = 1;
        int pageSize = 10;
        try {
            String p = req.getParameter("page");
            if (p != null && !p.trim().isEmpty()) {
                page = Integer.parseInt(p.trim());
            }
        } catch (NumberFormatException ignored) {}

        try {
            String ps = req.getParameter("pageSize");
            if (ps != null && !ps.trim().isEmpty()) {
                pageSize = Integer.parseInt(ps.trim());
            }
        } catch (NumberFormatException ignored) {}

        PagedResult<ExperimentRunRecord> pagedResult = ServiceRegistry.experimentService.getExperimentsPaged(
                user.getId(), workloadId, algorithm, sortBy, sortDir, page, pageSize
        );

        List<WorkloadRecord> userWorkloads = ServiceRegistry.workloadService.getWorkloadsByUser(user.getId());

        req.setAttribute("pagedResult", pagedResult);
        req.setAttribute("experiments", pagedResult.getItems());
        req.setAttribute("workloads", userWorkloads);
        req.setAttribute("selectedWorkloadId", workloadId);
        req.setAttribute("selectedAlgorithm", algorithm);
        req.setAttribute("sortBy", sortBy != null ? sortBy : "created_at");
        req.setAttribute("sortDir", sortDir != null ? sortDir : "DESC");

        req.getRequestDispatcher("/WEB-INF/views/experiments/list.jsp").forward(req, resp);
    }

    private void handleViewGet(HttpServletRequest req, HttpServletResponse resp, User user) throws ServletException, IOException {
        try {
            long id = Long.parseLong(req.getParameter("id"));
            Optional<ExperimentRunRecord> run = ServiceRegistry.experimentService.getExperimentById(id, user.getId());
            if (run.isPresent()) {
                req.setAttribute("experiment", run.get());
                req.getRequestDispatcher("/WEB-INF/views/experiments/view.jsp").forward(req, resp);
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Experiment not found");
            }
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid experiment ID");
        }
    }

    private void handleExportGet(HttpServletRequest req, HttpServletResponse resp, User user) throws IOException {
        try {
            long id = Long.parseLong(req.getParameter("id"));
            Optional<ExperimentRunRecord> run = ServiceRegistry.experimentService.getExperimentById(id, user.getId());
            if (run.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Experiment not found");
                return;
            }

            ExperimentRunRecord record = run.get();
            if (ServiceRegistry.auditService != null) {
                ServiceRegistry.auditService.log(user.getId(), "CSV_EXPORT", "EXPERIMENT_RUN", String.valueOf(id), Map.of("algorithm", record.getAlgorithmName()));
            }

            resp.setContentType("text/csv; charset=UTF-8");
            resp.setHeader("Content-Disposition", "attachment; filename=\"experiment-" + id + ".csv\"");
            CsvExporter.exportExperimentRun(record, resp.getWriter());
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid experiment ID");
        }
    }

    private void handleCompareGet(HttpServletRequest req, HttpServletResponse resp, User user) throws ServletException, IOException {
        String[] runIdParams = req.getParameterValues("runId");
        if (runIdParams == null || runIdParams.length == 0) {
            resp.sendRedirect(req.getContextPath() + "/experiments");
            return;
        }

        List<Long> runIds = new ArrayList<>();
        for (String s : runIdParams) {
            try {
                if (s != null && !s.trim().isEmpty()) {
                    runIds.add(Long.parseLong(s.trim()));
                }
            } catch (NumberFormatException ignored) {}
        }

        if (runIds.isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/experiments");
            return;
        }

        try {
            ComparisonResult comparison = ServiceRegistry.experimentService.compareExperiments(runIds, user.getId());

            String exportParam = req.getParameter("export");
            if ("csv".equalsIgnoreCase(exportParam)) {
                if (ServiceRegistry.auditService != null) {
                    ServiceRegistry.auditService.log(user.getId(), "CSV_EXPORT", "COMPARISON", String.join(",", runIdParams), Map.of("runCount", runIds.size()));
                }
                resp.setContentType("text/csv; charset=UTF-8");
                resp.setHeader("Content-Disposition", "attachment; filename=\"algorithm-comparison.csv\"");
                CsvExporter.exportComparison(comparison.getRuns(), resp.getWriter());
                return;
            }

            req.setAttribute("comparison", comparison);
            req.getRequestDispatcher("/WEB-INF/views/experiments/compare.jsp").forward(req, resp);
        } catch (SecurityException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        User user = (User) req.getSession().getAttribute("user");

        if ("/experiments/run".equals(path)) {
            handleRunPost(req, resp, user);
        } else if ("/experiments/delete".equals(path)) {
            handleDeletePost(req, resp, user);
        } else if ("/experiments/compare".equals(path)) {
            handleCompareGet(req, resp, user);
        }
    }

    private void handleRunPost(HttpServletRequest req, HttpServletResponse resp, User user) throws IOException {
        try {
            long workloadId = Long.parseLong(req.getParameter("workloadId"));
            String algorithmName = req.getParameter("algorithmName");
            int quantum = 3;
            String qStr = req.getParameter("quantum");
            if (qStr != null && !qStr.trim().isEmpty()) {
                quantum = Integer.parseInt(qStr);
            }

            Optional<WorkloadRecord> wr = ServiceRegistry.workloadService.getWorkloadById(workloadId, user.getId());
            if (wr.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Workload not found or not owned");
                return;
            }

            ExperimentRunRecord run = ServiceRegistry.experimentService.runExperiment(wr.get(), algorithmName, quantum, user.getId());
            if (ServiceRegistry.auditService != null) {
                ServiceRegistry.auditService.log(user.getId(), "EXPERIMENT_RUN", "EXPERIMENT_RUN", String.valueOf(run.getId()), Map.of("algorithm", run.getAlgorithmName(), "workloadId", workloadId));
            }
            resp.sendRedirect(req.getContextPath() + "/experiments/view?id=" + run.getId());
        } catch (Exception e) {
            getServletContext().log("Experiment execution failed for workloadId=" + req.getParameter("workloadId") + ": " + e.toString());
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Failed to start experiment due to invalid configuration or internal error.");
        }
    }

    private void handleDeletePost(HttpServletRequest req, HttpServletResponse resp, User user) throws IOException {
        try {
            long id = Long.parseLong(req.getParameter("id"));
            ServiceRegistry.experimentService.deleteExperiment(id, user.getId());
            if (ServiceRegistry.auditService != null) {
                ServiceRegistry.auditService.log(user.getId(), "EXPERIMENT_DELETE", "EXPERIMENT_RUN", String.valueOf(id), Collections.emptyMap());
            }
            resp.sendRedirect(req.getContextPath() + "/experiments");
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid experiment ID");
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (SecurityException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        }
    }
}
