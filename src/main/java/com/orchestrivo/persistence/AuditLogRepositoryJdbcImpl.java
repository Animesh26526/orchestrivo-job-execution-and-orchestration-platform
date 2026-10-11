package com.orchestrivo.persistence;

import com.orchestrivo.model.AuditLogRecord;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuditLogRepositoryJdbcImpl implements AuditLogRepository {

    @Override
    public void save(AuditLogRecord record) {
        String sql = "INSERT INTO audit_logs (actor_user_id, event_type, resource_type, resource_id, metadata) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DataSourceManager.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (record.getActorUserId() != null) {
                pstmt.setLong(1, record.getActorUserId());
            } else {
                pstmt.setNull(1, Types.BIGINT);
            }
            pstmt.setString(2, record.getEventType());
            pstmt.setString(3, record.getResourceType());
            pstmt.setString(4, record.getResourceId());
            pstmt.setString(5, record.getMetadata());

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    record.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error persisting audit log", e);
        }
    }

    @Override
    public List<AuditLogRecord> findByActorId(long actorUserId, int limit) {
        int safeLimit = Math.max(1, Math.min(100, limit));
        String sql = "SELECT * FROM audit_logs WHERE actor_user_id = ? ORDER BY timestamp DESC, id DESC LIMIT ?";
        List<AuditLogRecord> list = new ArrayList<>();
        try (Connection conn = DataSourceManager.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, actorUserId);
            pstmt.setInt(2, safeLimit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error querying audit logs by actor", e);
        }
        return list;
    }

    @Override
    public List<AuditLogRecord> findRecent(int limit) {
        int safeLimit = Math.max(1, Math.min(100, limit));
        String sql = "SELECT * FROM audit_logs ORDER BY timestamp DESC, id DESC LIMIT ?";
        List<AuditLogRecord> list = new ArrayList<>();
        try (Connection conn = DataSourceManager.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, safeLimit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error querying recent audit logs", e);
        }
        return list;
    }

    private AuditLogRecord mapRow(ResultSet rs) throws SQLException {
        AuditLogRecord record = new AuditLogRecord();
        record.setId(rs.getLong("id"));
        long actor = rs.getLong("actor_user_id");
        if (!rs.wasNull()) {
            record.setActorUserId(actor);
        }
        record.setEventType(rs.getString("event_type"));
        record.setResourceType(rs.getString("resource_type"));
        record.setResourceId(rs.getString("resource_id"));
        Timestamp ts = rs.getTimestamp("timestamp");
        if (ts != null) {
            record.setTimestamp(ts.toLocalDateTime());
        }
        record.setMetadata(rs.getString("metadata"));
        return record;
    }
}
