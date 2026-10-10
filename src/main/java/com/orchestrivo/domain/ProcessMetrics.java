package com.orchestrivo.domain;

public class ProcessMetrics {
    private final String processId;
    private final int firstStartTime;
    private final int completionTime;
    private final int turnaroundTime;
    private final int waitingTime;
    private final int responseTime;

    public ProcessMetrics(String processId, int firstStartTime, int completionTime, 
                          int turnaroundTime, int waitingTime, int responseTime) {
        this.processId = processId;
        this.firstStartTime = firstStartTime;
        this.completionTime = completionTime;
        this.turnaroundTime = turnaroundTime;
        this.waitingTime = waitingTime;
        this.responseTime = responseTime;
    }

    public String getProcessId() {
        return processId;
    }

    public int getFirstStartTime() {
        return firstStartTime;
    }

    public int getCompletionTime() {
        return completionTime;
    }

    public int getTurnaroundTime() {
        return turnaroundTime;
    }

    public int getWaitingTime() {
        return waitingTime;
    }

    public int getResponseTime() {
        return responseTime;
    }
}
