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
    private final double baselineThroughput;
    private final double candidateThroughput;
    private final double deltaThroughputPct;

    public BenchmarkComparisonResult(BenchmarkRunRecord baseline, BenchmarkRunRecord candidate) {
        if (baseline == null || candidate == null) {
            throw new IllegalArgumentException("Baseline and candidate benchmark runs cannot be null.");
        }
        this.baseline = baseline;
        this.candidate = candidate;

        // Verify workload matching
        boolean algoMatch = baseline.getAlgorithmName() != null
                && baseline.getAlgorithmName().equalsIgnoreCase(candidate.getAlgorithmName());
        boolean countMatch = baseline.getProcessCount() == candidate.getProcessCount();
        boolean seedMatch = baseline.getSeed() == candidate.getSeed();
        this.workloadMatched = algoMatch && countMatch && seedMatch;

        if (this.workloadMatched) {
            this.matchNotes = "Controlled Workload Matched: Identical algorithm ("
                    + baseline.getAlgorithmName() + "), " + baseline.getProcessCount()
                    + " processes, and pseudo-random seed (" + baseline.getSeed() + ").";
        } else {
            StringBuilder sb = new StringBuilder("Workload Parameter Variation: ");
            if (!algoMatch) sb.append("[Algorithm: ").append(baseline.getAlgorithmName()).append(" vs ").append(candidate.getAlgorithmName()).append("] ");
            if (!countMatch) sb.append("[Processes: ").append(baseline.getProcessCount()).append(" vs ").append(candidate.getProcessCount()).append("] ");
            if (!seedMatch) sb.append("[Seed: ").append(baseline.getSeed()).append(" vs ").append(candidate.getSeed()).append("] ");
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

        // Throughput in runs/sec based on median latency
        this.baselineThroughput = baseline.getMedianNanos() > 0 ? (1_000_000_000.0 / baseline.getMedianNanos()) : 0.0;
        this.candidateThroughput = candidate.getMedianNanos() > 0 ? (1_000_000_000.0 / candidate.getMedianNanos()) : 0.0;
        if (this.baselineThroughput > 0) {
            this.deltaThroughputPct = ((this.candidateThroughput - this.baselineThroughput) / this.baselineThroughput) * 100.0;
        } else {
            this.deltaThroughputPct = 0.0;
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

    public double getBaselineThroughput() { return baselineThroughput; }
    public double getCandidateThroughput() { return candidateThroughput; }
    public double getDeltaThroughputPct() { return deltaThroughputPct; }

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
        if (Math.abs(deltaThroughputPct) < 0.05) return "0.0% (parity)";
        String sign = deltaThroughputPct > 0 ? "+" : "";
        return String.format(Locale.US, "%s%.1f%%", sign, deltaThroughputPct);
    }
}
