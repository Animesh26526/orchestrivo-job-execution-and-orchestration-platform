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
        // Initialize DataSource (will read from env or local config)
        DataSourceManager.getDataSource();
        
        UserRepositoryJdbcImpl ur = new UserRepositoryJdbcImpl();
        WorkloadRepositoryJdbcImpl wr = new WorkloadRepositoryJdbcImpl();
        ExperimentRunRepositoryJdbcImpl er = new ExperimentRunRepositoryJdbcImpl();

        ServiceRegistry.initialize(ur, wr, er);
        System.out.println("Orchestrivo initialized successfully.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        DataSourceManager.closeDataSource();
        System.out.println("Orchestrivo shutting down.");
    }
}
