package com.orchestrivo.persistence;

import com.orchestrivo.domain.ProcessSpec;
import com.orchestrivo.domain.Workload;
import com.orchestrivo.model.WorkloadRecord;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WorkloadRepositoryJdbcImpl implements WorkloadRepository {




    @Override
    public WorkloadRecord save(WorkloadRecord record) {
        boolean isUpdate = record.getId() > 0;
        
        String sqlWorkload;
        if (isUpdate) {
            sqlWorkload = "UPDATE workloads SET owner_user_id = ?, name = ?, description = ?, source_type = ?, generation_seed = ?, generation_config = ? WHERE id = ?";
        } else {
            sqlWorkload = "INSERT INTO workloads (owner_user_id, name, description, source_type, generation_seed, generation_config) VALUES (?, ?, ?, ?, ?, ?)";
        }

        Connection conn = null;
        try {
            conn = DataSourceManager.getDataSource().getConnection();
            conn.setAutoCommit(false);
            
            try (PreparedStatement pstmt = conn.prepareStatement(sqlWorkload, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setLong(1, record.getOwnerUserId());
                pstmt.setString(2, record.getName());
                pstmt.setString(3, record.getDescription());
                pstmt.setString(4, record.getSourceType());
                
                if (record.getGenerationSeed() != null) {
                    pstmt.setLong(5, record.getGenerationSeed());
                } else {
                    pstmt.setNull(5, Types.BIGINT);
                }
                
                pstmt.setString(6, record.getGenerationConfig());
                
                if (isUpdate) {
                    pstmt.setLong(7, record.getId());
                    pstmt.executeUpdate();
                    
                    // delete old processes
                    try (PreparedStatement deletePstmt = conn.prepareStatement("DELETE FROM processes WHERE workload_id = ?")) {
                        deletePstmt.setLong(1, record.getId());
                        deletePstmt.executeUpdate();
                    }
                } else {
                    pstmt.executeUpdate();
                    try (ResultSet rs = pstmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            record.setId(rs.getLong(1));
                        }
                    }
                }
            }
            
            if (record.getWorkload() != null && !record.getWorkload().isEmpty()) {
                String sqlProcess = "INSERT INTO processes (workload_id, process_key, arrival_time, burst_time) VALUES (?, ?, ?, ?)";
                try (PreparedStatement pstmtProcess = conn.prepareStatement(sqlProcess)) {
                    for (ProcessSpec p : record.getWorkload().getProcesses()) {
                        pstmtProcess.setLong(1, record.getId());
                        pstmtProcess.setString(2, p.getProcessId());
                        pstmtProcess.setInt(3, p.getArrivalTime());
                        pstmtProcess.setInt(4, p.getBurstTime());
                        pstmtProcess.addBatch();
                    }
                    pstmtProcess.executeBatch();
                }
            }
            
            conn.commit();
            return findById(record.getId()).orElse(record);
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    e.addSuppressed(ex);
                }
            }
            throw new RuntimeException("Error saving workload", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    // ignore
                }
            }
        }
    }

    @Override
    public Optional<WorkloadRecord> findById(long workloadId) {
        String sql = "SELECT * FROM workloads WHERE id = ?";
        try (Connection conn = DataSourceManager.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, workloadId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    WorkloadRecord record = mapRowToWorkloadRecord(rs);
                    loadProcesses(conn, record);
                    return Optional.of(record);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding workload by id", e);
        }
        return Optional.empty();
    }

    @Override
    public List<WorkloadRecord> findByOwnerId(long ownerUserId) {
        String sql = "SELECT * FROM workloads WHERE owner_user_id = ?";
        List<WorkloadRecord> records = new ArrayList<>();
        try (Connection conn = DataSourceManager.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, ownerUserId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    WorkloadRecord record = mapRowToWorkloadRecord(rs);
                    loadProcesses(conn, record);
                    records.add(record);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding workloads by owner id", e);
        }
        return records;
    }

    @Override
    public WorkloadRecord update(WorkloadRecord record) {
        if (record.getId() <= 0) {
            throw new IllegalArgumentException("Cannot update workload without positive ID");
        }
        return save(record);
    }

    @Override
    public List<WorkloadRecord> findWithFilter(long ownerUserId, String search, String sortBy, String sortDir, int offset, int limit) {
        String safeColumn = "created_at";
        if ("name".equalsIgnoreCase(sortBy)) safeColumn = "name";
        else if ("id".equalsIgnoreCase(sortBy)) safeColumn = "id";
        else if ("updated_at".equalsIgnoreCase(sortBy)) safeColumn = "updated_at";

        String safeDir = "ASC".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";

        int safeOffset = Math.max(0, offset);
        int safeLimit = Math.max(1, Math.min(100, limit));

        boolean hasSearch = search != null && !search.trim().isEmpty();
        String sql = "SELECT * FROM workloads WHERE owner_user_id = ?"
                + (hasSearch ? " AND LOWER(name) LIKE ?" : "")
                + " ORDER BY " + safeColumn + " " + safeDir + ", id DESC LIMIT ? OFFSET ?";

        List<WorkloadRecord> records = new ArrayList<>();
        try (Connection conn = DataSourceManager.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            int idx = 1;
            pstmt.setLong(idx++, ownerUserId);
            if (hasSearch) {
                pstmt.setString(idx++, "%" + search.trim().toLowerCase() + "%");
            }
            pstmt.setInt(idx++, safeLimit);
            pstmt.setInt(idx++, safeOffset);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    WorkloadRecord record = mapRowToWorkloadRecord(rs);
                    loadProcesses(conn, record);
                    records.add(record);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding workloads with filter", e);
        }
        return records;
    }

    @Override
    public int countWithFilter(long ownerUserId, String search) {
        boolean hasSearch = search != null && !search.trim().isEmpty();
        String sql = "SELECT COUNT(*) FROM workloads WHERE owner_user_id = ?"
                + (hasSearch ? " AND LOWER(name) LIKE ?" : "");

        try (Connection conn = DataSourceManager.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, ownerUserId);
            if (hasSearch) {
                pstmt.setString(2, "%" + search.trim().toLowerCase() + "%");
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error counting workloads with filter", e);
        }
        return 0;
    }

    @Override
    public void deleteById(long workloadId) {
        String sql = "DELETE FROM workloads WHERE id = ?";
        try (Connection conn = DataSourceManager.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, workloadId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting workload by id", e);
        }
    }
    
    private void loadProcesses(Connection conn, WorkloadRecord record) throws SQLException {
        String sql = "SELECT * FROM processes WHERE workload_id = ?";
        List<ProcessSpec> processes = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, record.getId());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    processes.add(new ProcessSpec(
                        rs.getString("process_key"),
                        rs.getInt("arrival_time"),
                        rs.getInt("burst_time")
                    ));
                }
            }
        }
        if (!processes.isEmpty()) {
            record.setWorkload(new Workload(processes));
        } else {
            record.setWorkload(new Workload(new ArrayList<>()));
        }
    }
    
    private WorkloadRecord mapRowToWorkloadRecord(ResultSet rs) throws SQLException {
        WorkloadRecord record = new WorkloadRecord();
        record.setId(rs.getLong("id"));
        record.setOwnerUserId(rs.getLong("owner_user_id"));
        record.setName(rs.getString("name"));
        record.setDescription(rs.getString("description"));
        record.setSourceType(rs.getString("source_type"));
        
        long seed = rs.getLong("generation_seed");
        if (!rs.wasNull()) {
            record.setGenerationSeed(seed);
        }
        
        record.setGenerationConfig(rs.getString("generation_config"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            record.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            record.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return record;
    }
}
