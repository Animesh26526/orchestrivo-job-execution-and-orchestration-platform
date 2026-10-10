package com.orchestrivo.domain;

public class ExecutionSegment {
    public static final String IDLE_PROCESS_ID = "IDLE";
    
    private final String processId;
    private final int startTime;
    private final int endTime;

    public ExecutionSegment(String processId, int startTime, int endTime) {
        if (processId == null || processId.trim().isEmpty()) {
            throw new IllegalArgumentException("Process ID cannot be null or empty");
        }
        if (startTime < 0) {
            throw new IllegalArgumentException("Start time cannot be negative");
        }
        if (endTime <= startTime) {
            throw new IllegalArgumentException("End time must be strictly greater than start time");
        }
        
        this.processId = processId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getProcessId() {
        return processId;
    }

    public int getStartTime() {
        return startTime;
    }

    public int getEndTime() {
        return endTime;
    }
    
    public int getDuration() {
        return endTime - startTime;
    }
    
    public boolean isIdle() {
        return IDLE_PROCESS_ID.equals(processId);
    }
}
