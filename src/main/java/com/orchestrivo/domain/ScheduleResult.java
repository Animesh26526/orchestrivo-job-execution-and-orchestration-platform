package com.orchestrivo.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScheduleResult {
    private final List<ExecutionSegment> executionTimeline;
    private final List<ProcessMetrics> processMetrics;
    private final AggregateMetrics aggregateMetrics;
    private final String algorithmName;

    public ScheduleResult(List<ExecutionSegment> executionTimeline, 
                          List<ProcessMetrics> processMetrics, 
                          AggregateMetrics aggregateMetrics, 
                          String algorithmName) {
        this.executionTimeline = Collections.unmodifiableList(new ArrayList<>(executionTimeline));
        this.processMetrics = Collections.unmodifiableList(new ArrayList<>(processMetrics));
        this.aggregateMetrics = aggregateMetrics;
        this.algorithmName = algorithmName;
    }

    public List<ExecutionSegment> getExecutionTimeline() {
        return executionTimeline;
    }

    public List<ProcessMetrics> getProcessMetrics() {
        return processMetrics;
    }

    public AggregateMetrics getAggregateMetrics() {
        return aggregateMetrics;
    }

    public String getAlgorithmName() {
        return algorithmName;
    }
}
