package com.orchestrivo.model;

import com.orchestrivo.domain.AggregateMetrics;

import java.util.Collections;
import java.util.List;

public class ComparisonResult {
    private final List<ExperimentRunRecord> runs;
    private final boolean compatible;
    private final String compatibilityMessage;
    private final String tradeOffNotes;

    private Long minWaitingRunId;
    private Long minTurnaroundRunId;
    private Long minResponseRunId;
    private Long maxThroughputRunId;
    private Long minContextSwitchRunId;

    public ComparisonResult(List<ExperimentRunRecord> runs, boolean compatible, String compatibilityMessage, String tradeOffNotes) {
        this.runs = runs != null ? Collections.unmodifiableList(runs) : Collections.emptyList();
        this.compatible = compatible;
        this.compatibilityMessage = compatibilityMessage;
        this.tradeOffNotes = tradeOffNotes;
        computeHighlights();
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
    public boolean isCompatible() { return compatible; }
    public String getCompatibilityMessage() { return compatibilityMessage; }
    public String getTradeOffNotes() { return tradeOffNotes; }

    public Long getMinWaitingRunId() { return minWaitingRunId; }
    public Long getMinTurnaroundRunId() { return minTurnaroundRunId; }
    public Long getMinResponseRunId() { return minResponseRunId; }
    public Long getMaxThroughputRunId() { return maxThroughputRunId; }
    public Long getMinContextSwitchRunId() { return minContextSwitchRunId; }
}
