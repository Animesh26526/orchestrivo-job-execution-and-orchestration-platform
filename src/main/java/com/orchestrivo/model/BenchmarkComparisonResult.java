package com.orchestrivo.model;

import java.util.Locale;

/**
 * Encapsulates pairwise performance comparison between a Baseline and Candidate benchmark run.
 * Protects against division by zero and provides robust statistical delta indicators.
 */
public class BenchmarkComparisonResult {

    private final BenchmarkRunRecord baseline;
    private final BenchmarkRunRecord candidate;
    private final boolean workloadMatched;
    private final boolean fairComparison;
    private final String experimentalVariable;
    private final String variablesHeldConstant;
    private final String matchNotes;

    private final double speedupFactor;
    private final double deltaMedianNanos;
    private final double deltaMedianPct;
    private final double deltaMeanNanos;
    private final double deltaMeanPct;
    private final double deltaStdDevNanos;
    private final double deltaStdDevPct;
    private final long deltaMinNanos;
    private final long deltaMaxNanos;

    // Actual Batch Throughput (runs per elapsed wall-clock second of measured batch)
    private final double actualBaselineThroughput;
    private final double actualCandidateThroughput;
    private final double deltaActualThroughput;
    private final double deltaActualThroughputPct;

    // Latency-Derived Throughput Estimate (1 / median latency)
    private final double latencyDerivedBaselineThroughput;
    private final double latencyDerivedCandidateThroughput;
    private final double deltaLatencyDerivedThroughputPct;

