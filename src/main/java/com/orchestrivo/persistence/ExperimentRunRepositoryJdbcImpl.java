package com.orchestrivo.persistence;

import com.orchestrivo.domain.AggregateMetrics;
import com.orchestrivo.domain.ExecutionSegment;
import com.orchestrivo.domain.ProcessMetrics;
import com.orchestrivo.domain.ScheduleResult;
import com.orchestrivo.model.ExperimentRunRecord;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExperimentRunRepositoryJdbcImpl implements ExperimentRunRepository {




    @Override
    public ExperimentRunRecord save(ExperimentRunRecord record) {
        boolean isUpdate = record.getId() > 0;
        
        String sqlRun;
        if (isUpdate) {
            sqlRun = "UPDATE experiment_runs SET owner_user_id=?, workload_id=?, algorithm_name=?, algorithm_parameters=?, input_snapshot=?, run_status=?, started_at=?, completed_at=?, validation_status=?, average_waiting_time=?, average_turnaround_time=?, average_response_time=?, simulated_throughput=?, context_switches=?, cpu_idle_time=?, total_duration=?, error_message=? WHERE id=?";
        } else {
            sqlRun = "INSERT INTO experiment_runs (owner_user_id, workload_id, algorithm_name, algorithm_parameters, input_snapshot, run_status, started_at, completed_at, validation_status, average_waiting_time, average_turnaround_time, average_response_time, simulated_throughput, context_switches, cpu_idle_time, total_duration, error_message) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        }

        Connection conn = null;
        try {
            conn = DataSourceManager.getDataSource().getConnection();
            conn.setAutoCommit(false);
            
            try (PreparedStatement pstmt = conn.prepareStatement(sqlRun, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setLong(1, record.getOwnerUserId());
                pstmt.setLong(2, record.getWorkloadId());
                pstmt.setString(3, record.getAlgorithmName());
                pstmt.setString(4, record.getAlgorithmParameters());
                pstmt.setString(5, record.getInputSnapshot());
                pstmt.setString(6, record.getRunStatus());
                
                pstmt.setTimestamp(7, record.getStartedAt() != null ? Timestamp.valueOf(record.getStartedAt()) : null);
                pstmt.setTimestamp(8, record.getCompletedAt() != null ? Timestamp.valueOf(record.getCompletedAt()) : null);
                
                pstmt.setString(9, record.getValidationStatus());
                
                ScheduleResult result = record.getResult();
                if (result != null && result.getAggregateMetrics() != null) {
                    AggregateMetrics am = result.getAggregateMetrics();
                    pstmt.setDouble(10, am.getAverageWaitingTime());
                    pstmt.setDouble(11, am.getAverageTurnaroundTime());
                    pstmt.setDouble(12, am.getAverageResponseTime());
                    pstmt.setDouble(13, am.getThroughput());
                    pstmt.setInt(14, am.getContextSwitchCount());
                    pstmt.setInt(15, am.getCpuIdleTime());
                    pstmt.setInt(16, am.getTotalDuration());
                } else {
                    pstmt.setNull(10, Types.DOUBLE);
                    pstmt.setNull(11, Types.DOUBLE);
                    pstmt.setNull(12, Types.DOUBLE);
                    pstmt.setNull(13, Types.DOUBLE);
                    pstmt.setNull(14, Types.INTEGER);
                    pstmt.setNull(15, Types.INTEGER);
                    pstmt.setNull(16, Types.INTEGER);
                }
                
                pstmt.setString(17, record.getErrorMessage());
                
                if (isUpdate) {
                    pstmt.setLong(18, record.getId());
                    pstmt.executeUpdate();
                    
                    try (PreparedStatement delRes = conn.prepareStatement("DELETE FROM process_results WHERE experiment_run_id = ?")) {
                        delRes.setLong(1, record.getId());
                        delRes.executeUpdate();
                    }
                    try (PreparedStatement delSeg = conn.prepareStatement("DELETE FROM execution_segments WHERE experiment_run_id = ?")) {
                        delSeg.setLong(1, record.getId());
                        delSeg.executeUpdate();
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
            
            ScheduleResult result = record.getResult();
            if (result != null) {
                if (result.getProcessMetrics() != null && !result.getProcessMetrics().isEmpty()) {
                    String sqlProcRes = "INSERT INTO process_results (experiment_run_id, process_key, first_start_time, completion_time, waiting_time, turnaround_time, response_time) VALUES (?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement pstmtProcRes = conn.prepareStatement(sqlProcRes)) {
                        for (ProcessMetrics pm : result.getProcessMetrics()) {
                            pstmtProcRes.setLong(1, record.getId());
                            pstmtProcRes.setString(2, pm.getProcessId());
                            pstmtProcRes.setInt(3, pm.getFirstStartTime());
                            pstmtProcRes.setInt(4, pm.getCompletionTime());
                            pstmtProcRes.setInt(5, pm.getWaitingTime());
                            pstmtProcRes.setInt(6, pm.getTurnaroundTime());
                            pstmtProcRes.setInt(7, pm.getResponseTime());
                            pstmtProcRes.addBatch();
                        }
                        pstmtProcRes.executeBatch();
                    }
                }
                
                if (result.getExecutionTimeline() != null && !result.getExecutionTimeline().isEmpty()) {
                    String sqlSeg = "INSERT INTO execution_segments (experiment_run_id, segment_sequence, process_key, start_time, end_time) VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement pstmtSeg = conn.prepareStatement(sqlSeg)) {
                        int seq = 1;
                        for (ExecutionSegment seg : result.getExecutionTimeline()) {
                            pstmtSeg.setLong(1, record.getId());
                            pstmtSeg.setInt(2, seq++);
                            pstmtSeg.setString(3, seg.getProcessId());
                            pstmtSeg.setInt(4, seg.getStartTime());
                            pstmtSeg.setInt(5, seg.getEndTime());
                            pstmtSeg.addBatch();
                        }
                        pstmtSeg.executeBatch();
                    }
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
            throw new RuntimeException("Error saving experiment run", e);
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
    public Optional<ExperimentRunRecord> findById(long runId) {
        String sql = "SELECT * FROM experiment_runs WHERE id = ?";
        try (Connection conn = DataSourceManager.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, runId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    ExperimentRunRecord record = mapRowToRunRecord(rs);
                    loadResult(conn, record);
                    return Optional.of(record);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding run by id", e);
        }
        return Optional.empty();
    }

    @Override
    public List<ExperimentRunRecord> findByWorkloadId(long workloadId) {
        String sql = "SELECT * FROM experiment_runs WHERE workload_id = ?";
        List<ExperimentRunRecord> records = new ArrayList<>();
        try (Connection conn = DataSourceManager.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, workloadId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ExperimentRunRecord record = mapRowToRunRecord(rs);
                    loadResult(conn, record);
                    records.add(record);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding runs by workload id", e);
        }
        return records;
    }

    @Override
    public List<ExperimentRunRecord> findByOwnerId(long ownerUserId) {
        String sql = "SELECT * FROM experiment_runs WHERE owner_user_id = ?";
        List<ExperimentRunRecord> records = new ArrayList<>();
        try (Connection conn = DataSourceManager.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, ownerUserId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ExperimentRunRecord record = mapRowToRunRecord(rs);
                    loadResult(conn, record);
                    records.add(record);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding runs by owner id", e);
        }
        return records;
    }

    @Override
    public void deleteById(long runId) {
        String sql = "DELETE FROM experiment_runs WHERE id = ?";
        try (Connection conn = DataSourceManager.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, runId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting run by id", e);
        }
    }
    
    @Override
    public List<ExperimentRunRecord> findWithFilter(long ownerUserId, Long workloadId, String algorithm, String sortBy, String sortDir, int offset, int limit) {
        String safeColumn = "created_at";
        if ("id".equalsIgnoreCase(sortBy)) safeColumn = "id";
        else if ("algorithm_name".equalsIgnoreCase(sortBy)) safeColumn = "algorithm_name";
        else if ("total_duration".equalsIgnoreCase(sortBy)) safeColumn = "total_duration";
        else if ("validation_status".equalsIgnoreCase(sortBy)) safeColumn = "validation_status";

        String safeDir = "ASC".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";

        int safeOffset = Math.max(0, offset);
        int safeLimit = Math.max(1, Math.min(100, limit));

        StringBuilder sql = new StringBuilder("SELECT * FROM experiment_runs WHERE owner_user_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(ownerUserId);

        if (workloadId != null && workloadId > 0) {
            sql.append(" AND workload_id = ?");
            params.add(workloadId);
        }
        if (algorithm != null && !algorithm.trim().isEmpty()) {
            sql.append(" AND algorithm_name = ?");
            params.add(algorithm.trim().toUpperCase());
        }

        sql.append(" ORDER BY ").append(safeColumn).append(" ").append(safeDir).append(", id DESC LIMIT ? OFFSET ?");
        params.add(safeLimit);
        params.add(safeOffset);

        List<ExperimentRunRecord> records = new ArrayList<>();
        try (Connection conn = DataSourceManager.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ExperimentRunRecord record = mapRowToRunRecord(rs);
                    loadResult(conn, record);
                    records.add(record);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding experiment runs with filter", e);
        }
        return records;
    }

    @Override
    public int countWithFilter(long ownerUserId, Long workloadId, String algorithm) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM experiment_runs WHERE owner_user_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(ownerUserId);

        if (workloadId != null && workloadId > 0) {
            sql.append(" AND workload_id = ?");
            params.add(workloadId);
        }
        if (algorithm != null && !algorithm.trim().isEmpty()) {
            sql.append(" AND algorithm_name = ?");
            params.add(algorithm.trim().toUpperCase());
        }

        try (Connection conn = DataSourceManager.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error counting experiment runs with filter", e);
        }
        return 0;
    }

    private ExperimentRunRecord mapRowToRunRecord(ResultSet rs) throws SQLException {
        ExperimentRunRecord record = new ExperimentRunRecord();
        record.setId(rs.getLong("id"));
        record.setOwnerUserId(rs.getLong("owner_user_id"));
        record.setWorkloadId(rs.getLong("workload_id"));
        record.setAlgorithmName(rs.getString("algorithm_name"));
        record.setAlgorithmParameters(rs.getString("algorithm_parameters"));
        record.setInputSnapshot(rs.getString("input_snapshot"));
        record.setRunStatus(rs.getString("run_status"));
        
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) record.setCreatedAt(created.toLocalDateTime());
        
        Timestamp started = rs.getTimestamp("started_at");
        if (started != null) record.setStartedAt(started.toLocalDateTime());
        
        Timestamp completed = rs.getTimestamp("completed_at");
        if (completed != null) record.setCompletedAt(completed.toLocalDateTime());
        
        record.setValidationStatus(rs.getString("validation_status"));
        record.setErrorMessage(rs.getString("error_message"));
        
        // AggregateMetrics is recreated if we have values
        return record;
    }
    
    private void loadResult(Connection conn, ExperimentRunRecord record) throws SQLException {
        // Load aggregate metrics directly from the run row. Since we already read it in mapRowToRunRecord,
        // let's do a fast query just for those fields or fetch them while in mapRowToRunRecord.
        // Actually, I can just fetch them here by reading the run row again, or better, pass the ResultSet to mapRowToRunRecord and extract am.
        // But the ResultSet might be closed. I'll re-query the run row metrics:
        
        AggregateMetrics am = null;
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT average_waiting_time, average_turnaround_time, average_response_time, total_duration, cpu_idle_time, simulated_throughput, context_switches FROM experiment_runs WHERE id = ?")) {
            pstmt.setLong(1, record.getId());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    rs.getDouble("average_waiting_time");
                    if (!rs.wasNull()) {
                        int totalDur = rs.getInt("total_duration");
                        int cpuIdle = rs.getInt("cpu_idle_time");
                        int cpuBusy = totalDur - cpuIdle;
                        am = new AggregateMetrics(
                            rs.getDouble("average_waiting_time"),
                            rs.getDouble("average_turnaround_time"),
                            rs.getDouble("average_response_time"),
                            totalDur,
                            cpuBusy,
                            cpuIdle,
                            rs.getDouble("simulated_throughput"),
                            rs.getInt("context_switches")
                        );
                    }
                }
            }
        }
        
        List<ProcessMetrics> processMetricsList = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM process_results WHERE experiment_run_id = ?")) {
            pstmt.setLong(1, record.getId());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    processMetricsList.add(new ProcessMetrics(
                        rs.getString("process_key"),
                        rs.getInt("first_start_time"),
                        rs.getInt("completion_time"),
                        rs.getInt("turnaround_time"),
                        rs.getInt("waiting_time"),
                        rs.getInt("response_time")
                    ));
                }
            }
        }
        
        List<ExecutionSegment> executionTimeline = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM execution_segments WHERE experiment_run_id = ? ORDER BY segment_sequence ASC")) {
            pstmt.setLong(1, record.getId());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    executionTimeline.add(new ExecutionSegment(
                        rs.getString("process_key"),
                        rs.getInt("start_time"),
                        rs.getInt("end_time")
                    ));
                }
            }
        }
        
        if (am != null || !processMetricsList.isEmpty() || !executionTimeline.isEmpty()) {
            record.setResult(new ScheduleResult(executionTimeline, processMetricsList, am, record.getAlgorithmName()));
        }
    }
}
