package com.orchestrivo.model;

public class SchedulingAlgorithmSummary {
    private final String algorithmName;
    private final int totalRuns;
    private final int successCount;
    private final int failureCount;
    private final double avgWaitingTime;
    private final double avgTurnaroundTime;
    private final double avgCpuUtilization;

    public SchedulingAlgorithmSummary(String algorithmName, int totalRuns, int successCount, int failureCount,
                                      double avgWaitingTime, double avgTurnaroundTime, double avgCpuUtilization) {
        this.algorithmName = algorithmName;
        this.totalRuns = totalRuns;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.avgWaitingTime = avgWaitingTime;
        this.avgTurnaroundTime = avgTurnaroundTime;
        this.avgCpuUtilization = avgCpuUtilization;
    }

    public String getAlgorithmName() {
        return algorithmName;
    }

    public int getTotalRuns() {
        return totalRuns;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public double getAvgWaitingTime() {
        return avgWaitingTime;
    }

    public double getAvgTurnaroundTime() {
        return avgTurnaroundTime;
    }

    public double getAvgCpuUtilization() {
        return avgCpuUtilization;
    }

    public String getFormattedAvgWaitingTime() {
        return String.format(java.util.Locale.US, "%.2f", avgWaitingTime);
    }

    public String getFormattedAvgTurnaroundTime() {
        return String.format(java.util.Locale.US, "%.2f", avgTurnaroundTime);
    }

    public String getFormattedAvgCpuUtilization() {
        return String.format(java.util.Locale.US, "%.1f%%", avgCpuUtilization);
    }
}
