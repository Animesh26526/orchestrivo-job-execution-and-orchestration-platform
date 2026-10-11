package com.orchestrivo.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class DataSourceManager {

    private static HikariDataSource dataSource;

    private DataSourceManager() {}

    public static synchronized DataSource getDataSource() {
        if (dataSource == null) {
            HikariConfig config = new HikariConfig();
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            
            String url = getEnvOrProperty("JDBC_URL");
            if (url == null || url.trim().isEmpty()) {
                throw new IllegalStateException("JDBC_URL environment variable is required.");
            }
            config.setJdbcUrl(url);

            String user = getEnvOrProperty("JDBC_USER");
            if (user == null || user.trim().isEmpty()) {
                throw new IllegalStateException("JDBC_USER environment variable is required. (Do not use root in production)");
            }
            config.setUsername(user);

            String password = getEnvOrProperty("JDBC_PASSWORD");
            if (password == null || password.trim().isEmpty()) {
                throw new IllegalStateException("JDBC_PASSWORD environment variable is required.");
            }
            config.setPassword(password);

            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            
            dataSource = new HikariDataSource(config);
        }
        return dataSource;
    }

    public static synchronized void closeDataSource() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }

    private static String getEnvOrProperty(String key) {
        String val = System.getenv(key);
        if (val != null && !val.trim().isEmpty()) {
            return val.trim();
        }
        val = System.getProperty(key);
        if (val != null && !val.trim().isEmpty()) {
            return val.trim();
        }
        return null;
    }
}
