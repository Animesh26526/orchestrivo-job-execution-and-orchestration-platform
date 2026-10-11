package com.orchestrivo.persistence;

import com.orchestrivo.model.WorkloadRecord;
import java.util.Optional;
import java.util.List;

public interface WorkloadRepository {
    WorkloadRecord save(WorkloadRecord record);
    WorkloadRecord update(WorkloadRecord record);
    Optional<WorkloadRecord> findById(long workloadId);
    List<WorkloadRecord> findByOwnerId(long ownerUserId);
    List<WorkloadRecord> findWithFilter(long ownerUserId, String search, String sortBy, String sortDir, int offset, int limit);
    int countWithFilter(long ownerUserId, String search);
    void deleteById(long workloadId);
}
