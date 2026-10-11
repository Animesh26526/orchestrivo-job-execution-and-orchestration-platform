package com.orchestrivo.web;

import com.google.gson.Gson;
import com.orchestrivo.model.ExecutionJobRecord;
import com.orchestrivo.model.PagedResult;
import com.orchestrivo.model.QueueMetrics;
import com.orchestrivo.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@WebServlet({"/jobs", "/jobs/view", "/jobs/status", "/jobs/retry", "/jobs/cancel"})
public class JobServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        User user = (User) req.getSession().getAttribute("user");

        if ("/jobs/view".equals(path)) {
            handleViewGet(req, resp, user);
        } else if ("/jobs/status".equals(path)) {
            handleStatusJsonGet(req, resp);
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
