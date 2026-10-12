package com.orchestrivo.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class SystemSettingsRepositoryJdbcImpl implements SystemSettingsRepository {

    private static volatile boolean tableEnsured = false;

    private void ensureTable(Connection conn) {
        if (tableEnsured) return;
        synchronized (SystemSettingsRepositoryJdbcImpl.class) {
            if (tableEnsured) return;
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS system_settings (" +
                        "setting_key VARCHAR(64) NOT NULL PRIMARY KEY, " +
                        "setting_value VARCHAR(255) NOT NULL, " +
                        "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)");
                tableEnsured = true;
            } catch (SQLException ignored) {
                tableEnsured = true;
            }
        }
    }

    @Override
    public Optional<String> getSetting(String key) {
        if (key == null || key.trim().isEmpty()) return Optional.empty();
        String sql = "SELECT setting_value FROM system_settings WHERE setting_key = ?";
        try (Connection conn = DataSourceManager.getDataSource().getConnection()) {
            ensureTable(conn);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, key.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.ofNullable(rs.getString(1));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Warning: Error fetching system setting " + key + ": " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public void setSetting(String key, String value) {
        if (key == null || key.trim().isEmpty()) return;
        String sql = "INSERT INTO system_settings (setting_key, setting_value) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), updated_at = NOW()";
        try (Connection conn = DataSourceManager.getDataSource().getConnection()) {
            ensureTable(conn);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, key.trim());
                ps.setString(2, value != null ? value : "");
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error persisting system setting: " + key, e);
        }
    }

    @Override
    public void removeSetting(String key) {
        if (key == null || key.trim().isEmpty()) return;
        String sql = "DELETE FROM system_settings WHERE setting_key = ?";
        try (Connection conn = DataSourceManager.getDataSource().getConnection()) {
            ensureTable(conn);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, key.trim());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting system setting: " + key, e);
        }
    }

    @Override
    public Optional<Long> getLongSetting(String key) {
        return getSetting(key).flatMap(v -> {
            try {
                return Optional.of(Long.parseLong(v.trim()));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        });
    }

    @Override
    public void setLongSetting(String key, long value) {
        setSetting(key, String.valueOf(value));
    }
}
