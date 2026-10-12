package com.orchestrivo.persistence;

import com.orchestrivo.model.AuditLogRecord;
import java.util.List;

public interface AuditLogRepository {
    void save(AuditLogRecord record);
    List<AuditLogRecord> findByActorId(long actorUserId, int limit);
    List<AuditLogRecord> findRecent(int limit);
}
