package com.orchestrivo.scheduling.strategies;

import com.orchestrivo.domain.ExecutionSegment;
import com.orchestrivo.domain.ProcessSpec;
import com.orchestrivo.domain.ScheduleResult;
import com.orchestrivo.domain.SchedulingConfig;
import com.orchestrivo.domain.Workload;
import com.orchestrivo.scheduling.MetricsCalculator;
import com.orchestrivo.scheduling.SchedulingStrategy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class SrtfStrategy implements SchedulingStrategy {

    private static class ProcessState {
        final ProcessSpec spec;
        int remainingTime;

        ProcessState(ProcessSpec spec) {
            this.spec = spec;
            this.remainingTime = spec.getBurstTime();
        }
    }

    @Override
    public ScheduleResult schedule(Workload workload, SchedulingConfig config) {
        if (workload.isEmpty()) {
            return MetricsCalculator.calculateResult(workload, new ArrayList<>(), "SRTF");
        }

        List<ProcessSpec> processes = new ArrayList<>(workload.getProcesses());
        processes.sort(Comparator.comparingInt(ProcessSpec::getArrivalTime)
                                 .thenComparing(ProcessSpec::getProcessId));

        Map<String, ProcessState> stateMap = new HashMap<>();
        for (ProcessSpec p : processes) {
            stateMap.put(p.getProcessId(), new ProcessState(p));
        }

        PriorityQueue<ProcessState> readyQueue = new PriorityQueue<>(
            Comparator.comparingInt((ProcessState s) -> s.remainingTime)
                      .thenComparingInt(s -> s.spec.getArrivalTime())
                      .thenComparing(s -> s.spec.getProcessId())
        );

        List<ExecutionSegment> rawTimeline = new ArrayList<>();
        int currentTime = 0;
        int nextProcessIndex = 0;
        ProcessState runningProcess = null;
        int segmentStartTime = 0;

        while (nextProcessIndex < processes.size() || !readyQueue.isEmpty() || runningProcess != null) {
            // Find next event time: either next arrival, or completion of current process
            int nextArrivalTime = nextProcessIndex < processes.size() ? processes.get(nextProcessIndex).getArrivalTime() : Integer.MAX_VALUE;
            int nextCompletionTime = runningProcess != null ? currentTime + runningProcess.remainingTime : Integer.MAX_VALUE;

            if (runningProcess == null && readyQueue.isEmpty()) {
                // CPU is idle until next arrival
                if (nextArrivalTime != Integer.MAX_VALUE) {
                    if (currentTime < nextArrivalTime) {
                        rawTimeline.add(new ExecutionSegment(ExecutionSegment.IDLE_PROCESS_ID, currentTime, nextArrivalTime));
                    }
                    currentTime = nextArrivalTime;
                }
            } else {
                // Determine step time
                int stepTime = Math.min(nextArrivalTime, nextCompletionTime);
                if (runningProcess != null) {
                    runningProcess.remainingTime -= (stepTime - currentTime);
                }
                currentTime = stepTime;
                
                if (runningProcess != null && runningProcess.remainingTime == 0) {
                    rawTimeline.add(new ExecutionSegment(runningProcess.spec.getProcessId(), segmentStartTime, currentTime));
                    runningProcess = null;
                }
            }

            // Process arrivals
            while (nextProcessIndex < processes.size() && processes.get(nextProcessIndex).getArrivalTime() == currentTime) {
                readyQueue.add(stateMap.get(processes.get(nextProcessIndex).getProcessId()));
                nextProcessIndex++;
            }

            // Decide who runs next
            if (!readyQueue.isEmpty()) {
                ProcessState shortestReady = readyQueue.peek();
                if (runningProcess == null || shortestReady.remainingTime < runningProcess.remainingTime) {
                    if (runningProcess != null) {
                        // Preempt current process
                        rawTimeline.add(new ExecutionSegment(runningProcess.spec.getProcessId(), segmentStartTime, currentTime));
                        readyQueue.add(runningProcess);
                    }
                    runningProcess = readyQueue.poll();
                    segmentStartTime = currentTime;
                }
            } else if (runningProcess == null && !readyQueue.isEmpty()) {
                runningProcess = readyQueue.poll();
                segmentStartTime = currentTime;
            }
        }

        // Merge adjacent segments of the same process
        List<ExecutionSegment> mergedTimeline = new ArrayList<>();
        if (!rawTimeline.isEmpty()) {
            ExecutionSegment current = rawTimeline.get(0);
            for (int i = 1; i < rawTimeline.size(); i++) {
                ExecutionSegment next = rawTimeline.get(i);
                if (current.getProcessId().equals(next.getProcessId()) && current.getEndTime() == next.getStartTime()) {
                    current = new ExecutionSegment(current.getProcessId(), current.getStartTime(), next.getEndTime());
                } else {
                    mergedTimeline.add(current);
                    current = next;
                }
            }
            mergedTimeline.add(current);
        }

        return MetricsCalculator.calculateResult(workload, mergedTimeline, "SRTF");
    }
}
