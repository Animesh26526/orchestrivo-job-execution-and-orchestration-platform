package com.orchestrivo.web;

import com.orchestrivo.persistence.DataSourceManager;
import com.orchestrivo.persistence.UserRepositoryJdbcImpl;
import com.orchestrivo.persistence.WorkloadRepositoryJdbcImpl;
import com.orchestrivo.persistence.ExperimentRunRepositoryJdbcImpl;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class AppConfigListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // Configure Session tracking
        jakarta.servlet.SessionCookieConfig sessionCookieConfig = sce.getServletContext().getSessionCookieConfig();
        sessionCookieConfig.setHttpOnly(true);
        sessionCookieConfig.setAttribute("SameSite", "Lax");
        
        // Use only cookies for session tracking (disable URL rewriting)
        sce.getServletContext().setSessionTrackingModes(java.util.Set.of(jakarta.servlet.SessionTrackingMode.COOKIE));
        sce.getServletContext().setSessionTimeout(30); // 30 minutes

        // Enable secure cookies if running in production over HTTPS
        if ("true".equalsIgnoreCase(System.getenv("PRODUCTION_HTTPS"))) {
            sessionCookieConfig.setSecure(true);
        }
        // Initialize DataSource (will read from env or local config)
        DataSourceManager.getDataSource();
        
        UserRepositoryJdbcImpl ur = new UserRepositoryJdbcImpl();
        WorkloadRepositoryJdbcImpl wr = new WorkloadRepositoryJdbcImpl();
        ExperimentRunRepositoryJdbcImpl er = new ExperimentRunRepositoryJdbcImpl();
        com.orchestrivo.persistence.BenchmarkRepositoryJdbcImpl br = new com.orchestrivo.persistence.BenchmarkRepositoryJdbcImpl();
        com.orchestrivo.persistence.AuditLogRepositoryJdbcImpl ar = new com.orchestrivo.persistence.AuditLogRepositoryJdbcImpl();

        ServiceRegistry.initialize(ur, wr, er, br, ar);
        System.out.println("Orchestrivo initialized successfully.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        DataSourceManager.closeDataSource();
        System.out.println("Orchestrivo shutting down.");
    }
}
