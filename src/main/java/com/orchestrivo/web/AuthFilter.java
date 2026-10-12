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
        
        String servletPath = request.getServletPath();
        String pathInfo = request.getPathInfo();
        String path = (servletPath != null ? servletPath : "") + (pathInfo != null ? pathInfo : "");
        if (path.isEmpty()) {
            if (request.getRequestURI() != null) {
                String uri = request.getRequestURI();
                String ctx = request.getContextPath() != null ? request.getContextPath() : "";
                if (uri.startsWith(ctx)) {
                    path = uri.substring(ctx.length());
                } else {
                    path = uri;
                }
            }
        }
        if (path.isEmpty()) {
            path = "/";
        }
        int semicolonIdx = path.indexOf(';');
        if (semicolonIdx != -1) {
            path = path.substring(0, semicolonIdx);
        }
        
        boolean isPublic = path.equals("/") || path.equals("/login") || path.equals("/register") 
                || path.startsWith("/assets/") || path.startsWith("/demo");
        
        HttpSession session = request.getSession(true);
        boolean isLoggedIn = (session.getAttribute("user") != null);
        
        // Ensure a CSRF token always exists for the session
        if (session.getAttribute("csrfToken") == null) {
            session.setAttribute("csrfToken", java.util.UUID.randomUUID().toString());
        }
        request.setAttribute("csrfToken", session.getAttribute("csrfToken"));
        
        if (!isPublic && !isLoggedIn) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // Validate CSRF for any state-changing requests
        String method = request.getMethod();
        if (method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("PUT") || method.equalsIgnoreCase("DELETE")) {
            String sessionCsrf = (String) session.getAttribute("csrfToken");
            String requestCsrf = request.getParameter("csrfToken");
            if (requestCsrf == null || requestCsrf.trim().isEmpty()) {
                requestCsrf = request.getHeader("X-CSRF-TOKEN");
            }
            if (requestCsrf == null || requestCsrf.trim().isEmpty()) {
                requestCsrf = request.getHeader("X-XSRF-TOKEN");
            }

            if (sessionCsrf == null || !sessionCsrf.equals(requestCsrf)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing CSRF Token");
                return;
            }
        }
        
        chain.doFilter(request, response);
    }
}
