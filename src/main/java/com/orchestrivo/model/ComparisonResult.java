package com.orchestrivo.model;

import com.orchestrivo.domain.AggregateMetrics;

import java.util.Collections;
import java.util.List;

public class ComparisonResult {
    private final List<ExperimentRunRecord> runs;
    private final ComparisonMode mode;
    private final boolean compatible;
    private final String compatibilityMessage;
    private final String tradeOffNotes;

    private Long minWaitingRunId;
    private Long minTurnaroundRunId;
    private Long minResponseRunId;
    private Long maxThroughputRunId;
    private Long minContextSwitchRunId;

    public ComparisonResult(List<ExperimentRunRecord> runs, boolean compatible, String compatibilityMessage, String tradeOffNotes) {
        this(runs, compatible ? ComparisonMode.MODE_A_CONTROLLED : deduceAlternativeMode(runs), compatibilityMessage, tradeOffNotes);
    }

    public ComparisonResult(List<ExperimentRunRecord> runs, ComparisonMode mode, String compatibilityMessage, String tradeOffNotes) {
        this.runs = runs != null ? Collections.unmodifiableList(runs) : Collections.emptyList();
        this.mode = mode != null ? mode : ComparisonMode.MODE_A_CONTROLLED;
        this.compatible = (this.mode != ComparisonMode.MODE_C_EXPLORATORY);
        this.compatibilityMessage = compatibilityMessage;
        this.tradeOffNotes = tradeOffNotes;
        computeHighlights();
    }

    private static ComparisonMode deduceAlternativeMode(List<ExperimentRunRecord> runs) {
        if (runs == null || runs.size() < 2) return ComparisonMode.MODE_A_CONTROLLED;
        String firstAlgo = runs.get(0).getAlgorithmName();
        boolean allSameAlgo = runs.stream().allMatch(r -> r.getAlgorithmName() != null && r.getAlgorithmName().equalsIgnoreCase(firstAlgo));
        return allSameAlgo ? ComparisonMode.MODE_B_WORKLOAD_SCALING : ComparisonMode.MODE_C_EXPLORATORY;
    }

    private void computeHighlights() {
        if (!compatible || runs.isEmpty()) return;

        double minWait = Double.MAX_VALUE;
        double minTat = Double.MAX_VALUE;
        double minResp = Double.MAX_VALUE;
        double maxTput = -1.0;
        int minCs = Integer.MAX_VALUE;

        for (ExperimentRunRecord r : runs) {
            if (r.getResult() != null && r.getResult().getAggregateMetrics() != null) {
                AggregateMetrics am = r.getResult().getAggregateMetrics();
                if (am.getAverageWaitingTime() < minWait) {
                    minWait = am.getAverageWaitingTime();
                    minWaitingRunId = r.getId();
                }
                if (am.getAverageTurnaroundTime() < minTat) {
                    minTat = am.getAverageTurnaroundTime();
                    minTurnaroundRunId = r.getId();
                }
                if (am.getAverageResponseTime() < minResp) {
                    minResp = am.getAverageResponseTime();
                    minResponseRunId = r.getId();
                }
                if (am.getThroughput() > maxTput) {
                    maxTput = am.getThroughput();
                    maxThroughputRunId = r.getId();
                }
                if (am.getContextSwitchCount() < minCs) {
                    minCs = am.getContextSwitchCount();
                    minContextSwitchRunId = r.getId();
                }
            }
        }
    }

    public List<ExperimentRunRecord> getRuns() { return runs; }
    public ComparisonMode getMode() { return mode; }
    public boolean isCompatible() { return compatible; }
    public String getCompatibilityMessage() { return compatibilityMessage; }
    public String getTradeOffNotes() { return tradeOffNotes; }

    public Long getMinWaitingRunId() { return minWaitingRunId; }
    public Long getMinTurnaroundRunId() { return minTurnaroundRunId; }
    public Long getMinResponseRunId() { return minResponseRunId; }
    public Long getMaxThroughputRunId() { return maxThroughputRunId; }
    public Long getMinContextSwitchRunId() { return minContextSwitchRunId; }

    public static String formatDelta(double candidate, double baseline, boolean higherIsBetter) {
        if (Double.isNaN(candidate) || Double.isNaN(baseline) || Double.isInfinite(candidate) || Double.isInfinite(baseline)) {
            return "N/A";
        }
        if (Math.abs(baseline) < 1e-9) {
            return candidate == 0.0 ? "0.0%" : (candidate > 0 ? "+N/A" : "-N/A");
        }
        double delta = candidate - baseline;
        double pct = (delta / baseline) * 100.0;
        if (Math.abs(pct) < 0.05) {
            return "0.0% (parity)";
        }
        String sign = pct > 0 ? "+" : "";
        return String.format(java.util.Locale.US, "%s%.1f%%", sign, pct);
    }

    public String getWaitDelta(ExperimentRunRecord candidate) {
        if (runs.isEmpty() || candidate == null || candidate.equals(runs.get(0))) return "Baseline (Ref)";
        ExperimentRunRecord base = runs.get(0);
        if (base.getResult() == null || base.getResult().getAggregateMetrics() == null ||
            candidate.getResult() == null || candidate.getResult().getAggregateMetrics() == null) return "N/A";
        return formatDelta(candidate.getResult().getAggregateMetrics().getAverageWaitingTime(),
                           base.getResult().getAggregateMetrics().getAverageWaitingTime(), false);
    }

    public String getTurnaroundDelta(ExperimentRunRecord candidate) {
        if (runs.isEmpty() || candidate == null || candidate.equals(runs.get(0))) return "Baseline (Ref)";
        ExperimentRunRecord base = runs.get(0);
        if (base.getResult() == null || base.getResult().getAggregateMetrics() == null ||
            candidate.getResult() == null || candidate.getResult().getAggregateMetrics() == null) return "N/A";
        return formatDelta(candidate.getResult().getAggregateMetrics().getAverageTurnaroundTime(),
                           base.getResult().getAggregateMetrics().getAverageTurnaroundTime(), false);
    }

    public String getResponseDelta(ExperimentRunRecord candidate) {
        if (runs.isEmpty() || candidate == null || candidate.equals(runs.get(0))) return "Baseline (Ref)";
        ExperimentRunRecord base = runs.get(0);
        if (base.getResult() == null || base.getResult().getAggregateMetrics() == null ||
            candidate.getResult() == null || candidate.getResult().getAggregateMetrics() == null) return "N/A";
        return formatDelta(candidate.getResult().getAggregateMetrics().getAverageResponseTime(),
                           base.getResult().getAggregateMetrics().getAverageResponseTime(), false);
    }

    public String getThroughputDelta(ExperimentRunRecord candidate) {
        if (runs.isEmpty() || candidate == null || candidate.equals(runs.get(0))) return "Baseline (Ref)";
        ExperimentRunRecord base = runs.get(0);
        if (base.getResult() == null || base.getResult().getAggregateMetrics() == null ||
            candidate.getResult() == null || candidate.getResult().getAggregateMetrics() == null) return "N/A";
        return formatDelta(candidate.getResult().getAggregateMetrics().getThroughput(),
                           base.getResult().getAggregateMetrics().getThroughput(), true);
    }
}
