package com.orchestrivo.persistence;

import com.orchestrivo.model.ExperimentRunRecord;
import java.util.Optional;
import java.util.List;

public interface ExperimentRunRepository {
    ExperimentRunRecord save(ExperimentRunRecord record);
    Optional<ExperimentRunRecord> findById(long runId);
    List<ExperimentRunRecord> findByWorkloadId(long workloadId);
    List<ExperimentRunRecord> findByOwnerId(long ownerUserId);
    List<ExperimentRunRecord> findWithFilter(long ownerUserId, Long workloadId, String algorithm, String sortBy, String sortDir, int offset, int limit);
    int countWithFilter(long ownerUserId, Long workloadId, String algorithm);
    void deleteById(long runId);
}
