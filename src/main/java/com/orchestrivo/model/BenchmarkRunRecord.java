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
    private String runType = "STANDARD";
    private String benchmarkGroup;
    private long batchElapsedNanos;
    private int validRunCount;
    private Integer quantum;
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
    public String getRunType() { return runType != null ? runType : "STANDARD"; }
    public void setRunType(String runType) { this.runType = runType; }
    public String getBenchmarkGroup() { return benchmarkGroup; }
    public void setBenchmarkGroup(String benchmarkGroup) { this.benchmarkGroup = benchmarkGroup; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<BenchmarkMeasurement> getMeasurements() { return measurements; }
    public void setMeasurements(List<BenchmarkMeasurement> measurements) {
        this.measurements = measurements != null ? new ArrayList<>(measurements) : new ArrayList<>();
    }

    public long getBatchElapsedNanos() { return batchElapsedNanos; }
    public void setBatchElapsedNanos(long batchElapsedNanos) { this.batchElapsedNanos = batchElapsedNanos; }
    public int getValidRunCount() { return validRunCount; }
    public void setValidRunCount(int validRunCount) { this.validRunCount = validRunCount; }
    public Integer getQuantum() { return quantum; }
    public void setQuantum(Integer quantum) { this.quantum = quantum; }

    public double getBatchElapsedMillis() { return batchElapsedNanos / 1_000_000.0; }
    public double getBatchElapsedSeconds() { return batchElapsedNanos / 1_000_000_000.0; }
    public String getFormattedBatchElapsed() {
        if (batchElapsedNanos <= 0) {
            return "N/A";
        }
        double ms = batchElapsedNanos / 1_000_000.0;
        if (ms < 1000.0) {
            return String.format(java.util.Locale.US, "%.2f ms", ms);
        }
        return String.format(java.util.Locale.US, "%.3f s", ms / 1000.0);
    }

    /**
     * Measures actual host benchmark throughput as successfully completed runs per elapsed wall-clock second of the measured batch.
     */
    public double getActualBatchThroughput() {
        if (batchElapsedNanos > 0 && validRunCount > 0) {
            return ((double) validRunCount * 1_000_000_000.0) / (double) batchElapsedNanos;
        }
        if (measurements != null && !measurements.isEmpty()) {
            long total = 0;
            for (BenchmarkMeasurement m : measurements) total += m.getDurationNanos();
            if (total > 0) {
                return ((double) measurements.size() * 1_000_000_000.0) / (double) total;
            }
        }
        return getLatencyDerivedThroughputEstimate();
    }

    /**
     * Reciprocal of median latency (1 / median), clearly labeled as an estimate rather than independently measured throughput.
     */
    public double getLatencyDerivedThroughputEstimate() {
        return medianNanos > 0 ? (1_000_000_000.0 / medianNanos) : 0.0;
    }

    /**
     * Backward-compatible throughput accessor returning the actual batch throughput.
     */
    public double getThroughputPerSecond() {
        return getActualBatchThroughput();
    }

    public double getMedianMicros() { return medianNanos / 1_000.0; }
    public double getMinMicros() { return minNanos / 1_000.0; }
    public double getMaxMicros() { return maxNanos / 1_000.0; }
    public double getStdDevMicros() { return stdDevNanos / 1_000.0; }

    public double getMedianMillis() { return medianNanos / 1_000_000.0; }
    public double getMinMillis() { return minNanos / 1_000_000.0; }
    public double getMaxMillis() { return maxNanos / 1_000_000.0; }
    public double getStdDevMillis() { return stdDevNanos / 1_000_000.0; }

    public double getMeanNanos() {
        if (measurements == null || measurements.isEmpty()) {
            return medianNanos;
        }
        double sum = 0.0;
        for (BenchmarkMeasurement m : measurements) {
            sum += m.getDurationNanos();
        }
        return sum / measurements.size();
    }

    public double getMeanMicros() { return getMeanNanos() / 1_000.0; }
    public double getMeanMillis() { return getMeanNanos() / 1_000_000.0; }

    public double getP95Nanos() {
        if (measurements == null || measurements.isEmpty()) {
            return maxNanos;
        }
        List<Long> sorted = new ArrayList<>();
        for (BenchmarkMeasurement m : measurements) {
            sorted.add(m.getDurationNanos());
        }
        Collections.sort(sorted);
        int index = (int) Math.ceil(0.95 * sorted.size()) - 1;
        index = Math.max(0, Math.min(sorted.size() - 1, index));
        return sorted.get(index);
    }

    public double getP95Micros() { return getP95Nanos() / 1_000.0; }
    public double getP95Millis() { return getP95Nanos() / 1_000_000.0; }

    public boolean isSampleSizeSmallForP95() {
        return measurements == null || measurements.size() < 20;
    }

    public double getCoefficientOfVariation() {
        double mean = getMeanNanos();
        if (mean <= 0.0) return 0.0;
        return (stdDevNanos / mean) * 100.0;
    }

    public java.util.Map<String, Object> getEnvironmentInfoMap() {
        if (environmentInfo == null || environmentInfo.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<java.util.Map<String, Object>>(){}.getType();
            return new com.google.gson.Gson().fromJson(environmentInfo, type);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
