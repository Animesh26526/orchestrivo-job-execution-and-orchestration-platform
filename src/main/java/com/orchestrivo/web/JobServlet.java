package com.orchestrivo.web;

import com.google.gson.Gson;
import com.orchestrivo.model.AlgorithmConfigSpec;
import com.orchestrivo.model.BatchJobResult;
import com.orchestrivo.model.ExecutionJobRecord;
import com.orchestrivo.model.PagedResult;
import com.orchestrivo.model.QueueMetrics;
import com.orchestrivo.model.User;
import com.orchestrivo.model.WorkloadRecord;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@WebServlet({"/jobs", "/jobs/view", "/jobs/status", "/jobs/retry", "/jobs/cancel", "/jobs/batch"})
public class JobServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        User user = (User) req.getSession().getAttribute("user");

        if ("/jobs/view".equals(path)) {
            handleViewGet(req, resp, user);
        } else if ("/jobs/status".equals(path)) {
            handleStatusJsonGet(req, resp);
        } else if ("/jobs/batch".equals(path)) {
            handleBatchGet(req, resp, user);
        } else {
            handleListGet(req, resp, user);
        }
    }

    private void handleListGet(HttpServletRequest req, HttpServletResponse resp, User user) throws ServletException, IOException {
        String statusFilter = req.getParameter("status");
        String sortBy = req.getParameter("sortBy");
        String sortDir = req.getParameter("sortDir");

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

        PagedResult<ExecutionJobRecord> pagedResult = ServiceRegistry.executionJobService.getJobsPaged(
                user.getId(), statusFilter, sortBy, sortDir, page, pageSize
        );
        QueueMetrics metrics = ServiceRegistry.executionJobService.getQueueMetrics(user.getId());

        req.setAttribute("pagedResult", pagedResult);
        req.setAttribute("jobs", pagedResult.getItems());
        req.setAttribute("metrics", metrics);
        req.setAttribute("selectedStatus", statusFilter != null ? statusFilter : "ALL");
        req.setAttribute("sortBy", sortBy != null ? sortBy : "created_at");
        req.setAttribute("sortDir", sortDir != null ? sortDir : "DESC");

        req.getRequestDispatcher("/WEB-INF/views/jobs/list.jsp").forward(req, resp);
    }

    private void handleViewGet(HttpServletRequest req, HttpServletResponse resp, User user) throws ServletException, IOException {
        try {
            long id = Long.parseLong(req.getParameter("id"));
            Optional<ExecutionJobRecord> job = ServiceRegistry.executionJobService.getJobById(id, user.getId());
            if (job.isPresent()) {
                req.setAttribute("job", job.get());
                req.getRequestDispatcher("/WEB-INF/views/jobs/view.jsp").forward(req, resp);
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Job not found");
            }
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid job ID");
        }
    }

    private void handleStatusJsonGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = (User) req.getSession().getAttribute("user");
        try {
            long id = Long.parseLong(req.getParameter("id"));
            Optional<ExecutionJobRecord> jobOpt = ServiceRegistry.executionJobService.getJobById(id, user.getId());
            if (jobOpt.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Job not found");
                return;
            }

            ExecutionJobRecord job = jobOpt.get();
            Map<String, Object> json = new HashMap<>();
            json.put("id", job.getId());
            json.put("status", job.getStatus().name());
            json.put("attemptCount", job.getAttemptCount());
            json.put("maxAttempts", job.getMaxAttempts());
            json.put("experimentRunId", job.getExperimentRunId());
            json.put("executionDurationMs", job.getExecutionDurationMs());
            json.put("executionDurationNanos", job.getExecutionDurationNanos());
            json.put("formattedDuration", job.getFormattedDuration());
            json.put("batchId", job.getBatchId());
            json.put("queueWaitMs", job.getQueueWaitMs());
            json.put("errorMessage", job.getUserFacingErrorMessage());

            resp.setContentType("application/json; charset=UTF-8");
            resp.getWriter().write(new Gson().toJson(json));
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid job ID");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        User user = (User) req.getSession().getAttribute("user");

        if ("/jobs/retry".equals(path)) {
            handleRetryPost(req, resp, user);
        } else if ("/jobs/cancel".equals(path)) {
            handleCancelPost(req, resp, user);
        } else if ("/jobs/batch".equals(path)) {
            handleBatchPost(req, resp, user);
        }
    }

    private void handleBatchGet(HttpServletRequest req, HttpServletResponse resp, User user) throws ServletException, IOException {
        String batchId = req.getParameter("batchId");
        if (batchId != null && !batchId.trim().isEmpty()) {
            List<ExecutionJobRecord> batchJobs = ServiceRegistry.executionJobService.getJobsByBatchId(batchId.trim(), user.getId());
            req.setAttribute("batchJobs", batchJobs);
            req.setAttribute("batchId", batchId.trim());
        }

        String workloadIdParam = req.getParameter("workloadId");
        if (workloadIdParam != null && !workloadIdParam.trim().isEmpty()) {
            try {
                long wid = Long.parseLong(workloadIdParam.trim());
                Optional<WorkloadRecord> wr = ServiceRegistry.workloadService.getWorkloadById(wid, user.getId());
                wr.ifPresent(w -> req.setAttribute("selectedWorkload", w));
            } catch (NumberFormatException ignored) {}
        }

        List<WorkloadRecord> userWorkloads = ServiceRegistry.workloadService.getWorkloadsByUser(user.getId());
        req.setAttribute("workloads", userWorkloads);
        req.getRequestDispatcher("/WEB-INF/views/jobs/batch.jsp").forward(req, resp);
    }

    private void handleBatchPost(HttpServletRequest req, HttpServletResponse resp, User user) throws ServletException, IOException {
        String workloadIdStr = req.getParameter("workloadId");
        if (workloadIdStr == null || workloadIdStr.trim().isEmpty()) {
            req.setAttribute("error", "Please select a valid workload.");
            handleBatchGet(req, resp, user);
            return;
        }

        long workloadId;
        try {
            workloadId = Long.parseLong(workloadIdStr.trim());
        } catch (NumberFormatException e) {
            req.setAttribute("error", "Invalid workload ID.");
            handleBatchGet(req, resp, user);
            return;
        }

        Optional<WorkloadRecord> wrOpt = ServiceRegistry.workloadService.getWorkloadById(workloadId, user.getId());
        if (wrOpt.isEmpty()) {
            req.setAttribute("error", "Workload not found or access denied.");
            handleBatchGet(req, resp, user);
            return;
        }

        String[] selectedAlgorithms = req.getParameterValues("algorithms");
        if (selectedAlgorithms == null || selectedAlgorithms.length == 0) {
            req.setAttribute("error", "Please select at least one algorithm to execute.");
            req.setAttribute("selectedWorkloadId", workloadId);
            handleBatchGet(req, resp, user);
            return;
        }

        int quantum = 3;
        try {
            String qStr = req.getParameter("quantum");
            if (qStr != null && !qStr.trim().isEmpty()) {
                quantum = Integer.parseInt(qStr.trim());
            }
        } catch (NumberFormatException ignored) {}

        List<AlgorithmConfigSpec> configs = new ArrayList<>();
        for (String algo : selectedAlgorithms) {
            if ("ROUND_ROBIN".equalsIgnoreCase(algo)) {
                configs.add(new AlgorithmConfigSpec("ROUND_ROBIN", quantum));
            } else {
                configs.add(new AlgorithmConfigSpec(algo));
            }
        }

        try {
            BatchJobResult batchResult = ServiceRegistry.executionJobService.enqueueBatch(wrOpt.get(), configs, user.getId());
            if (ServiceRegistry.auditService != null) {
                ServiceRegistry.auditService.log(user.getId(), "BATCH_ENQUEUED", "EXECUTION_JOB",
                        batchResult.getBatchId(),
                        Map.of("workloadId", workloadId, "count", batchResult.getJobCount()));
            }
            resp.sendRedirect(req.getContextPath() + "/jobs/batch?batchId=" + batchResult.getBatchId());
        } catch (IllegalArgumentException | SecurityException e) {
            req.setAttribute("error", e.getMessage());
            req.setAttribute("selectedWorkloadId", workloadId);
            handleBatchGet(req, resp, user);
        }
    }

    private void handleRetryPost(HttpServletRequest req, HttpServletResponse resp, User user) throws IOException {
        try {
            long id = Long.parseLong(req.getParameter("id"));
            boolean ok = ServiceRegistry.executionJobService.retryJob(id, user.getId());
            if (ok) {
                resp.sendRedirect(req.getContextPath() + "/jobs/view?id=" + id);
            } else {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Job is not eligible for retry.");
            }
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid job ID");
        } catch (SecurityException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        } catch (IllegalStateException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void handleCancelPost(HttpServletRequest req, HttpServletResponse resp, User user) throws IOException {
        try {
            long id = Long.parseLong(req.getParameter("id"));
            boolean ok = ServiceRegistry.executionJobService.cancelJob(id, user.getId());
            if (ok) {
                resp.sendRedirect(req.getContextPath() + "/jobs/view?id=" + id);
            } else {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Job cannot be cancelled in its current state.");
            }
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid job ID");
        } catch (SecurityException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        } catch (IllegalStateException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }
}
