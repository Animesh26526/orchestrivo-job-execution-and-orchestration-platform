package com.orchestrivo.model;

import com.orchestrivo.domain.Workload;
import java.time.LocalDateTime;

public class WorkloadRecord {
    private long id;
    private long ownerUserId;
    private String name;
    private String description;
    private String sourceType;
    private Long generationSeed;
    private String generationConfig;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Workload workload;

    public WorkloadRecord() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(long ownerUserId) { this.ownerUserId = ownerUserId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Long getGenerationSeed() { return generationSeed; }
    public void setGenerationSeed(Long generationSeed) { this.generationSeed = generationSeed; }
    public String getGenerationConfig() { return generationConfig; }
    public void setGenerationConfig(String generationConfig) { this.generationConfig = generationConfig; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Workload getWorkload() { return workload; }
    public void setWorkload(Workload workload) { this.workload = workload; }

    public int getProcessCount() {
        return (workload != null && workload.getProcesses() != null) ? workload.getProcesses().size() : 0;
    }
}
