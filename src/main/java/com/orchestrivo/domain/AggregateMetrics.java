package com.orchestrivo.domain;

public class AggregateMetrics {
    private final double averageWaitingTime;
    private final double averageTurnaroundTime;
    private final double averageResponseTime;
    private final int totalDuration;
    private final int cpuBusyTime;
    private final int cpuIdleTime;
    private final double throughput; // processes per total duration
    private final int contextSwitchCount;

    public AggregateMetrics(double averageWaitingTime, double averageTurnaroundTime, 
                            double averageResponseTime, int totalDuration, 
                            int cpuBusyTime, int cpuIdleTime, double throughput, 
                            int contextSwitchCount) {
        this.averageWaitingTime = averageWaitingTime;
        this.averageTurnaroundTime = averageTurnaroundTime;
        this.averageResponseTime = averageResponseTime;
        this.totalDuration = totalDuration;
        this.cpuBusyTime = cpuBusyTime;
        this.cpuIdleTime = cpuIdleTime;
        this.throughput = throughput;
        this.contextSwitchCount = contextSwitchCount;
    }

    public double getAverageWaitingTime() { return averageWaitingTime; }
    public double getAverageTurnaroundTime() { return averageTurnaroundTime; }
    public double getAverageResponseTime() { return averageResponseTime; }
    public int getTotalDuration() { return totalDuration; }
    public int getCpuBusyTime() { return cpuBusyTime; }
    public int getCpuIdleTime() { return cpuIdleTime; }
    public double getThroughput() { return throughput; }
    public int getContextSwitchCount() { return contextSwitchCount; }

    public String getFormattedThroughput() {
        return String.format(java.util.Locale.US, "%.4f", throughput);
    }

    public String getFormattedAverageWaitingTime() {
        return String.format(java.util.Locale.US, "%.2f", averageWaitingTime);
    }

    public String getFormattedAverageTurnaroundTime() {
        return String.format(java.util.Locale.US, "%.2f", averageTurnaroundTime);
    }

    public String getFormattedAverageResponseTime() {
        return String.format(java.util.Locale.US, "%.2f", averageResponseTime);
    }

    public double getCpuUtilizationPercentage() {
        return totalDuration > 0 ? ((double) cpuBusyTime / totalDuration) * 100.0 : 0.0;
    }

    public String getFormattedCpuUtilization() {
        return String.format(java.util.Locale.US, "%.1f%%", getCpuUtilizationPercentage());
    }
}
