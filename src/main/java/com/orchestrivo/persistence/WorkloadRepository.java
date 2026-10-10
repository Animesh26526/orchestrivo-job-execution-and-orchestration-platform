package com.orchestrivo.persistence;

import com.orchestrivo.model.WorkloadRecord;
import java.util.Optional;
import java.util.List;

public interface WorkloadRepository {
    WorkloadRecord save(WorkloadRecord record);
    Optional<WorkloadRecord> findById(long workloadId);
    List<WorkloadRecord> findByOwnerId(long ownerUserId);
    void deleteById(long workloadId);
}
