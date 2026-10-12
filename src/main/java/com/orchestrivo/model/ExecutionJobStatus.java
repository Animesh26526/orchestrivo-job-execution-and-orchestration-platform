package com.orchestrivo.model;

public enum ExecutionJobStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }

    public boolean canRetry(int attemptCount, int maxAttempts) {
        return this == FAILED && attemptCount < maxAttempts;
    }

    public boolean canCancel() {
        return this == QUEUED;
    }
}