    public BenchmarkComparisonResult(BenchmarkRunRecord baseline, BenchmarkRunRecord candidate) {
        if (baseline == null || candidate == null) {
            throw new IllegalArgumentException("Baseline and candidate benchmark runs cannot be null.");
        }
        this.baseline = baseline;
        this.candidate = candidate;

        // Verify complete workload & configuration matching
        boolean algoMatch = baseline.getAlgorithmName() != null
                && baseline.getAlgorithmName().equalsIgnoreCase(candidate.getAlgorithmName());
        boolean countMatch = baseline.getProcessCount() == candidate.getProcessCount();
        boolean seedMatch = baseline.getSeed() == candidate.getSeed();
        boolean quantumMatch = true;
        if ("ROUND_ROBIN".equalsIgnoreCase(baseline.getAlgorithmName()) || "ROUND_ROBIN".equalsIgnoreCase(candidate.getAlgorithmName())) {
            quantumMatch = java.util.Objects.equals(baseline.getQuantum(), candidate.getQuantum());
        }
        boolean warmupMatch = baseline.getWarmupIterations() == candidate.getWarmupIterations();
        boolean measuredMatch = baseline.getMeasuredIterations() == candidate.getMeasuredIterations();

        boolean inputWorkloadMatched = countMatch && seedMatch && warmupMatch && measuredMatch;
        this.workloadMatched = algoMatch && countMatch && seedMatch && quantumMatch && warmupMatch && measuredMatch;

        if (this.workloadMatched) {
            this.fairComparison = true;
            this.experimentalVariable = "Implementation / Execution Environment";
            this.variablesHeldConstant = "Algorithm (" + baseline.getAlgorithmName() + (baseline.getQuantum() != null ? " q=" + baseline.getQuantum() : "") + "), " + baseline.getProcessCount() + " processes, random seed " + baseline.getSeed() + ", " + baseline.getWarmupIterations() + " warmup / " + baseline.getMeasuredIterations() + " measured iterations";
            this.matchNotes = "Controlled Workload Matched: Identical algorithm ("
                    + baseline.getAlgorithmName() + (baseline.getQuantum() != null ? " q=" + baseline.getQuantum() : "")
                    + "), " + baseline.getProcessCount() + " processes, random seed (" + baseline.getSeed()
                    + "), and iteration parameters (" + baseline.getWarmupIterations() + " warmup / " + baseline.getMeasuredIterations() + " measured).";
        } else if (inputWorkloadMatched) {
            this.fairComparison = true;
            if (algoMatch && !quantumMatch) {
                this.experimentalVariable = "Time Quantum (" + baseline.getQuantum() + " -> " + candidate.getQuantum() + ")";
            } else {
                this.experimentalVariable = "Algorithm (" + baseline.getAlgorithmName() + " vs " + candidate.getAlgorithmName() + ")";
            }
            this.variablesHeldConstant = baseline.getProcessCount() + " processes, random seed " + baseline.getSeed() + ", " + baseline.getWarmupIterations() + " warmup / " + baseline.getMeasuredIterations() + " measured iterations";
            StringBuilder sb = new StringBuilder("Controlled A/B Comparison on ").append(this.experimentalVariable).append(". Held constant: ").append(this.variablesHeldConstant).append(". ");
            if (!algoMatch) sb.append("[Algorithm: ").append(baseline.getAlgorithmName()).append(" vs ").append(candidate.getAlgorithmName()).append("] ");
            if (!quantumMatch) sb.append("[Quantum: ").append(baseline.getQuantum()).append(" vs ").append(candidate.getQuantum()).append("] ");
            this.matchNotes = sb.toString().trim();
        } else {
            this.fairComparison = false;
            this.experimentalVariable = "Mismatched Inputs";
            this.variablesHeldConstant = "None (Mismatched Inputs)";
            StringBuilder sb = new StringBuilder("Configuration Variations: ");
            if (!algoMatch) sb.append("[Algorithm: ").append(baseline.getAlgorithmName()).append(" vs ").append(candidate.getAlgorithmName()).append("] ");
            if (!quantumMatch) sb.append("[Quantum: ").append(baseline.getQuantum()).append(" vs ").append(candidate.getQuantum()).append("] ");
            if (!countMatch) sb.append("[Processes: ").append(baseline.getProcessCount()).append(" vs ").append(candidate.getProcessCount()).append("] ");
            if (!seedMatch) sb.append("[Seed: ").append(baseline.getSeed()).append(" vs ").append(candidate.getSeed()).append("] ");
            if (!warmupMatch) sb.append("[Warmup: ").append(baseline.getWarmupIterations()).append(" vs ").append(candidate.getWarmupIterations()).append("] ");
            if (!measuredMatch) sb.append("[Measured: ").append(baseline.getMeasuredIterations()).append(" vs ").append(candidate.getMeasuredIterations()).append("] ");
            this.matchNotes = sb.toString().trim();
        }

        // Latency deltas: Candidate - Baseline (negative delta indicates lower latency / faster execution)
        this.deltaMedianNanos = candidate.getMedianNanos() - baseline.getMedianNanos();
        if (Math.abs(baseline.getMedianNanos()) < 1e-6) {
            this.deltaMedianPct = 0.0;
            this.speedupFactor = 1.0;
        } else {
            this.deltaMedianPct = (this.deltaMedianNanos / baseline.getMedianNanos()) * 100.0;
            if (candidate.getMedianNanos() > 1e-6) {
                this.speedupFactor = baseline.getMedianNanos() / candidate.getMedianNanos();
            } else {
                this.speedupFactor = 1.0;
            }
        }

        double baseMean = baseline.getMeanNanos();
        double candMean = candidate.getMeanNanos();
        this.deltaMeanNanos = candMean - baseMean;
        if (Math.abs(baseMean) < 1e-6) {
            this.deltaMeanPct = 0.0;
        } else {
            this.deltaMeanPct = (this.deltaMeanNanos / baseMean) * 100.0;
        }

        double baseStd = baseline.getStdDevNanos();
        double candStd = candidate.getStdDevNanos();
        this.deltaStdDevNanos = candStd - baseStd;
        if (Math.abs(baseStd) < 1e-6) {
            this.deltaStdDevPct = 0.0;
        } else {
            this.deltaStdDevPct = (this.deltaStdDevNanos / baseStd) * 100.0;
        }

        this.deltaMinNanos = candidate.getMinNanos() - baseline.getMinNanos();
        this.deltaMaxNanos = candidate.getMaxNanos() - baseline.getMaxNanos();

        // Actual batch throughput in runs/sec based on elapsed wall-clock time
        this.actualBaselineThroughput = baseline.getActualBatchThroughput();
        this.actualCandidateThroughput = candidate.getActualBatchThroughput();
        this.deltaActualThroughput = this.actualCandidateThroughput - this.actualBaselineThroughput;
        if (this.actualBaselineThroughput > 0) {
            this.deltaActualThroughputPct = (this.deltaActualThroughput / this.actualBaselineThroughput) * 100.0;
        } else {
            this.deltaActualThroughputPct = 0.0;
        }

        // Latency-derived throughput estimate in runs/sec (1 / median latency)
        this.latencyDerivedBaselineThroughput = baseline.getLatencyDerivedThroughputEstimate();
        this.latencyDerivedCandidateThroughput = candidate.getLatencyDerivedThroughputEstimate();
        if (this.latencyDerivedBaselineThroughput > 0) {
            this.deltaLatencyDerivedThroughputPct = ((this.latencyDerivedCandidateThroughput - this.latencyDerivedBaselineThroughput) / this.latencyDerivedBaselineThroughput) * 100.0;
        } else {
            this.deltaLatencyDerivedThroughputPct = 0.0;
        }
    }

    public BenchmarkRunRecord getBaseline() { return baseline; }
    public BenchmarkRunRecord getCandidate() { return candidate; }
    public boolean isWorkloadMatched() { return workloadMatched; }
    public String getMatchNotes() { return matchNotes; }

    public double getSpeedupFactor() { return speedupFactor; }
    public double getDeltaMedianNanos() { return deltaMedianNanos; }
    public double getDeltaMedianPct() { return deltaMedianPct; }
    public double getDeltaMeanNanos() { return deltaMeanNanos; }
    public double getDeltaMeanPct() { return deltaMeanPct; }
    public double getDeltaStdDevNanos() { return deltaStdDevNanos; }
    public double getDeltaStdDevPct() { return deltaStdDevPct; }
    public long getDeltaMinNanos() { return deltaMinNanos; }
    public long getDeltaMaxNanos() { return deltaMaxNanos; }

