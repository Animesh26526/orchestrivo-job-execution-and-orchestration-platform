package com.orchestrivo.model;

import java.time.LocalDateTime;

public class AuditLogRecord {
    private long id;
    private Long actorUserId;
    private String eventType;
    private String resourceType;
    private String resourceId;
    private LocalDateTime timestamp;
    private String metadata;

    public AuditLogRecord() {}

    public AuditLogRecord(Long actorUserId, String eventType, String resourceType, String resourceId, String metadata) {
        this.actorUserId = actorUserId;
        this.eventType = eventType;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.metadata = metadata;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public Long getActorUserId() { return actorUserId; }
    public void setActorUserId(Long actorUserId) { this.actorUserId = actorUserId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}
