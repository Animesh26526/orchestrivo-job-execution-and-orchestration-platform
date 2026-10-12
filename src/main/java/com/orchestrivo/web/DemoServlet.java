package com.orchestrivo.web;

import com.orchestrivo.model.User;
import com.orchestrivo.service.DemoDataSeeder;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.Optional;

@WebServlet({"/demo", "/demo/seed"})
public class DemoServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Optional<User> demoUserOpt = ServiceRegistry.userRepository != null
                ? ServiceRegistry.userRepository.findByEmail(DemoDataSeeder.DEMO_EMAIL)
                : Optional.empty();

        req.setAttribute("demoUserExists", demoUserOpt.isPresent());
        if (demoUserOpt.isPresent()) {
            User demoUser = demoUserOpt.get();
            req.setAttribute("demoUserId", demoUser.getId());
            if (ServiceRegistry.workloadRepository != null) {
                req.setAttribute("workloadCount", ServiceRegistry.workloadRepository.findByOwnerId(demoUser.getId()).size());
            }
            if (ServiceRegistry.executionJobService != null) {
                req.setAttribute("queueMetrics", ServiceRegistry.executionJobService.getQueueMetrics(demoUser.getId()));
            }
            if (ServiceRegistry.benchmarkRepository != null) {
                req.setAttribute("benchmarkCount", ServiceRegistry.benchmarkRepository.countByOwnerId(demoUser.getId()));
            }
        }

        req.setAttribute("demoEmail", DemoDataSeeder.DEMO_EMAIL);
        req.setAttribute("demoPassword", DemoDataSeeder.DEMO_PASSWORD);

        req.getRequestDispatcher("/WEB-INF/views/demo/index.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/demo/seed".equals(path)) {
            boolean reset = "true".equalsIgnoreCase(req.getParameter("reset"));
            DemoDataSeeder.DemoSeedResult result = DemoDataSeeder.seed(reset);

            HttpSession session = req.getSession();
            session.setAttribute("flashMessage", result.message);
            session.setAttribute("flashType", "SUCCESS".equalsIgnoreCase(result.status) ? "success" : "warning");

            // If auto-login requested or user is currently not logged in
            String autoLogin = req.getParameter("autoLogin");
            if ("true".equalsIgnoreCase(autoLogin) || session.getAttribute("user") == null) {
                Optional<User> demoUserOpt = ServiceRegistry.userRepository.findByEmail(DemoDataSeeder.DEMO_EMAIL);
                demoUserOpt.ifPresent(user -> session.setAttribute("user", user));
            }

            resp.sendRedirect(req.getContextPath() + "/dashboard");
        } else {
            resp.sendRedirect(req.getContextPath() + "/demo");
        }
    }
}
