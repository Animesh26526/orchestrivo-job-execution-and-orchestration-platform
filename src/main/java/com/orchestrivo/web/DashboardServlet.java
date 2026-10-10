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
        req.getRequestDispatcher("/WEB-INF/views/dashboard/index.jsp").forward(req, resp);
    }
}
