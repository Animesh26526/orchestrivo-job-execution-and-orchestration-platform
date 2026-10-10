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

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }
}
