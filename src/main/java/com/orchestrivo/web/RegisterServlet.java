package com.orchestrivo.web;

import com.orchestrivo.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.UUID;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String fullName = req.getParameter("fullName");
        String email = req.getParameter("email");
        String password = req.getParameter("password");
        String confirmPassword = req.getParameter("confirmPassword");
        
        if (confirmPassword != null && !confirmPassword.equals(password)) {
            req.setAttribute("error", "Passwords do not match.");
            req.setAttribute("fullName", fullName);
            req.setAttribute("email", email);
            req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(req, resp);
            return;
        }

        try {
            User user;
            if (fullName != null && !fullName.trim().isEmpty()) {
                user = ServiceRegistry.authService.register(email, password, fullName, "USER");
            } else {
                user = ServiceRegistry.authService.register(email, password, "USER");
            }
            HttpSession session = req.getSession(true);
            req.changeSessionId();
            session.setAttribute("user", user);
            session.setAttribute("csrfToken", UUID.randomUUID().toString());
            if (ServiceRegistry.auditService != null) {
                ServiceRegistry.auditService.log(user.getId(), "AUTH_REGISTER", "USER", String.valueOf(user.getId()), java.util.Map.of("email", user.getEmail()));
            }
            resp.sendRedirect(req.getContextPath() + "/dashboard");
        } catch (IllegalArgumentException e) {
            req.setAttribute("error", e.getMessage());
            req.setAttribute("fullName", fullName);
            req.setAttribute("email", email);
            req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(req, resp);
        }
    }
}
