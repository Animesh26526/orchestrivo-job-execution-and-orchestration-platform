package com.orchestrivo.web;

import com.orchestrivo.domain.ProcessSpec;
import com.orchestrivo.model.User;
import com.orchestrivo.model.WorkloadRecord;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@WebServlet({"/workloads", "/workloads/new", "/workloads/view", "/workloads/delete"})
public class WorkloadServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        User user = (User) req.getSession().getAttribute("user");

        if ("/workloads/new".equals(path)) {
            req.getRequestDispatcher("/WEB-INF/views/workloads/new.jsp").forward(req, resp);
        } else if ("/workloads/view".equals(path)) {
            try {
                long id = Long.parseLong(req.getParameter("id"));
                Optional<WorkloadRecord> wr = ServiceRegistry.workloadService.getWorkloadById(id, user.getId());
                if (wr.isPresent()) {
                    req.setAttribute("workload", wr.get());
                    req.getRequestDispatcher("/WEB-INF/views/workloads/view.jsp").forward(req, resp);
                } else {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Workload not found");
                }
            } catch (NumberFormatException e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid workload ID");
            }
        } else {
            req.setAttribute("workloads", ServiceRegistry.workloadService.getWorkloadsByUser(user.getId()));
            req.getRequestDispatcher("/WEB-INF/views/workloads/list.jsp").forward(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        User user = (User) req.getSession().getAttribute("user");

        if ("/workloads/new".equals(path)) {
            try {
                String name = req.getParameter("name");
                String description = req.getParameter("description");
                String[] pids = req.getParameterValues("processId");
                String[] arrivals = req.getParameterValues("arrivalTime");
                String[] bursts = req.getParameterValues("burstTime");

                List<ProcessSpec> processes = new ArrayList<>();
                if (pids != null) {
                    for (int i = 0; i < pids.length; i++) {
                        processes.add(new ProcessSpec(pids[i], Integer.parseInt(arrivals[i]), Integer.parseInt(bursts[i])));
                    }
                }
                ServiceRegistry.workloadService.createManualWorkload(user.getId(), name, description, processes);
                resp.sendRedirect(req.getContextPath() + "/workloads");
            } catch (Exception e) {
                req.setAttribute("error", e.getMessage());
                req.getRequestDispatcher("/WEB-INF/views/workloads/new.jsp").forward(req, resp);
            }
        } else if ("/workloads/delete".equals(path)) {
            try {
                long id = Long.parseLong(req.getParameter("id"));
                ServiceRegistry.workloadService.deleteWorkload(id, user.getId());
                resp.sendRedirect(req.getContextPath() + "/workloads");
            } catch (IllegalArgumentException e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            }
        }
    }
}
