package com.orchestrivo.web;

import com.orchestrivo.persistence.DataSourceManager;
import com.orchestrivo.persistence.UserRepositoryJdbcImpl;
import com.orchestrivo.persistence.WorkloadRepositoryJdbcImpl;
import com.orchestrivo.persistence.ExperimentRunRepositoryJdbcImpl;
import com.orchestrivo.persistence.BenchmarkRepositoryJdbcImpl;
import com.orchestrivo.persistence.AuditLogRepositoryJdbcImpl;
import com.orchestrivo.persistence.ExecutionJobRepositoryJdbcImpl;
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

        try {
            // Initialize DataSource (reads from env or local config)
            DataSourceManager.getDataSource();
            
            UserRepositoryJdbcImpl ur = new UserRepositoryJdbcImpl();
            WorkloadRepositoryJdbcImpl wr = new WorkloadRepositoryJdbcImpl();
            ExperimentRunRepositoryJdbcImpl er = new ExperimentRunRepositoryJdbcImpl();
            BenchmarkRepositoryJdbcImpl br = new BenchmarkRepositoryJdbcImpl();
            AuditLogRepositoryJdbcImpl ar = new AuditLogRepositoryJdbcImpl();
            ExecutionJobRepositoryJdbcImpl jr = new ExecutionJobRepositoryJdbcImpl();
            com.orchestrivo.persistence.SystemSettingsRepositoryJdbcImpl sr = new com.orchestrivo.persistence.SystemSettingsRepositoryJdbcImpl();

            ServiceRegistry.initialize(ur, wr, er, br, ar, jr, sr);

            // Start the background execution engine
            if (ServiceRegistry.workerManager != null) {
                ServiceRegistry.workerManager.start();
            }

            System.out.println("Orchestrivo initialized successfully with background worker engine.");
        } catch (Exception e) {
            System.err.println("Fatal: Orchestrivo failed to initialize DataSource or background worker: " + e.getMessage());
            throw new RuntimeException("Orchestrivo critical startup failure: " + e.getMessage(), e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (ServiceRegistry.workerManager != null) {
            ServiceRegistry.workerManager.shutdown();
        }
        DataSourceManager.closeDataSource();
        System.out.println("Orchestrivo shut down cleanly.");
    }
}
