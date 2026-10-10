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
            
            String url = System.getenv("JDBC_URL");
            if (url == null || url.trim().isEmpty()) {
                url = "jdbc:mysql://localhost:3306/orchestrivo?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            }
            config.setJdbcUrl(url);

            String user = System.getenv("JDBC_USER");
            if (user == null || user.trim().isEmpty()) {
                user = "root";
            }
            config.setUsername(user);

            String password = System.getenv("JDBC_PASSWORD");
            if (password == null || password.trim().isEmpty()) {
                password = "password";
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
}
