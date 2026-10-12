package com.orchestrivo.web;

import com.orchestrivo.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.UUID;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String email = req.getParameter("email");
        String password = req.getParameter("password");

        User user = ServiceRegistry.authService.authenticate(email, password);
        if (user != null) {
            HttpSession session = req.getSession(true);
            req.changeSessionId();
            session.setAttribute("user", user);
            session.setAttribute("csrfToken", UUID.randomUUID().toString());
            if (ServiceRegistry.auditService != null) {
                ServiceRegistry.auditService.log(user.getId(), "AUTH_LOGIN", "USER", String.valueOf(user.getId()), java.util.Map.of("email", user.getEmail()));
            }
            resp.sendRedirect(req.getContextPath() + "/dashboard");
        } else {
            req.setAttribute("error", "Invalid email or password");
            req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, resp);
        }
    }
}
