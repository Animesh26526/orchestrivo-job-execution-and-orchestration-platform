package com.orchestrivo.persistence;

import com.orchestrivo.domain.ScheduleResult;
import java.util.Optional;
import java.util.List;

/**
 * Stage 1 Boundary: ExperimentRunRepository
 * Defines the contract for persisting and retrieving ScheduleResults (experiment runs).
 * Implementation will be provided in Stage 2 (JDBC).
 */
public interface ExperimentRunRepository {
    void save(ScheduleResult result, long workloadId, long ownerUserId);
    Optional<ScheduleResult> findById(long runId);
    List<ScheduleResult> findByWorkloadId(long workloadId);
    void deleteById(long runId);
}
