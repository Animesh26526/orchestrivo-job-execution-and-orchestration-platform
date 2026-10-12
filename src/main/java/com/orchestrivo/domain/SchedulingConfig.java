package com.orchestrivo.domain;

public class SchedulingConfig {
    private final AlgorithmType algorithmType;
    private final int timeQuantum; // For Round Robin, 0 if not applicable

    public SchedulingConfig(AlgorithmType algorithmType) {
        this(algorithmType, 0);
    }

    public SchedulingConfig(AlgorithmType algorithmType, int timeQuantum) {
        if (algorithmType == null) {
            throw new IllegalArgumentException("AlgorithmType cannot be null");
        }
        if (algorithmType == AlgorithmType.ROUND_ROBIN && timeQuantum <= 0) {
            throw new IllegalArgumentException("Round Robin requires a strictly positive time quantum");
        }
        this.algorithmType = algorithmType;
        this.timeQuantum = timeQuantum;
    }

    public AlgorithmType getAlgorithmType() {
        return algorithmType;
    }

    public int getTimeQuantum() {
        return timeQuantum;
    }
}
