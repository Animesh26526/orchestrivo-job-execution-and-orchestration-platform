package com.orchestrivo.persistence;

import com.orchestrivo.model.BenchmarkMeasurement;
import com.orchestrivo.model.BenchmarkRunRecord;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BenchmarkRepositoryJdbcImpl implements BenchmarkRepository {

    private static volatile boolean schemaEnsured = false;

    private void ensureSchema(Connection conn) {
        if (schemaEnsured) return;
        synchronized (BenchmarkRepositoryJdbcImpl.class) {
            if (schemaEnsured) return;
            try (Statement st = conn.createStatement()) {
                try {
                    st.execute("ALTER TABLE benchmark_runs ADD COLUMN run_type VARCHAR(50) NOT NULL DEFAULT 'STANDARD'");
                } catch (SQLException ignored) {}
                try {
                    st.execute("ALTER TABLE benchmark_runs ADD COLUMN benchmark_group VARCHAR(100) NULL");
                } catch (SQLException ignored) {}
                schemaEnsured = true;
            } catch (SQLException ignored) {
                schemaEnsured = true;
            }
        }
    }

    @Override
    public BenchmarkRunRecord save(BenchmarkRunRecord record) {
        String sqlRun = "INSERT INTO benchmark_runs (owner_user_id, algorithm_name, process_count, seed, warmup_iterations, measured_iterations, median_nanos, min_nanos, max_nanos, std_dev_nanos, validation_passed, environment_info, run_type, benchmark_group) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DataSourceManager.getDataSource().getConnection();
            ensureSchema(conn);
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(sqlRun, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setLong(1, record.getOwnerUserId());
                pstmt.setString(2, record.getAlgorithmName());
                pstmt.setInt(3, record.getProcessCount());
                pstmt.setLong(4, record.getSeed());
                pstmt.setInt(5, record.getWarmupIterations());
                pstmt.setInt(6, record.getMeasuredIterations());
                pstmt.setDouble(7, record.getMedianNanos());
                pstmt.setLong(8, record.getMinNanos());
                pstmt.setLong(9, record.getMaxNanos());
                pstmt.setDouble(10, record.getStdDevNanos());
                pstmt.setBoolean(11, record.isValidationPassed());
                pstmt.setString(12, record.getEnvironmentInfo());
                pstmt.setString(13, record.getRunType());
                pstmt.setString(14, record.getBenchmarkGroup());

                pstmt.executeUpdate();

                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        record.setId(rs.getLong(1));
                    }
                }
            }

            if (record.getMeasurements() != null && !record.getMeasurements().isEmpty()) {
                String sqlMeas = "INSERT INTO benchmark_measurements (benchmark_run_id, iteration_number, duration_nanos) VALUES (?, ?, ?)";
                try (PreparedStatement pstmtMeas = conn.prepareStatement(sqlMeas)) {
                    for (BenchmarkMeasurement bm : record.getMeasurements()) {
                        pstmtMeas.setLong(1, record.getId());
                        pstmtMeas.setInt(2, bm.getIterationNumber());
                        pstmtMeas.setLong(3, bm.getDurationNanos());
                        pstmtMeas.addBatch();
                    }
                    pstmtMeas.executeBatch();
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
            throw new RuntimeException("Error saving benchmark run", e);
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
    public Optional<BenchmarkRunRecord> findById(long id) {
        String sql = "SELECT * FROM benchmark_runs WHERE id = ?";
        try (Connection conn = DataSourceManager.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    BenchmarkRunRecord record = mapRow(rs);
                    loadMeasurements(conn, record);
                    return Optional.of(record);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding benchmark run by id", e);
        }
        return Optional.empty();
    }

    @Override
    public List<BenchmarkRunRecord> findByOwnerId(long ownerUserId) {
        String sql = "SELECT * FROM benchmark_runs WHERE owner_user_id = ? ORDER BY created_at DESC, id DESC";
        List<BenchmarkRunRecord> list = new ArrayList<>();
        try (Connection conn = DataSourceManager.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, ownerUserId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    BenchmarkRunRecord record = mapRow(rs);
                    loadMeasurements(conn, record);
                    list.add(record);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding benchmark runs by owner id", e);
        }
        return list;
    }

    @Override
    public List<BenchmarkRunRecord> findWithFilter(long ownerUserId, int offset, int limit) {
        int safeOffset = Math.max(0, offset);
        int safeLimit = Math.max(1, Math.min(100, limit));
        String sql = "SELECT * FROM benchmark_runs WHERE owner_user_id = ? ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?";
        List<BenchmarkRunRecord> list = new ArrayList<>();
        try (Connection conn = DataSourceManager.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, ownerUserId);
            pstmt.setInt(2, safeLimit);
            pstmt.setInt(3, safeOffset);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    BenchmarkRunRecord record = mapRow(rs);
                    loadMeasurements(conn, record);
                    list.add(record);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding benchmark runs with filter", e);
        }
        return list;
    }

    @Override
    public int countByOwnerId(long ownerUserId) {
        String sql = "SELECT COUNT(*) FROM benchmark_runs WHERE owner_user_id = ?";
        try (Connection conn = DataSourceManager.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, ownerUserId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error counting benchmark runs", e);
        }
        return 0;
    }

    @Override
    public void deleteById(long id) {
        String sql = "DELETE FROM benchmark_runs WHERE id = ?";
        try (Connection conn = DataSourceManager.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting benchmark run", e);
        }
    }

    private BenchmarkRunRecord mapRow(ResultSet rs) throws SQLException {
        BenchmarkRunRecord record = new BenchmarkRunRecord();
        record.setId(rs.getLong("id"));
        record.setOwnerUserId(rs.getLong("owner_user_id"));
        record.setAlgorithmName(rs.getString("algorithm_name"));
        record.setProcessCount(rs.getInt("process_count"));
        record.setSeed(rs.getLong("seed"));
        record.setWarmupIterations(rs.getInt("warmup_iterations"));
        record.setMeasuredIterations(rs.getInt("measured_iterations"));
        record.setMedianNanos(rs.getDouble("median_nanos"));
        record.setMinNanos(rs.getLong("min_nanos"));
        record.setMaxNanos(rs.getLong("max_nanos"));
        record.setStdDevNanos(rs.getDouble("std_dev_nanos"));
        record.setValidationPassed(rs.getBoolean("validation_passed"));
        record.setEnvironmentInfo(rs.getString("environment_info"));

        try {
            String rt = rs.getString("run_type");
            record.setRunType(rt != null ? rt : "STANDARD");
        } catch (SQLException ignored) {
            record.setRunType("STANDARD");
        }

        try {
            record.setBenchmarkGroup(rs.getString("benchmark_group"));
        } catch (SQLException ignored) {}

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            record.setCreatedAt(ts.toLocalDateTime());
        }
        return record;
    }

    @Override
    public List<BenchmarkRunRecord> findByRunType(long ownerUserId, String runType) {
        String sql = "SELECT * FROM benchmark_runs WHERE owner_user_id = ? AND run_type = ? ORDER BY created_at DESC, id DESC";
        List<BenchmarkRunRecord> list = new ArrayList<>();
        try (Connection conn = DataSourceManager.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, ownerUserId);
            pstmt.setString(2, runType != null ? runType : "STANDARD");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    BenchmarkRunRecord record = mapRow(rs);
                    loadMeasurements(conn, record);
                    list.add(record);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding benchmark runs by run type", e);
        }
        return list;
    }

    @Override
    public List<BenchmarkRunRecord> findByBenchmarkGroup(long ownerUserId, String benchmarkGroup) {
        String sql = "SELECT * FROM benchmark_runs WHERE owner_user_id = ? AND benchmark_group = ? ORDER BY created_at DESC, id DESC";
        List<BenchmarkRunRecord> list = new ArrayList<>();
        try (Connection conn = DataSourceManager.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, ownerUserId);
            pstmt.setString(2, benchmarkGroup);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    BenchmarkRunRecord record = mapRow(rs);
                    loadMeasurements(conn, record);
                    list.add(record);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding benchmark runs by benchmark group", e);
        }
        return list;
    }

    private void loadMeasurements(Connection conn, BenchmarkRunRecord record) throws SQLException {
        String sql = "SELECT * FROM benchmark_measurements WHERE benchmark_run_id = ? ORDER BY iteration_number ASC";
        List<BenchmarkMeasurement> measurements = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, record.getId());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    BenchmarkMeasurement m = new BenchmarkMeasurement();
                    m.setId(rs.getLong("id"));
                    m.setBenchmarkRunId(rs.getLong("benchmark_run_id"));
                    m.setIterationNumber(rs.getInt("iteration_number"));
                    m.setDurationNanos(rs.getLong("duration_nanos"));
                    measurements.add(m);
                }
            }
        }
        record.setMeasurements(measurements);
    }
}
