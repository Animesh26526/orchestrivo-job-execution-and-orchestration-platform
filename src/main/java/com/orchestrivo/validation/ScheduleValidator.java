package com.orchestrivo.validation;

import com.orchestrivo.domain.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScheduleValidator {

    public ValidationResult validate(Workload workload, ScheduleResult result) {
        List<String> errors = new ArrayList<>();
        List<ExecutionSegment> timeline = result.getExecutionTimeline();
        
        if (timeline == null) {
            errors.add("Timeline is null");
            return ValidationResult.failure(errors);
        }

        if (workload.isEmpty()) {
            if (!timeline.isEmpty()) {
                errors.add("Empty workload should produce empty timeline");
            }
            return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
        }

        // 1. & 2. Chronological order & no gaps/overlaps
        int expectedStartTime = 0;
        for (int i = 0; i < timeline.size(); i++) {
            ExecutionSegment segment = timeline.get(i);
            
            if (segment.getDuration() <= 0) {
                errors.add("Segment at index " + i + " has non-positive duration");
            }
            
            if (segment.getStartTime() != expectedStartTime) {
                if (i == 0 && segment.getStartTime() > 0) {
                     errors.add("Timeline must start at 0. Found gap from 0 to " + segment.getStartTime());
                } else if (segment.getStartTime() < expectedStartTime) {
                    errors.add("Overlap detected at time " + segment.getStartTime());
                } else {
                    errors.add("Gap detected in timeline from " + expectedStartTime + " to " + segment.getStartTime());
                }
            }
            expectedStartTime = segment.getEndTime();
        }

        // 3. & 4. Process execution constraints
        Map<String, ProcessSpec> processMap = new HashMap<>();
        for (ProcessSpec p : workload.getProcesses()) {
            processMap.put(p.getProcessId(), p);
        }

        Map<String, Integer> executedBurst = new HashMap<>();
        Map<String, Integer> completionTimes = new HashMap<>();
        Map<String, Integer> firstStartTimes = new HashMap<>();

        for (ExecutionSegment segment : timeline) {
            if (segment.isIdle()) continue;

            String pid = segment.getProcessId();
            if (!processMap.containsKey(pid)) {
                errors.add("Timeline contains unknown process ID: " + pid);
                continue;
            }

            ProcessSpec spec = processMap.get(pid);
            if (segment.getStartTime() < spec.getArrivalTime()) {
                errors.add("Process " + pid + " executed before its arrival time of " + spec.getArrivalTime());
            }

            executedBurst.put(pid, executedBurst.getOrDefault(pid, 0) + segment.getDuration());
            completionTimes.put(pid, segment.getEndTime());
            
            if (!firstStartTimes.containsKey(pid)) {
                firstStartTimes.put(pid, segment.getStartTime());
            }
        }

        // Check if every process received exactly its requested burst time
        for (ProcessSpec spec : workload.getProcesses()) {
            String pid = spec.getProcessId();
            int exec = executedBurst.getOrDefault(pid, 0);
            if (exec != spec.getBurstTime()) {
                errors.add("Process " + pid + " executed for " + exec + " units, expected " + spec.getBurstTime());
            }
        }

        // 5. Verify metrics consistency
        List<ProcessMetrics> metrics = result.getProcessMetrics();
        Map<String, ProcessMetrics> metricsMap = new HashMap<>();
        for (ProcessMetrics pm : metrics) {
            metricsMap.put(pm.getProcessId(), pm);
        }

        // Verify duplicates and unexpected metrics
        if (metrics.size() != workload.getProcesses().size()) {
            errors.add("Process metrics count (" + metrics.size() + ") does not match workload size (" + workload.getProcesses().size() + ")");
        }
        for (ProcessMetrics pm : metrics) {
            if (!processMap.containsKey(pm.getProcessId())) {
                errors.add("Unexpected metrics for process: " + pm.getProcessId());
            }
        }

        for (ProcessSpec spec : workload.getProcesses()) {
            String pid = spec.getProcessId();
            ProcessMetrics pm = metricsMap.get(pid);
            
            if (pm == null) {
                errors.add("Missing metrics for process " + pid);
                continue;
            }

            if (pm.getFirstStartTime() != firstStartTimes.getOrDefault(pid, -1)) {
                errors.add("First start time mismatch for process " + pid);
            }
            if (pm.getCompletionTime() != completionTimes.getOrDefault(pid, -1)) {
                errors.add("Completion time mismatch for process " + pid);
            }

            int expectedTat = pm.getCompletionTime() - spec.getArrivalTime();
            if (pm.getTurnaroundTime() != expectedTat) {
                errors.add("Turnaround time incorrect for process " + pid + ". Expected " + expectedTat);
            }

            int expectedWt = pm.getTurnaroundTime() - spec.getBurstTime();
            if (pm.getWaitingTime() != expectedWt) {
                errors.add("Waiting time incorrect for process " + pid + ". Expected " + expectedWt);
            }

            int expectedRt = pm.getFirstStartTime() - spec.getArrivalTime();
            if (pm.getResponseTime() != expectedRt) {
                errors.add("Response time incorrect for process " + pid + ". Expected " + expectedRt);
            }
        }

        // Aggregate Metrics Verification
        AggregateMetrics ag = result.getAggregateMetrics();
        if (ag != null) {
            double tol = 0.0001;
            int n = workload.getProcesses().size();
            double expectedAvgWt = 0;
            double expectedAvgTat = 0;
            double expectedAvgRt = 0;
            
            for (ProcessSpec spec : workload.getProcesses()) {
                String pid = spec.getProcessId();
                ProcessMetrics pm = metricsMap.get(pid);
                if (pm != null) {
                    expectedAvgWt += pm.getWaitingTime();
                    expectedAvgTat += pm.getTurnaroundTime();
                    expectedAvgRt += pm.getResponseTime();
                }
            }
            if (n > 0) {
                expectedAvgWt /= n;
                expectedAvgTat /= n;
                expectedAvgRt /= n;
            }
            
            if (Math.abs(ag.getAverageWaitingTime() - expectedAvgWt) > tol) {
                errors.add("Aggregate average waiting time incorrect. Expected " + expectedAvgWt);
            }
            if (Math.abs(ag.getAverageTurnaroundTime() - expectedAvgTat) > tol) {
                errors.add("Aggregate average turnaround time incorrect. Expected " + expectedAvgTat);
            }
            if (Math.abs(ag.getAverageResponseTime() - expectedAvgRt) > tol) {
                errors.add("Aggregate average response time incorrect. Expected " + expectedAvgRt);
            }
            
            int expectedDuration = timeline.isEmpty() ? 0 : timeline.get(timeline.size() - 1).getEndTime();
            if (ag.getTotalDuration() != expectedDuration) {
                errors.add("Aggregate total duration incorrect. Expected " + expectedDuration);
            }
            
            int expectedIdle = 0;
            int expectedBusy = 0;
            int contextSwitches = 0;
            String lastProcessId = null;
            
            for (ExecutionSegment segment : timeline) {
                if (segment.isIdle()) {
                    expectedIdle += segment.getDuration();
                } else {
                    expectedBusy += segment.getDuration();
                }
                
                if (!segment.getProcessId().equals(lastProcessId)) {
                    if (lastProcessId != null && !ExecutionSegment.IDLE_PROCESS_ID.equalsIgnoreCase(lastProcessId)) {
                        contextSwitches++;
                    }
                    lastProcessId = segment.getProcessId();
                }
            }
            
            if (ag.getCpuIdleTime() != expectedIdle) {
                errors.add("Aggregate CPU idle time incorrect. Expected " + expectedIdle);
            }
            if (ag.getCpuBusyTime() != expectedBusy) {
                errors.add("Aggregate CPU busy time incorrect. Expected " + expectedBusy);
            }
            if (ag.getContextSwitchCount() != contextSwitches) {
                errors.add("Aggregate context switch count incorrect. Expected " + contextSwitches);
            }
            double expectedThroughput = expectedDuration == 0 ? 0 : (double) n / expectedDuration;
            if (Math.abs(ag.getThroughput() - expectedThroughput) > tol) {
                errors.add("Aggregate throughput incorrect. Expected " + expectedThroughput);
            }
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }
}
