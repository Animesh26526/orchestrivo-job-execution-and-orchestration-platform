package com.orchestrivo.persistence;

import com.orchestrivo.model.ExecutionJobRecord;
import com.orchestrivo.model.ExecutionJobStatus;
import com.orchestrivo.model.PagedResult;
import com.orchestrivo.model.QueueMetrics;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class ExecutionJobRepositoryJdbcImpl implements ExecutionJobRepository {

    private final DataSource dataSource;

    public ExecutionJobRepositoryJdbcImpl() {
        this(DataSourceManager.getDataSource());
    }

    public ExecutionJobRepositoryJdbcImpl(DataSource dataSource) {
        this.dataSource = dataSource;
        ensureSchema(this.dataSource);
    }

    private static synchronized void ensureSchema(DataSource ds) {
        if (ds == null) return;
        try (Connection conn = ds.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            boolean hasClaimToken = false;
            try (ResultSet rs = meta.getColumns(null, null, "execution_jobs", "claim_token")) {
                if (rs.next()) {
                    hasClaimToken = true;
                }
            }
            if (!hasClaimToken) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE execution_jobs ADD COLUMN claim_token VARCHAR(64) NULL, ADD COLUMN lease_expires_at TIMESTAMP NULL");
                    try {
                        stmt.executeUpdate("CREATE INDEX idx_jobs_lease_expires ON execution_jobs(lease_expires_at)");
                    } catch (SQLException ignored) {}
                    try {
                        stmt.executeUpdate("CREATE INDEX idx_jobs_claim_token ON execution_jobs(claim_token)");
                    } catch (SQLException ignored) {}
                }
            }
            boolean hasBatchId = false;
            try (ResultSet rs = meta.getColumns(null, null, "execution_jobs", "batch_id")) {
                if (rs.next()) {
                    hasBatchId = true;
                }
            }
            if (!hasBatchId) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE execution_jobs ADD COLUMN execution_duration_nanos BIGINT NULL AFTER execution_duration_ms, ADD COLUMN batch_id VARCHAR(64) NULL AFTER idempotency_key");
                    try {
                        stmt.executeUpdate("CREATE INDEX idx_jobs_batch ON execution_jobs(batch_id)");
                    } catch (SQLException ignored) {}
                } catch (SQLException ignored) {}
            }
        } catch (Exception ignored) {
            // DataSource may not yet be initialized or table might not exist in isolated test contexts
        }
    }

    @Override
    public ExecutionJobRecord save(ExecutionJobRecord job) {
        String sql = "INSERT INTO execution_jobs " +
                "(owner_user_id, workload_id, algorithm_name, algorithm_parameters, input_snapshot, " +
                "status, attempt_count, max_attempts, claimed_by, idempotency_key, batch_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, job.getOwnerUserId());
            ps.setLong(2, job.getWorkloadId());
            ps.setString(3, job.getAlgorithmName());
            ps.setString(4, job.getAlgorithmParameters());
            ps.setString(5, job.getInputSnapshot());
            ps.setString(6, job.getStatus() != null ? job.getStatus().name() : ExecutionJobStatus.QUEUED.name());
            ps.setInt(7, job.getAttemptCount());
            ps.setInt(8, job.getMaxAttempts() > 0 ? job.getMaxAttempts() : 3);
            ps.setString(9, job.getClaimedBy());
            ps.setString(10, job.getIdempotencyKey());
            ps.setString(11, job.getBatchId());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    job.setId(rs.getLong(1));
                }
            }
            job.setCreatedAt(LocalDateTime.now());
            return job;
        } catch (SQLException e) {
            throw new RuntimeException("Error saving execution job", e);
        }
    }

    @Override
    public List<ExecutionJobRecord> saveAll(List<ExecutionJobRecord> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            return Collections.emptyList();
        }
        String sql = "INSERT INTO execution_jobs " +
                "(owner_user_id, workload_id, algorithm_name, algorithm_parameters, input_snapshot, " +
                "status, attempt_count, max_attempts, claimed_by, idempotency_key, batch_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                for (ExecutionJobRecord job : jobs) {
                    ps.setLong(1, job.getOwnerUserId());
                    ps.setLong(2, job.getWorkloadId());
                    ps.setString(3, job.getAlgorithmName());
                    ps.setString(4, job.getAlgorithmParameters());
                    ps.setString(5, job.getInputSnapshot());
                    ps.setString(6, job.getStatus() != null ? job.getStatus().name() : ExecutionJobStatus.QUEUED.name());
                    ps.setInt(7, job.getAttemptCount());
                    ps.setInt(8, job.getMaxAttempts() > 0 ? job.getMaxAttempts() : 3);
                    ps.setString(9, job.getClaimedBy());
                    ps.setString(10, job.getIdempotencyKey());
                    ps.setString(11, job.getBatchId());
                    ps.addBatch();
                }
                ps.executeBatch();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    int idx = 0;
                    LocalDateTime now = LocalDateTime.now();
                    while (rs.next() && idx < jobs.size()) {
                        ExecutionJobRecord j = jobs.get(idx++);
                        j.setId(rs.getLong(1));
                        j.setCreatedAt(now);
                    }
                }
            }
            conn.commit();
            return jobs;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw new RuntimeException("Error batch saving execution jobs", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    @Override
    public Optional<ExecutionJobRecord> findById(long id) {
        String sql = "SELECT j.*, w.name AS workload_name " +
                "FROM execution_jobs j " +
                "LEFT JOIN workloads w ON j.workload_id = w.id " +
                "WHERE j.id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Error finding execution job by id: " + id, e);
        }
    }

    @Override
    public Optional<ExecutionJobRecord> findByIdAndOwner(long id, long userId) {
        String sql = "SELECT j.*, w.name AS workload_name " +
                "FROM execution_jobs j " +
                "LEFT JOIN workloads w ON j.workload_id = w.id " +
                "WHERE j.id = ? AND j.owner_user_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Error finding execution job by id and owner", e);
        }
    }

    @Override
    public Optional<ExecutionJobRecord> findByIdempotencyKey(long userId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return Optional.empty();
        }
        String sql = "SELECT j.*, w.name AS workload_name " +
                "FROM execution_jobs j " +
                "LEFT JOIN workloads w ON j.workload_id = w.id " +
                "WHERE j.owner_user_id = ? AND j.idempotency_key = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setString(2, idempotencyKey.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Error finding execution job by idempotency key", e);
        }
    }

    @Override
    public PagedResult<ExecutionJobRecord> findByOwnerPaged(long userId, String statusFilter, String sortBy, String sortDir, int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, Math.min(50, pageSize));
        int offset = (safePage - 1) * safePageSize;

        Set<String> allowedSortCols = Set.of("created_at", "id", "status", "execution_duration_ms");
        String safeSort = (sortBy != null && allowedSortCols.contains(sortBy.toLowerCase())) ? sortBy.toLowerCase() : "created_at";
        String safeDir = "ASC".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";

        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM execution_jobs WHERE owner_user_id = ?");
        StringBuilder querySql = new StringBuilder("SELECT j.*, w.name AS workload_name FROM execution_jobs j LEFT JOIN workloads w ON j.workload_id = w.id WHERE j.owner_user_id = ?");

        boolean hasStatus = (statusFilter != null && !statusFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusFilter));
        if (hasStatus) {
            countSql.append(" AND status = ?");
            querySql.append(" AND j.status = ?");
        }

        querySql.append(" ORDER BY j.").append(safeSort).append(" ").append(safeDir).append(" LIMIT ? OFFSET ?");

        int totalCount = 0;
        List<ExecutionJobRecord> list = new ArrayList<>();

        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement psCount = conn.prepareStatement(countSql.toString())) {
                psCount.setLong(1, userId);
                if (hasStatus) {
                    psCount.setString(2, statusFilter.trim().toUpperCase());
                }
                try (ResultSet rs = psCount.executeQuery()) {
                    if (rs.next()) totalCount = rs.getInt(1);
                }
            }

            try (PreparedStatement psQuery = conn.prepareStatement(querySql.toString())) {
                int paramIdx = 1;
                psQuery.setLong(paramIdx++, userId);
                if (hasStatus) {
                    psQuery.setString(paramIdx++, statusFilter.trim().toUpperCase());
                }
                psQuery.setInt(paramIdx++, safePageSize);
                psQuery.setInt(paramIdx, offset);

                try (ResultSet rs = psQuery.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapRow(rs));
                    }
                }
            }

            return new PagedResult<>(list, safePage, safePageSize, totalCount);
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching paged execution jobs", e);
        }
    }

    @Override
    public List<ExecutionJobRecord> findRecentByOwner(long userId, int limit) {
        int safeLimit = Math.max(1, Math.min(20, limit));
        String sql = "SELECT j.*, w.name AS workload_name " +
                "FROM execution_jobs j " +
                "LEFT JOIN workloads w ON j.workload_id = w.id " +
                "WHERE j.owner_user_id = ? " +
                "ORDER BY j.created_at DESC " +
                "LIMIT ?";

        List<ExecutionJobRecord> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setInt(2, safeLimit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Error finding recent execution jobs", e);
        }
    }

    @Override
    public Optional<ExecutionJobRecord> claimNextQueuedJob(String workerId) {
        return claimNextQueuedJob(workerId, 30);
    }

    @Override
    public Optional<ExecutionJobRecord> claimNextQueuedJob(String workerId, int leaseDurationSeconds) {
        int safeDuration = Math.max(5, leaseDurationSeconds);
        String selectSql = "SELECT * FROM execution_jobs USE INDEX (idx_jobs_status_id) " +
                "WHERE status = 'QUEUED' " +
                "ORDER BY id ASC " +
                "LIMIT 1 " +
                "FOR UPDATE SKIP LOCKED";

        String updateSql = "UPDATE execution_jobs " +
                "SET status = 'RUNNING', claimed_by = ?, claim_token = ?, claimed_at = NOW(), " +
                "lease_expires_at = DATE_ADD(NOW(), INTERVAL ? SECOND), " +
                "started_at = IFNULL(started_at, NOW()), " +
                "queue_wait_ms = IFNULL(queue_wait_ms, TIMESTAMPDIFF(MICROSECOND, created_at, NOW()) / 1000), " +
                "attempt_count = attempt_count + 1 " +
                "WHERE id = ? AND status = 'QUEUED'";

        int maxRetries = 3;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            Connection conn = null;
            Integer prevIsolation = null;
            try {
                conn = dataSource.getConnection();
                prevIsolation = conn.getTransactionIsolation();
                conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                conn.setAutoCommit(false);

                ExecutionJobRecord job = null;
                long candidateId = -1;

                try (PreparedStatement psSelect = conn.prepareStatement(selectSql);
                     ResultSet rs = psSelect.executeQuery()) {
                    if (rs.next()) {
                        job = mapRow(rs);
                        candidateId = job.getId();
                    }
                }

                if (job == null || candidateId <= 0) {
                    conn.commit();
                    return Optional.empty();
                }

                String token = UUID.randomUUID().toString();
                try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                    psUpdate.setString(1, workerId);
                    psUpdate.setString(2, token);
                    psUpdate.setInt(3, safeDuration);
                    psUpdate.setLong(4, candidateId);
                    int updated = psUpdate.executeUpdate();
                    if (updated == 1) {
                        conn.commit();
                        job.setStatus(ExecutionJobStatus.RUNNING);
                        job.setClaimedBy(workerId);
                        job.setClaimToken(token);
                        job.setAttemptCount(job.getAttemptCount() + 1);
                        LocalDateTime now = LocalDateTime.now();
                        job.setClaimedAt(now);
                        job.setLeaseExpiresAt(now.plusSeconds(safeDuration));
                        if (job.getStartedAt() == null) {
                            job.setStartedAt(now);
                        }
                        if (job.getCreatedAt() != null && job.getQueueWaitMs() == null) {
                            long waitMs = java.time.Duration.between(job.getCreatedAt(), now).toMillis();
                            job.setQueueWaitMs(Math.max(0L, waitMs));
                        }
                        return Optional.of(job);
                    } else {
                        conn.rollback();
                        return Optional.empty();
                    }
                }
            } catch (SQLException e) {
                if (conn != null) {
                    try { conn.rollback(); } catch (SQLException ignored) {}
                }
                boolean isDeadlock = e.getErrorCode() == 1213 || "40001".equals(e.getSQLState())
                        || (e.getMessage() != null && e.getMessage().toLowerCase().contains("deadlock"));
                if (isDeadlock && attempt < maxRetries - 1) {
                    try {
                        Thread.sleep(10 + (long)(Math.random() * 20));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during claim retry", ie);
                    }
                    continue;
                }
                throw new RuntimeException("Error atomically claiming queued job", e);
            } finally {
                if (conn != null) {
                    try {
                        if (prevIsolation != null) {
                            conn.setTransactionIsolation(prevIsolation);
                        }
                        conn.setAutoCommit(true);
                        conn.close();
                    } catch (SQLException ignored) {}
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean renewLease(long jobId, String claimToken, int durationSeconds) {
        if (claimToken == null || claimToken.trim().isEmpty()) {
            return false;
        }
        int safeDuration = Math.max(5, durationSeconds);
        String sql = "UPDATE execution_jobs " +
                "SET lease_expires_at = DATE_ADD(NOW(), INTERVAL ? SECOND) " +
                "WHERE id = ? AND status = 'RUNNING' AND claim_token = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, safeDuration);
            ps.setLong(2, jobId);
            ps.setString(3, claimToken.trim());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("Error renewing job lease for job: " + jobId, e);
        }
    }

    @Override
    public boolean markCompleted(long jobId, long experimentRunId, long durationMs) {
        // Strict fencing requires the active attempt claim token; unfenced completions are rejected.
        return false;
    }

    @Override
    public boolean markCompleted(long jobId, String claimToken, long experimentRunId, long durationMs) {
        if (claimToken == null || claimToken.trim().isEmpty()) {
            return false;
        }
        String sql = "UPDATE execution_jobs " +
                "SET status = 'COMPLETED', experiment_run_id = ?, execution_duration_ms = ?, " +
                "completed_at = NOW(), claimed_by = NULL, claim_token = NULL, lease_expires_at = NULL " +
                "WHERE id = ? AND status = 'RUNNING' AND claim_token = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, experimentRunId);
            ps.setLong(2, durationMs);
            ps.setLong(3, jobId);
            ps.setString(4, claimToken.trim());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("Error marking job completed: " + jobId, e);
        }
    }

    @Override
    public boolean markFailed(long jobId, String errorMessage, boolean willRetry) {
        // Strict fencing requires the active attempt claim token; unfenced failure transitions are rejected.
        return false;
    }

    @Override
    public boolean markFailed(long jobId, String claimToken, String errorMessage, boolean willRetry) {
        if (claimToken == null || claimToken.trim().isEmpty()) {
            return false;
        }
        String sql = willRetry
                ? "UPDATE execution_jobs " +
                  "SET status = CASE WHEN attempt_count < max_attempts THEN 'QUEUED' ELSE 'FAILED' END, " +
                  "error_message = ?, claimed_by = NULL, claimed_at = NULL, claim_token = NULL, lease_expires_at = NULL, " +
                  "completed_at = CASE WHEN attempt_count < max_attempts THEN NULL ELSE NOW() END " +
                  "WHERE id = ? AND status = 'RUNNING' AND claim_token = ?"
                : "UPDATE execution_jobs " +
                  "SET status = 'FAILED', error_message = ?, completed_at = NOW(), claimed_by = NULL, " +
                  "claim_token = NULL, lease_expires_at = NULL " +
                  "WHERE id = ? AND status = 'RUNNING' AND claim_token = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, errorMessage);
            ps.setLong(2, jobId);
            ps.setString(3, claimToken.trim());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("Error marking job failed: " + jobId, e);
        }
    }

    @Override
    public boolean completeJobWithExperimentRun(long jobId, String claimToken, com.orchestrivo.model.ExperimentRunRecord runRecord, long durationMs) {
        return completeJobWithExperimentRun(jobId, claimToken, runRecord, durationMs, durationMs * 1_000_000L);
    }

    @Override
    public boolean completeJobWithExperimentRun(long jobId, String claimToken, com.orchestrivo.model.ExperimentRunRecord runRecord, long durationMs, long durationNanos) {
        if (claimToken == null || claimToken.trim().isEmpty()) {
            return false;
        }
        String checkAndLockSql = "SELECT status, claim_token FROM execution_jobs WHERE id = ? FOR UPDATE";
        String insertRunSql = "INSERT INTO experiment_runs " +
                "(owner_user_id, workload_id, algorithm_name, algorithm_parameters, input_snapshot, run_status, " +
                "started_at, completed_at, validation_status, average_waiting_time, average_turnaround_time, " +
                "average_response_time, simulated_throughput, context_switches, cpu_idle_time, total_duration, error_message) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String updateJobSql = "UPDATE execution_jobs " +
                "SET status = 'COMPLETED', experiment_run_id = ?, execution_duration_ms = ?, execution_duration_nanos = ?, " +
                "completed_at = NOW(), claimed_by = NULL, claim_token = NULL, lease_expires_at = NULL " +
                "WHERE id = ? AND status = 'RUNNING' AND claim_token = ?";

        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            // 1. Verify job is still RUNNING and owned by exact non-null claimToken
            try (PreparedStatement psCheck = conn.prepareStatement(checkAndLockSql)) {
                psCheck.setLong(1, jobId);
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                    String currentStatus = rs.getString("status");
                    String currentToken = rs.getString("claim_token");
                    if (!"RUNNING".equalsIgnoreCase(currentStatus) || currentToken == null || !claimToken.trim().equals(currentToken)) {
                        conn.rollback();
                        return false;
                    }
                }
            }

            // 2. Insert experiment run
            long runId;
            try (PreparedStatement psRun = conn.prepareStatement(insertRunSql, Statement.RETURN_GENERATED_KEYS)) {
                psRun.setLong(1, runRecord.getOwnerUserId());
                psRun.setLong(2, runRecord.getWorkloadId());
                psRun.setString(3, runRecord.getAlgorithmName());
                psRun.setString(4, runRecord.getAlgorithmParameters());
                psRun.setString(5, runRecord.getInputSnapshot());
                psRun.setString(6, runRecord.getRunStatus());
                psRun.setTimestamp(7, runRecord.getStartedAt() != null ? Timestamp.valueOf(runRecord.getStartedAt()) : Timestamp.valueOf(LocalDateTime.now()));
                psRun.setTimestamp(8, runRecord.getCompletedAt() != null ? Timestamp.valueOf(runRecord.getCompletedAt()) : Timestamp.valueOf(LocalDateTime.now()));
                psRun.setString(9, runRecord.getValidationStatus());

                com.orchestrivo.domain.ScheduleResult result = runRecord.getResult();
                if (result != null && result.getAggregateMetrics() != null) {
                    com.orchestrivo.domain.AggregateMetrics am = result.getAggregateMetrics();
                    psRun.setDouble(10, am.getAverageWaitingTime());
                    psRun.setDouble(11, am.getAverageTurnaroundTime());
                    psRun.setDouble(12, am.getAverageResponseTime());
                    psRun.setDouble(13, am.getThroughput());
                    psRun.setInt(14, am.getContextSwitchCount());
                    psRun.setInt(15, am.getCpuIdleTime());
                    psRun.setInt(16, am.getTotalDuration());
                } else {
                    psRun.setNull(10, Types.DOUBLE);
                    psRun.setNull(11, Types.DOUBLE);
                    psRun.setNull(12, Types.DOUBLE);
                    psRun.setNull(13, Types.DOUBLE);
                    psRun.setNull(14, Types.INTEGER);
                    psRun.setNull(15, Types.INTEGER);
                    psRun.setNull(16, Types.INTEGER);
                }
                psRun.setString(17, runRecord.getErrorMessage());

                psRun.executeUpdate();
                try (ResultSet rs = psRun.getGeneratedKeys()) {
                    if (rs.next()) {
                        runId = rs.getLong(1);
                        runRecord.setId(runId);
                    } else {
                        conn.rollback();
                        return false;
                    }
                }
            }

            // 3. Insert child results if available
            com.orchestrivo.domain.ScheduleResult result = runRecord.getResult();
            if (result != null) {
                if (result.getProcessMetrics() != null && !result.getProcessMetrics().isEmpty()) {
                    String sqlProcRes = "INSERT INTO process_results (experiment_run_id, process_key, first_start_time, completion_time, waiting_time, turnaround_time, response_time) VALUES (?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement pstmtProcRes = conn.prepareStatement(sqlProcRes)) {
                        for (com.orchestrivo.domain.ProcessMetrics pm : result.getProcessMetrics()) {
                            pstmtProcRes.setLong(1, runId);
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
                        for (com.orchestrivo.domain.ExecutionSegment seg : result.getExecutionTimeline()) {
                            pstmtSeg.setLong(1, runId);
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

            // 4. Update execution job
            try (PreparedStatement psUpdate = conn.prepareStatement(updateJobSql)) {
                psUpdate.setLong(1, runId);
                psUpdate.setLong(2, durationMs);
                psUpdate.setLong(3, durationNanos);
                psUpdate.setLong(4, jobId);
                psUpdate.setString(5, claimToken.trim());
                int updated = psUpdate.executeUpdate();
                if (updated == 1) {
                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                    return false;
                }
            }

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw new RuntimeException("Error completing job with experiment run: " + jobId, e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    @Override
    public boolean cancelJob(long jobId, long userId) {
        String sql = "UPDATE execution_jobs " +
                "SET status = 'CANCELLED', completed_at = NOW(), error_message = 'Cancelled by user', " +
                "claimed_by = NULL, claim_token = NULL, lease_expires_at = NULL " +
                "WHERE id = ? AND owner_user_id = ? AND status = 'QUEUED'";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, jobId);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error cancelling job: " + jobId, e);
        }
    }

    @Override
    public boolean requestRetry(long jobId, long userId) {
        String sql = "UPDATE execution_jobs " +
                "SET status = 'QUEUED', claimed_by = NULL, claimed_at = NULL, claim_token = NULL, " +
                "lease_expires_at = NULL, error_message = NULL " +
                "WHERE id = ? AND owner_user_id = ? AND status = 'FAILED' AND attempt_count < max_attempts";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, jobId);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error requesting retry for job: " + jobId, e);
        }
    }

    @Override
    public int recoverStaleJobs(int staleThresholdSeconds) {
        int recovered = 0;
        int safeThreshold = Math.max(0, staleThresholdSeconds);

        String condition;
        if (safeThreshold == 0) {
            condition = "(lease_expires_at IS NOT NULL AND (lease_expires_at <= NOW() OR ? = 0)) " +
                    "OR (claimed_at IS NULL OR claimed_at <= NOW())";
        } else {
            condition = "(lease_expires_at IS NOT NULL AND lease_expires_at <= NOW()) " +
                    "OR (lease_expires_at IS NULL AND (claimed_at IS NULL OR claimed_at <= DATE_SUB(NOW(), INTERVAL ? SECOND)))";
        }

        String recoverSql = "UPDATE execution_jobs " +
                "SET status = 'QUEUED', claimed_by = NULL, claimed_at = NULL, claim_token = NULL, lease_expires_at = NULL, " +
                "error_message = 'Recovered from inactive worker lease' " +
                "WHERE status = 'RUNNING' " +
                "AND (" + condition + ") " +
                "AND attempt_count < max_attempts";

        String failExhaustedSql = "UPDATE execution_jobs " +
                "SET status = 'FAILED', completed_at = NOW(), claimed_by = NULL, claim_token = NULL, lease_expires_at = NULL, " +
                "error_message = 'Exceeded maximum attempts during inactive worker lease' " +
                "WHERE status = 'RUNNING' " +
                "AND (" + condition + ") " +
                "AND attempt_count >= max_attempts";

        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement psRec = conn.prepareStatement(recoverSql)) {
                psRec.setInt(1, safeThreshold);
                recovered += psRec.executeUpdate();
            }
            try (PreparedStatement psFail = conn.prepareStatement(failExhaustedSql)) {
                psFail.setInt(1, safeThreshold);
                recovered += psFail.executeUpdate();
            }
            return recovered;
        } catch (SQLException e) {
            throw new RuntimeException("Error recovering stale execution jobs", e);
        }
    }

    @Override
    public QueueMetrics getQueueMetrics(long userId) {
        return queryMetrics("WHERE owner_user_id = ?", userId);
    }

    @Override
    public QueueMetrics getGlobalQueueMetrics() {
        return queryMetrics("", null);
    }

    @Override
    public void deleteById(long id) {
        String sql = "DELETE FROM execution_jobs WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting execution job: " + id, e);
        }
    }

    private QueueMetrics queryMetrics(String whereClause, Long userId) {
        String sql = "SELECT " +
                "SUM(CASE WHEN status = 'QUEUED' THEN 1 ELSE 0 END) AS queued_cnt, " +
                "SUM(CASE WHEN status = 'RUNNING' THEN 1 ELSE 0 END) AS running_cnt, " +
                "SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed_cnt, " +
                "SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) AS failed_cnt, " +
                "SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END) AS cancelled_cnt, " +
                "SUM(CASE WHEN status = 'FAILED' AND attempt_count >= max_attempts THEN 1 ELSE 0 END) AS dead_letter_cnt, " +
                "AVG(queue_wait_ms) AS avg_wait, " +
                "AVG(execution_duration_ms) AS avg_duration, " +
                "MAX(CASE WHEN status = 'QUEUED' THEN TIMESTAMPDIFF(SECOND, created_at, NOW()) ELSE 0 END) AS oldest_queued " +
                "FROM execution_jobs " + whereClause;

        QueueMetrics metrics = new QueueMetrics();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (userId != null) {
                ps.setLong(1, userId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    metrics.setQueuedCount(rs.getInt("queued_cnt"));
                    metrics.setRunningCount(rs.getInt("running_cnt"));
                    metrics.setCompletedCount(rs.getInt("completed_cnt"));
                    metrics.setFailedCount(rs.getInt("failed_cnt"));
                    metrics.setCancelledCount(rs.getInt("cancelled_cnt"));
                    metrics.setDeadLetterCount(rs.getInt("dead_letter_cnt"));
                    metrics.setAvgQueueWaitMs(rs.getDouble("avg_wait"));
                    metrics.setAvgExecutionDurationMs(rs.getDouble("avg_duration"));
                    metrics.setOldestQueuedAgeSeconds(rs.getLong("oldest_queued"));
                }
            }
            return metrics;
        } catch (SQLException e) {
            throw new RuntimeException("Error querying queue metrics", e);
        }
    }

    private ExecutionJobRecord mapRow(ResultSet rs) throws SQLException {
        ExecutionJobRecord job = new ExecutionJobRecord();
        job.setId(rs.getLong("id"));
        job.setOwnerUserId(rs.getLong("owner_user_id"));
        job.setWorkloadId(rs.getLong("workload_id"));
        job.setAlgorithmName(rs.getString("algorithm_name"));
        job.setAlgorithmParameters(rs.getString("algorithm_parameters"));
        job.setInputSnapshot(rs.getString("input_snapshot"));

        String statusStr = rs.getString("status");
        if (statusStr != null) {
            try {
                job.setStatus(ExecutionJobStatus.valueOf(statusStr));
            } catch (IllegalArgumentException e) {
                job.setStatus(ExecutionJobStatus.QUEUED);
            }
        }

        job.setAttemptCount(rs.getInt("attempt_count"));
        job.setMaxAttempts(rs.getInt("max_attempts"));
        job.setClaimedBy(rs.getString("claimed_by"));

        try {
            job.setClaimToken(rs.getString("claim_token"));
        } catch (SQLException ignored) {}

        Timestamp claimedAt = rs.getTimestamp("claimed_at");
        if (claimedAt != null) job.setClaimedAt(claimedAt.toLocalDateTime());

        try {
            Timestamp leaseExp = rs.getTimestamp("lease_expires_at");
            if (leaseExp != null) job.setLeaseExpiresAt(leaseExp.toLocalDateTime());
        } catch (SQLException ignored) {}

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) job.setCreatedAt(createdAt.toLocalDateTime());

        Timestamp startedAt = rs.getTimestamp("started_at");
        if (startedAt != null) job.setStartedAt(startedAt.toLocalDateTime());

        Timestamp completedAt = rs.getTimestamp("completed_at");
        if (completedAt != null) job.setCompletedAt(completedAt.toLocalDateTime());

        long waitMs = rs.getLong("queue_wait_ms");
        if (!rs.wasNull()) job.setQueueWaitMs(waitMs);

        long durMs = rs.getLong("execution_duration_ms");
        if (!rs.wasNull()) job.setExecutionDurationMs(durMs);

        job.setErrorMessage(rs.getString("error_message"));

        long runId = rs.getLong("experiment_run_id");
        if (!rs.wasNull()) job.setExperimentRunId(runId);

        job.setIdempotencyKey(rs.getString("idempotency_key"));

        try {
            long durNanos = rs.getLong("execution_duration_nanos");
            if (!rs.wasNull()) job.setExecutionDurationNanos(durNanos);
        } catch (SQLException ignored) {}

        try {
            job.setBatchId(rs.getString("batch_id"));
        } catch (SQLException ignored) {}

        try {
            job.setWorkloadName(rs.getString("workload_name"));
        } catch (SQLException ignored) {}

        return job;
    }

    @Override
    public List<ExecutionJobRecord> findByBatchId(String batchId, long userId) {
        String sql = "SELECT j.*, w.name AS workload_name " +
                "FROM execution_jobs j " +
                "LEFT JOIN workloads w ON j.workload_id = w.id " +
                "WHERE j.batch_id = ? AND j.owner_user_id = ? " +
                "ORDER BY j.id ASC";

        List<ExecutionJobRecord> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, batchId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error querying jobs for batch: " + batchId, e);
        }
        return list;
    }
}
