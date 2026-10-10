package com.orchestrivo.web;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebFilter("/*")
public class AuthFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        
        String path = request.getRequestURI().substring(request.getContextPath().length());
        
        boolean isPublic = path.equals("/") || path.equals("/login") || path.equals("/register") || path.startsWith("/assets/");
        
        HttpSession session = request.getSession(false);
        boolean isLoggedIn = (session != null && session.getAttribute("user") != null);
        
        if (!isPublic && !isLoggedIn) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // Basic CSRF check for state-changing requests
        if (isLoggedIn && request.getMethod().equalsIgnoreCase("POST") && !isPublic) {
            String sessionCsrf = (String) session.getAttribute("csrfToken");
            String requestCsrf = request.getParameter("csrfToken");
            if (sessionCsrf == null || !sessionCsrf.equals(requestCsrf)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF Token");
                return;
            }
        }
        
        chain.doFilter(request, response);
    }
}
