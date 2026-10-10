package com.orchestrivo.model;

import com.orchestrivo.domain.ScheduleResult;
import java.time.LocalDateTime;

public class ExperimentRunRecord {
    private long id;
    private long ownerUserId;
    private long workloadId;
    private String algorithmName;
    private String algorithmParameters;
    private String runStatus;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String validationStatus;
    private String errorMessage;
    private ScheduleResult result;

    public ExperimentRunRecord() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(long ownerUserId) { this.ownerUserId = ownerUserId; }
    public long getWorkloadId() { return workloadId; }
    public void setWorkloadId(long workloadId) { this.workloadId = workloadId; }
    public String getAlgorithmName() { return algorithmName; }
    public void setAlgorithmName(String algorithmName) { this.algorithmName = algorithmName; }
    public String getAlgorithmParameters() { return algorithmParameters; }
    public void setAlgorithmParameters(String algorithmParameters) { this.algorithmParameters = algorithmParameters; }
    public String getRunStatus() { return runStatus; }
    public void setRunStatus(String runStatus) { this.runStatus = runStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public String getValidationStatus() { return validationStatus; }
    public void setValidationStatus(String validationStatus) { this.validationStatus = validationStatus; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public ScheduleResult getResult() { return result; }
    public void setResult(ScheduleResult result) { this.result = result; }
}
