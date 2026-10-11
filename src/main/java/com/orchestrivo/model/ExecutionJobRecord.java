package com.orchestrivo.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ExecutionJobRecord {
    private long id;
    private long ownerUserId;
    private long workloadId;
    private String algorithmName;
    private String algorithmParameters;
    private String inputSnapshot;
    private ExecutionJobStatus status = ExecutionJobStatus.QUEUED;
    private int attemptCount = 0;
    private int maxAttempts = 3;
    private String claimedBy;
    private String claimToken;
    private LocalDateTime claimedAt;
    private LocalDateTime leaseExpiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long queueWaitMs;
    private Long executionDurationMs;
    private String errorMessage;
    private Long experimentRunId;
    private String idempotencyKey;

    // Associated UI helper data
    private String workloadName;

    public ExecutionJobRecord() {}

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public long getWorkloadId() {
        return workloadId;
    }

    public void setWorkloadId(long workloadId) {
        this.workloadId = workloadId;
    }

    public String getAlgorithmName() {
        return algorithmName;
    }

    public void setAlgorithmName(String algorithmName) {
        this.algorithmName = algorithmName;
    }

    public String getAlgorithmParameters() {
        return algorithmParameters;
    }

    public void setAlgorithmParameters(String algorithmParameters) {
        this.algorithmParameters = algorithmParameters;
    }

    public String getInputSnapshot() {
        return inputSnapshot;
    }

    public void setInputSnapshot(String inputSnapshot) {
        this.inputSnapshot = inputSnapshot;
    }

    public ExecutionJobStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionJobStatus status) {
        this.status = status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public String getClaimedBy() {
        return claimedBy;
    }

    public void setClaimedBy(String claimedBy) {
        this.claimedBy = claimedBy;
    }

    public LocalDateTime getClaimedAt() {
        return claimedAt;
    }

    public void setClaimedAt(LocalDateTime claimedAt) {
        this.claimedAt = claimedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Long getQueueWaitMs() {
        return queueWaitMs;
    }

    public void setQueueWaitMs(Long queueWaitMs) {
        this.queueWaitMs = queueWaitMs;
    }

    public Long getExecutionDurationMs() {
        return executionDurationMs;
    }

    public void setExecutionDurationMs(Long executionDurationMs) {
        this.executionDurationMs = executionDurationMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getExperimentRunId() {
        return experimentRunId;
    }

    public void setExperimentRunId(Long experimentRunId) {
        this.experimentRunId = experimentRunId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getWorkloadName() {
        return workloadName;
    }

    public void setWorkloadName(String workloadName) {
        this.workloadName = workloadName;
    }

    public String getClaimToken() {
        return claimToken;
    }

    public void setClaimToken(String claimToken) {
        this.claimToken = claimToken;
    }

    public LocalDateTime getLeaseExpiresAt() {
        return leaseExpiresAt;
    }

    public void setLeaseExpiresAt(LocalDateTime leaseExpiresAt) {
        this.leaseExpiresAt = leaseExpiresAt;
    }

    public List<com.orchestrivo.domain.ProcessSpec> getSnapshotProcesses() {
        if (inputSnapshot == null || inputSnapshot.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<com.orchestrivo.domain.ProcessSpec>>(){}.getType();
            return new com.google.gson.Gson().fromJson(inputSnapshot, listType);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public String getFormattedParameters() {
        if (algorithmParameters == null || algorithmParameters.trim().isEmpty()) {
            return "Standard (Default parameters)";
        }
        try {
            java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<java.util.Map<String, Object>>(){}.getType();
            java.util.Map<String, Object> map = new com.google.gson.Gson().fromJson(algorithmParameters, type);
            if (map == null || map.isEmpty()) {
                return "Standard (Default parameters)";
            }
            List<String> parts = new ArrayList<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                if ("quantum".equalsIgnoreCase(key)) {
                    parts.add("Time Quantum: " + entry.getValue());
                } else {
                    parts.add(key + ": " + entry.getValue());
                }
            }
            return String.join(", ", parts);
        } catch (Exception e) {
            return algorithmParameters;
        }
    }

    public String getUserFacingErrorMessage() {
        if (errorMessage == null || errorMessage.trim().isEmpty()) {
            return null;
        }
        String msg = errorMessage.trim();
        // Prevent leaking SQL, connection strings, passwords, or full traces
        if (msg.contains("SQLException") || msg.contains("Hikari") || msg.contains("jdbc:") || msg.contains("password")) {
            return "Execution failed due to a database processing error. The job may be retried.";
        }
        if (msg.contains("Exception:") || msg.contains("\tat ")) {
            int idx = msg.indexOf("Exception:");
            if (idx >= 0 && idx + 10 < msg.length()) {
                String clean = msg.substring(idx + 10).split("\n")[0].trim();
                return clean.isEmpty() ? "Execution failed due to an internal error." : clean;
            }
            return "Execution failed due to an internal calculation error.";
        }
        return msg;
    }

    public boolean isEligibleForRetry() {
        return status != null && status.canRetry(attemptCount, maxAttempts);
    }

    public boolean isEligibleForCancellation() {
        return status != null && status.canCancel();
    }
}
