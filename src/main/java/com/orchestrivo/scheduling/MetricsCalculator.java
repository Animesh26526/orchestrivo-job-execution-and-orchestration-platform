package com.orchestrivo.scheduling;

import com.orchestrivo.domain.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetricsCalculator {

    public static ScheduleResult calculateResult(Workload workload, List<ExecutionSegment> timeline, String algorithmName) {
        Map<String, ProcessSpec> specs = new HashMap<>();
        for (ProcessSpec p : workload.getProcesses()) {
            specs.put(p.getProcessId(), p);
        }

        Map<String, Integer> firstStartTimes = new HashMap<>();
        Map<String, Integer> completionTimes = new HashMap<>();
        
        int totalDuration = 0;
        int cpuIdleTime = 0;
        int cpuBusyTime = 0;
        
        if (!timeline.isEmpty()) {
            totalDuration = timeline.get(timeline.size() - 1).getEndTime();
        }

        for (ExecutionSegment segment : timeline) {
            String pid = segment.getProcessId();
            if (segment.isIdle()) {
                cpuIdleTime += segment.getDuration();
            } else {
                cpuBusyTime += segment.getDuration();
                if (!firstStartTimes.containsKey(pid)) {
                    firstStartTimes.put(pid, segment.getStartTime());
                }
                completionTimes.put(pid, segment.getEndTime());
            }
        }

        List<ProcessMetrics> processMetricsList = new ArrayList<>();
        double totalWaitingTime = 0;
        double totalTurnaroundTime = 0;
        double totalResponseTime = 0;

        for (ProcessSpec p : workload.getProcesses()) {
            String pid = p.getProcessId();
            if (!firstStartTimes.containsKey(pid)) {
                continue; // Process didn't run
            }
            int st = firstStartTimes.get(pid);
            int ct = completionTimes.get(pid);
            int at = p.getArrivalTime();
            int bt = p.getBurstTime();

            int tat = ct - at;
            int wt = tat - bt;
            int rt = st - at;

            processMetricsList.add(new ProcessMetrics(pid, st, ct, tat, wt, rt));
            
            totalWaitingTime += wt;
            totalTurnaroundTime += tat;
            totalResponseTime += rt;
        }

        int n = processMetricsList.size();
        double avgWt = n == 0 ? 0 : totalWaitingTime / n;
        double avgTat = n == 0 ? 0 : totalTurnaroundTime / n;
        double avgRt = n == 0 ? 0 : totalResponseTime / n;
        
        double throughput = totalDuration == 0 ? 0 : (double) n / totalDuration;
        
        // Context switch count: number of times we change the running process (ignoring consecutive same-process segments)
        int contextSwitches = 0;
        String lastProcessId = null;
        for (ExecutionSegment segment : timeline) {
            if (!segment.getProcessId().equals(lastProcessId)) {
                if (lastProcessId != null && !ExecutionSegment.IDLE_PROCESS_ID.equals(lastProcessId)) {
                    contextSwitches++;
                }
                lastProcessId = segment.getProcessId();
            }
        }

        AggregateMetrics aggregateMetrics = new AggregateMetrics(
            avgWt, avgTat, avgRt, totalDuration, cpuBusyTime, cpuIdleTime, throughput, contextSwitches
        );

        return new ScheduleResult(timeline, processMetricsList, aggregateMetrics, algorithmName);
    }
}
