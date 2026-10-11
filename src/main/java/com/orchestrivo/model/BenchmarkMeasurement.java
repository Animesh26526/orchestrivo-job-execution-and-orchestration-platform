package com.orchestrivo.model;

public class BenchmarkMeasurement {
    private long id;
    private long benchmarkRunId;
    private int iterationNumber;
    private long durationNanos;

    public BenchmarkMeasurement() {}

    public BenchmarkMeasurement(int iterationNumber, long durationNanos) {
        this.iterationNumber = iterationNumber;
        this.durationNanos = durationNanos;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getBenchmarkRunId() { return benchmarkRunId; }
    public void setBenchmarkRunId(long benchmarkRunId) { this.benchmarkRunId = benchmarkRunId; }
    public int getIterationNumber() { return iterationNumber; }
    public void setIterationNumber(int iterationNumber) { this.iterationNumber = iterationNumber; }
    public long getDurationNanos() { return durationNanos; }
    public void setDurationNanos(long durationNanos) { this.durationNanos = durationNanos; }

    public double getDurationMicros() {
        return durationNanos / 1_000.0;
    }

    public double getDurationMillis() {
        return durationNanos / 1_000_000.0;
    }
}
