package com.orchestrivo.service;

import com.google.gson.Gson;
import com.orchestrivo.model.AuditLogRecord;
import com.orchestrivo.persistence.AuditLogRepository;

import java.util.*;

public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final Gson gson = new Gson();

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(Long actorUserId, String eventType, String resourceType, String resourceId, Map<String, Object> metadata) {
        try {
            Map<String, Object> sanitized = sanitizeMetadata(metadata);
            String metaJson = sanitized != null && !sanitized.isEmpty() ? gson.toJson(sanitized) : null;

            AuditLogRecord record = new AuditLogRecord(actorUserId, eventType, resourceType, resourceId, metaJson);
            auditLogRepository.save(record);
        } catch (Exception e) {
            // Non-blocking: audit log failure should never abort the primary business transaction
            System.err.println("WARN: Failed to persist audit log: " + e.getMessage());
        }
    }

    public List<AuditLogRecord> getLogsForActor(long actorUserId, int limit) {
        return auditLogRepository.findByActorId(actorUserId, limit);
    }

    public List<AuditLogRecord> getRecentLogs(int limit) {
        return auditLogRepository.findRecent(limit);
    }

    private Map<String, Object> sanitizeMetadata(Map<String, Object> metadata) {
        if (metadata == null) return null;
        Map<String, Object> sanitized = new HashMap<>();
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            String key = entry.getKey();
            if (key == null) continue;
            String lower = key.toLowerCase();
            if (lower.contains("password") || lower.contains("token") || lower.contains("secret")
                    || lower.contains("credential") || lower.contains("session") || lower.contains("auth")) {
                sanitized.put(key, "[REDACTED]");
            } else {
                sanitized.put(key, entry.getValue());
            }
        }
        return sanitized;
    }
}
