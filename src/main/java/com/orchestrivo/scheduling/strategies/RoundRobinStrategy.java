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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class RoundRobinStrategy implements SchedulingStrategy {

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
            return MetricsCalculator.calculateResult(workload, new ArrayList<>(), "Round Robin");
        }

        int quantum = config.getTimeQuantum();
        if (quantum <= 0) {
            throw new IllegalArgumentException("Round Robin requires a strictly positive time quantum");
        }

        List<ProcessSpec> processes = new ArrayList<>(workload.getProcesses());
        // Sort initial processes by arrival time, then process ID
        processes.sort(Comparator.comparingInt(ProcessSpec::getArrivalTime)
                                 .thenComparing(ProcessSpec::getProcessId));

        Map<String, ProcessState> stateMap = new HashMap<>();
        for (ProcessSpec p : processes) {
            stateMap.put(p.getProcessId(), new ProcessState(p));
        }

        Queue<ProcessState> readyQueue = new LinkedList<>();
        List<ExecutionSegment> timeline = new ArrayList<>();
        
        int currentTime = 0;
        int nextProcessIndex = 0;

        while (nextProcessIndex < processes.size() || !readyQueue.isEmpty()) {
            // Add any processes that have arrived by currentTime
            while (nextProcessIndex < processes.size() && processes.get(nextProcessIndex).getArrivalTime() <= currentTime) {
                readyQueue.add(stateMap.get(processes.get(nextProcessIndex).getProcessId()));
                nextProcessIndex++;
            }

            if (readyQueue.isEmpty()) {
                // CPU is idle
                int nextArrivalTime = processes.get(nextProcessIndex).getArrivalTime();
                timeline.add(new ExecutionSegment(ExecutionSegment.IDLE_PROCESS_ID, currentTime, nextArrivalTime));
                currentTime = nextArrivalTime;
                continue;
            }

            ProcessState currentProcess = readyQueue.poll();
            int executionTime = Math.min(quantum, currentProcess.remainingTime);
            
            int startTime = currentTime;
            currentTime += executionTime;
            currentProcess.remainingTime -= executionTime;
            
            timeline.add(new ExecutionSegment(currentProcess.spec.getProcessId(), startTime, currentTime));

            // Any processes that arrive *during* this execution segment (or exactly at completion time)
            // must be added to the ready queue BEFORE the preempted process is added back.
            // (Standard RR tie-breaking convention).
            while (nextProcessIndex < processes.size() && processes.get(nextProcessIndex).getArrivalTime() <= currentTime) {
                readyQueue.add(stateMap.get(processes.get(nextProcessIndex).getProcessId()));
                nextProcessIndex++;
            }

            if (currentProcess.remainingTime > 0) {
                readyQueue.add(currentProcess);
            }
        }

        // Merge adjacent segments for the same process if it ran continuously (e.g., if it was the only one in queue)
        List<ExecutionSegment> mergedTimeline = new ArrayList<>();
        if (!timeline.isEmpty()) {
            ExecutionSegment current = timeline.get(0);
            for (int i = 1; i < timeline.size(); i++) {
                ExecutionSegment next = timeline.get(i);
                if (current.getProcessId().equals(next.getProcessId()) && current.getEndTime() == next.getStartTime()) {
                    current = new ExecutionSegment(current.getProcessId(), current.getStartTime(), next.getEndTime());
                } else {
                    mergedTimeline.add(current);
                    current = next;
                }
            }
            mergedTimeline.add(current);
        }

        return MetricsCalculator.calculateResult(workload, mergedTimeline, "Round Robin");
    }
}
