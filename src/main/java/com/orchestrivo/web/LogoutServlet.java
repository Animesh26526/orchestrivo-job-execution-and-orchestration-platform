package com.orchestrivo.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            com.orchestrivo.model.User user = (com.orchestrivo.model.User) session.getAttribute("user");
            if (user != null && ServiceRegistry.auditService != null) {
                ServiceRegistry.auditService.log(user.getId(), "AUTH_LOGOUT", "USER", String.valueOf(user.getId()), java.util.Map.of("email", user.getEmail()));
            }
            session.invalidate();
        }
        resp.sendRedirect(req.getContextPath() + "/login");
    }
}
