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
        String email = req.getParameter("email");
        String password = req.getParameter("password");
        
        try {
            User user = ServiceRegistry.authService.register(email, password, "USER");
            HttpSession session = req.getSession(true);
            session.setAttribute("user", user);
            session.setAttribute("csrfToken", UUID.randomUUID().toString());
            resp.sendRedirect(req.getContextPath() + "/dashboard");
        } catch (IllegalArgumentException e) {
            req.setAttribute("error", e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(req, resp);
        }
    }
}
