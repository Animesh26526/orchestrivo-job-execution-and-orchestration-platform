package com.orchestrivo.model;

public class AlgorithmBenchmarkMetric {
    private final String algorithmName;
    private final int runCount;
    private final double avgMedianMicros;
    private final double minMicros;
    private final double maxMicros;
    private final double avgActualBatchThroughput;

    public AlgorithmBenchmarkMetric(String algorithmName, int runCount, double avgMedianMicros,
                                    double minMicros, double maxMicros, double avgActualBatchThroughput) {
        this.algorithmName = algorithmName;
        this.runCount = runCount;
        this.avgMedianMicros = avgMedianMicros;
        this.minMicros = minMicros;
        this.maxMicros = maxMicros;
        this.avgActualBatchThroughput = avgActualBatchThroughput;
    }

    public String getAlgorithmName() {
        return algorithmName;
    }

    public int getRunCount() {
        return runCount;
    }

    public double getAvgMedianMicros() {
        return avgMedianMicros;
    }

    public double getMinMicros() {
        return minMicros;
    }

    public double getMaxMicros() {
        return maxMicros;
    }

    public double getAvgActualBatchThroughput() {
        return avgActualBatchThroughput;
    }

    public String getFormattedAvgMedianMicros() {
        return String.format(java.util.Locale.US, "%.2f µs", avgMedianMicros);
    }

    public String getFormattedMinMicros() {
        return String.format(java.util.Locale.US, "%.2f µs", minMicros);
    }

    public String getFormattedMaxMicros() {
        return String.format(java.util.Locale.US, "%.2f µs", maxMicros);
    }

    public String getFormattedAvgActualBatchThroughput() {
        return String.format(java.util.Locale.US, "%.1f runs/s", avgActualBatchThroughput);
    }
}
