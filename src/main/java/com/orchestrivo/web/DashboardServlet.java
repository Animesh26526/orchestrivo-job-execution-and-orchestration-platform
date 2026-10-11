package com.orchestrivo.web;

import com.orchestrivo.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = (User) req.getSession().getAttribute("user");

        req.setAttribute("workloads", ServiceRegistry.workloadService.getWorkloadsByUser(user.getId()));
        req.setAttribute("experiments", ServiceRegistry.experimentService.getExperimentsByUser(user.getId()));

        if (ServiceRegistry.benchmarkService != null) {
            req.setAttribute("benchmarks", ServiceRegistry.benchmarkService.getBenchmarksPaged(user.getId(), 1, 5).getItems());
        }

        if (ServiceRegistry.executionJobService != null) {
            req.setAttribute("queueMetrics", ServiceRegistry.executionJobService.getQueueMetrics(user.getId()));
            req.setAttribute("recentJobs", ServiceRegistry.executionJobService.getRecentJobs(user.getId(), 5));
        }

        if (ServiceRegistry.auditLogRepository != null) {
            req.setAttribute("recentActivity", ServiceRegistry.auditLogRepository.findByActorId(user.getId(), 6));
        }

        req.getRequestDispatcher("/WEB-INF/views/dashboard/index.jsp").forward(req, resp);
    }
}
