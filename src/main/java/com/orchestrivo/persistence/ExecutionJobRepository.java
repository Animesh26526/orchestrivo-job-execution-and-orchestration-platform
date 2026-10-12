package com.orchestrivo.persistence;

import com.orchestrivo.model.ExecutionJobRecord;
import com.orchestrivo.model.PagedResult;
import com.orchestrivo.model.QueueMetrics;

import java.util.List;
import java.util.Optional;

public interface ExecutionJobRepository {
    ExecutionJobRecord save(ExecutionJobRecord job);

    List<ExecutionJobRecord> saveAll(List<ExecutionJobRecord> jobs);

    Optional<ExecutionJobRecord> findById(long id);

    Optional<ExecutionJobRecord> findByIdAndOwner(long id, long userId);

    Optional<ExecutionJobRecord> findByIdempotencyKey(long userId, String idempotencyKey);

    PagedResult<ExecutionJobRecord> findByOwnerPaged(long userId, String statusFilter, String sortBy, String sortDir, int page, int pageSize);

    List<ExecutionJobRecord> findRecentByOwner(long userId, int limit);

    Optional<ExecutionJobRecord> claimNextQueuedJob(String workerId);

    Optional<ExecutionJobRecord> claimNextQueuedJob(String workerId, int leaseDurationSeconds);

    boolean renewLease(long jobId, String claimToken, int durationSeconds);

    boolean markCompleted(long jobId, long experimentRunId, long durationMs);

    boolean markCompleted(long jobId, String claimToken, long experimentRunId, long durationMs);

    boolean markFailed(long jobId, String errorMessage, boolean canRetry);

    boolean markFailed(long jobId, String claimToken, String errorMessage, boolean canRetry);

    List<ExecutionJobRecord> findByBatchId(String batchId, long userId);

    boolean completeJobWithExperimentRun(long jobId, String claimToken, com.orchestrivo.model.ExperimentRunRecord runRecord, long durationMs);

    boolean completeJobWithExperimentRun(long jobId, String claimToken, com.orchestrivo.model.ExperimentRunRecord runRecord, long durationMs, long durationNanos);

    boolean cancelJob(long jobId, long userId);

    boolean requestRetry(long jobId, long userId);

    int recoverStaleJobs(int staleThresholdSeconds);

    QueueMetrics getQueueMetrics(long userId);

    QueueMetrics getGlobalQueueMetrics();

    void deleteById(long id);
}