    public boolean isControlledMatch() { return workloadMatched; }
    public boolean isFairComparison() { return fairComparison; }
    public String getExperimentalVariable() { return experimentalVariable; }
    public String getVariablesHeldConstant() { return variablesHeldConstant; }

    // Actual Batch Throughput
    public double getActualBaselineThroughput() { return actualBaselineThroughput; }
    public double getActualCandidateThroughput() { return actualCandidateThroughput; }
    public double getDeltaActualThroughput() { return deltaActualThroughput; }
    public double getDeltaActualThroughputPct() { return deltaActualThroughputPct; }

    public double getBaselineActualBatchThroughput() { return actualBaselineThroughput; }
    public double getCandidateActualBatchThroughput() { return actualCandidateThroughput; }
    public double getDeltaActualBatchThroughput() { return deltaActualThroughput; }
    public double getDeltaActualBatchThroughputPct() { return deltaActualThroughputPct; }

    // Latency-derived estimate
    public double getLatencyDerivedBaselineThroughput() { return latencyDerivedBaselineThroughput; }
    public double getLatencyDerivedCandidateThroughput() { return latencyDerivedCandidateThroughput; }
    public double getDeltaLatencyDerivedThroughputPct() { return deltaLatencyDerivedThroughputPct; }

    public double getBaselineLatencyDerivedThroughputEstimate() { return latencyDerivedBaselineThroughput; }
    public double getCandidateLatencyDerivedThroughputEstimate() { return latencyDerivedCandidateThroughput; }
    public double getDeltaLatencyDerivedThroughputEstimate() { return latencyDerivedCandidateThroughput - latencyDerivedBaselineThroughput; }
    public double getDeltaLatencyDerivedThroughputEstimatePct() { return deltaLatencyDerivedThroughputPct; }

    // Backward-compatible throughput accessors
    public double getBaselineThroughput() { return actualBaselineThroughput; }
    public double getCandidateThroughput() { return actualCandidateThroughput; }
    public double getDeltaThroughputPct() { return deltaActualThroughputPct; }

    public boolean isImprovement() { return speedupFactor > 1.005; }
    public boolean isRegression() { return speedupFactor < 0.995; }
    public boolean isParity() { return !isImprovement() && !isRegression(); }

    public String getFormattedSpeedup() {
        return String.format(Locale.US, "%.2fx", speedupFactor);
    }

    public String getFormattedDeltaMedian() {
        if (Math.abs(deltaMedianPct) < 0.05) return "0.0% (parity)";
        String sign = deltaMedianPct > 0 ? "+" : "";
        return String.format(Locale.US, "%s%.1f%% (%.2f µs)", sign, deltaMedianPct, deltaMedianNanos / 1000.0);
    }

    public String getFormattedDeltaMean() {
        if (Math.abs(deltaMeanPct) < 0.05) return "0.0% (parity)";
        String sign = deltaMeanPct > 0 ? "+" : "";
        return String.format(Locale.US, "%s%.1f%% (%.2f µs)", sign, deltaMeanPct, deltaMeanNanos / 1000.0);
    }

    public String getFormattedDeltaStdDev() {
        if (Math.abs(deltaStdDevPct) < 0.05) return "0.0% (parity)";
        String sign = deltaStdDevPct > 0 ? "+" : "";
        return String.format(Locale.US, "%s%.1f%%", sign, deltaStdDevPct);
    }

    public String getFormattedDeltaThroughput() {
        if (Math.abs(deltaActualThroughputPct) < 0.05) return "0.0% (parity)";
        String sign = deltaActualThroughputPct > 0 ? "+" : "";
        return String.format(Locale.US, "%s%.1f%%", sign, deltaActualThroughputPct);
    }

    public String getFormattedDeltaActualBatchThroughput() {
        if (Math.abs(deltaActualThroughputPct) < 0.05) return "0.0% (parity)";
        String sign = deltaActualThroughputPct > 0 ? "+" : "";
        return String.format(Locale.US, "%s%.1f%% (%+.1f runs/s)", sign, deltaActualThroughputPct, deltaActualThroughput);
    }

    public String getFormattedDeltaLatencyDerivedThroughputEstimate() {
        if (Math.abs(deltaLatencyDerivedThroughputPct) < 0.05) return "0.0% (parity)";
        String sign = deltaLatencyDerivedThroughputPct > 0 ? "+" : "";
        double delta = latencyDerivedCandidateThroughput - latencyDerivedBaselineThroughput;
        return String.format(Locale.US, "%s%.1f%% (%+.1f runs/s)", sign, deltaLatencyDerivedThroughputPct, delta);
    }
}
