package com.orchestrivo.persistence;

import com.orchestrivo.model.BenchmarkRunRecord;
import java.util.List;
import java.util.Optional;

public interface BenchmarkRepository {
    BenchmarkRunRecord save(BenchmarkRunRecord record);
    Optional<BenchmarkRunRecord> findById(long id);
    List<BenchmarkRunRecord> findByOwnerId(long ownerUserId);
    List<BenchmarkRunRecord> findWithFilter(long ownerUserId, int offset, int limit);
    int countByOwnerId(long ownerUserId);
    void deleteById(long id);
}
