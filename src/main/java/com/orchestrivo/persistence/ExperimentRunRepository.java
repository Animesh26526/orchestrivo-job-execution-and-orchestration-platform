package com.orchestrivo.persistence;

import com.orchestrivo.model.ExperimentRunRecord;
import java.util.Optional;
import java.util.List;

public interface ExperimentRunRepository {
    ExperimentRunRecord save(ExperimentRunRecord record);
    Optional<ExperimentRunRecord> findById(long runId);
    List<ExperimentRunRecord> findByWorkloadId(long workloadId);
    List<ExperimentRunRecord> findByOwnerId(long ownerUserId);
    void deleteById(long runId);
}
