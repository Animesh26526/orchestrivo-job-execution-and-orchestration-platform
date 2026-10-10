package com.orchestrivo.domain;

import java.util.Objects;

public class ProcessSpec {
    private final String processId;
    private final int arrivalTime;
    private final int burstTime;

    public ProcessSpec(String processId, int arrivalTime, int burstTime) {
        if (processId == null || processId.trim().isEmpty()) {
            throw new IllegalArgumentException("Process ID cannot be null or empty");
        }
        if (arrivalTime < 0) {
            throw new IllegalArgumentException("Arrival time cannot be negative");
        }
        if (burstTime <= 0) {
            throw new IllegalArgumentException("Burst time must be strictly positive");
        }
        this.processId = processId;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
    }

    public String getProcessId() {
        return processId;
    }

    public int getArrivalTime() {
        return arrivalTime;
    }

    public int getBurstTime() {
        return burstTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessSpec that = (ProcessSpec) o;
        return arrivalTime == that.arrivalTime &&
               burstTime == that.burstTime &&
               processId.equals(that.processId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processId, arrivalTime, burstTime);
    }
}
