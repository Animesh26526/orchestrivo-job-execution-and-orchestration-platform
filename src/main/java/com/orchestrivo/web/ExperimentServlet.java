package com.orchestrivo.web;

import com.orchestrivo.model.ExperimentRunRecord;
import com.orchestrivo.model.User;
import com.orchestrivo.model.WorkloadRecord;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Optional;

@WebServlet({"/experiments", "/experiments/run", "/experiments/view"})
public class ExperimentServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        User user = (User) req.getSession().getAttribute("user");

        if ("/experiments/view".equals(path)) {
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
        } else {
            req.setAttribute("experiments", ServiceRegistry.experimentService.getExperimentsByUser(user.getId()));
            req.getRequestDispatcher("/WEB-INF/views/experiments/list.jsp").forward(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        User user = (User) req.getSession().getAttribute("user");

        if ("/experiments/run".equals(path)) {
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
                resp.sendRedirect(req.getContextPath() + "/experiments/view?id=" + run.getId());
            } catch (Exception e) {
                getServletContext().log("Experiment execution failed for workloadId=" + req.getParameter("workloadId") + ": " + e.toString());
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Failed to start experiment due to invalid configuration or internal error.");
            }
        }
    }
}
