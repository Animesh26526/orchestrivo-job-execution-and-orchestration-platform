package com.orchestrivo.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BenchmarkRunRecord {
    private long id;
    private long ownerUserId;
    private String algorithmName;
    private int processCount;
    private long seed;
    private int warmupIterations;
    private int measuredIterations;
    private double medianNanos;
    private long minNanos;
    private long maxNanos;
    private double stdDevNanos;
    private boolean validationPassed;
    private String environmentInfo;
    private LocalDateTime createdAt;
    private List<BenchmarkMeasurement> measurements = new ArrayList<>();

    public BenchmarkRunRecord() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(long ownerUserId) { this.ownerUserId = ownerUserId; }
    public String getAlgorithmName() { return algorithmName; }
    public void setAlgorithmName(String algorithmName) { this.algorithmName = algorithmName; }
    public int getProcessCount() { return processCount; }
    public void setProcessCount(int processCount) { this.processCount = processCount; }
    public long getSeed() { return seed; }
    public void setSeed(long seed) { this.seed = seed; }
    public int getWarmupIterations() { return warmupIterations; }
    public void setWarmupIterations(int warmupIterations) { this.warmupIterations = warmupIterations; }
    public int getMeasuredIterations() { return measuredIterations; }
    public void setMeasuredIterations(int measuredIterations) { this.measuredIterations = measuredIterations; }
    public double getMedianNanos() { return medianNanos; }
    public void setMedianNanos(double medianNanos) { this.medianNanos = medianNanos; }
    public long getMinNanos() { return minNanos; }
    public void setMinNanos(long minNanos) { this.minNanos = minNanos; }
    public long getMaxNanos() { return maxNanos; }
    public void setMaxNanos(long maxNanos) { this.maxNanos = maxNanos; }
    public double getStdDevNanos() { return stdDevNanos; }
    public void setStdDevNanos(double stdDevNanos) { this.stdDevNanos = stdDevNanos; }
    public boolean isValidationPassed() { return validationPassed; }
    public void setValidationPassed(boolean validationPassed) { this.validationPassed = validationPassed; }
    public String getEnvironmentInfo() { return environmentInfo; }
    public void setEnvironmentInfo(String environmentInfo) { this.environmentInfo = environmentInfo; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<BenchmarkMeasurement> getMeasurements() { return measurements; }
    public void setMeasurements(List<BenchmarkMeasurement> measurements) {
        this.measurements = measurements != null ? new ArrayList<>(measurements) : new ArrayList<>();
    }

    public double getMedianMicros() { return medianNanos / 1_000.0; }
    public double getMinMicros() { return minNanos / 1_000.0; }
    public double getMaxMicros() { return maxNanos / 1_000.0; }
    public double getStdDevMicros() { return stdDevNanos / 1_000.0; }

    public double getMedianMillis() { return medianNanos / 1_000_000.0; }
    public double getMinMillis() { return minNanos / 1_000_000.0; }
    public double getMaxMillis() { return maxNanos / 1_000_000.0; }
    public double getStdDevMillis() { return stdDevNanos / 1_000_000.0; }
}
