package com.orchestrivo.persistence;

import com.orchestrivo.domain.Workload;
import java.util.Optional;
import java.util.List;

/**
 * Stage 1 Boundary: WorkloadRepository
 * Defines the contract for persisting and retrieving Workload definitions.
 * Implementation will be provided in Stage 2 (JDBC).
 */
public interface WorkloadRepository {
    void save(Workload workload, long ownerUserId);
    Optional<Workload> findById(long workloadId);
    List<Workload> findByOwnerId(long ownerUserId);
    void deleteById(long workloadId);
}
