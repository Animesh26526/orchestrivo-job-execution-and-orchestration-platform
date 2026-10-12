package com.orchestrivo.model;

public class QueueMetrics {
    private int queuedCount;
    private int runningCount;
    private int completedCount;
    private int failedCount;
    private int cancelledCount;
    private int deadLetterCount;
    private double avgQueueWaitMs;
    private double avgExecutionDurationMs;
    private long oldestQueuedAgeSeconds;

    public QueueMetrics() {}

    public int getQueuedCount() {
        return queuedCount;
    }

    public void setQueuedCount(int queuedCount) {
        this.queuedCount = queuedCount;
    }

    public int getRunningCount() {
        return runningCount;
    }

    public void setRunningCount(int runningCount) {
        this.runningCount = runningCount;
    }

    public int getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(int completedCount) {
        this.completedCount = completedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public int getCancelledCount() {
        return cancelledCount;
    }

    public void setCancelledCount(int cancelledCount) {
        this.cancelledCount = cancelledCount;
    }

    public int getDeadLetterCount() {
        return deadLetterCount;
    }

    public void setDeadLetterCount(int deadLetterCount) {
        this.deadLetterCount = deadLetterCount;
    }

    public double getAvgQueueWaitMs() {
        return avgQueueWaitMs;
    }

    public void setAvgQueueWaitMs(double avgQueueWaitMs) {
        this.avgQueueWaitMs = avgQueueWaitMs;
    }

    public double getAvgExecutionDurationMs() {
        return avgExecutionDurationMs;
    }

    public void setAvgExecutionDurationMs(double avgExecutionDurationMs) {
        this.avgExecutionDurationMs = avgExecutionDurationMs;
    }

    public long getOldestQueuedAgeSeconds() {
        return oldestQueuedAgeSeconds;
    }

    public void setOldestQueuedAgeSeconds(long oldestQueuedAgeSeconds) {
        this.oldestQueuedAgeSeconds = oldestQueuedAgeSeconds;
    }

    public int getTotalJobs() {
        return queuedCount + runningCount + completedCount + failedCount + cancelledCount;
    }

    public String getFormattedAvgQueueWaitMs() {
        if (avgQueueWaitMs <= 0) {
            return "0.0 ms";
        }
        return String.format(java.util.Locale.US, "%.1f ms", avgQueueWaitMs);
    }

    public String getFormattedAvgExecutionDurationMs() {
        if (avgExecutionDurationMs <= 0) {
            return "0.0 ms";
        }
        return String.format(java.util.Locale.US, "%.1f ms", avgExecutionDurationMs);
    }
}
