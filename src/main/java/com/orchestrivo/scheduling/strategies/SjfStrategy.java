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
import java.util.List;
import java.util.PriorityQueue;

public class SjfStrategy implements SchedulingStrategy {

    @Override
    public ScheduleResult schedule(Workload workload, SchedulingConfig config) {
        if (workload.isEmpty()) {
            return MetricsCalculator.calculateResult(workload, new ArrayList<>(), "SJF");
        }

        List<ProcessSpec> remainingProcesses = new ArrayList<>(workload.getProcesses());
        remainingProcesses.sort(Comparator.comparingInt(ProcessSpec::getArrivalTime)
                                          .thenComparing(ProcessSpec::getProcessId));

        // Priority queue to order ready processes by burst time, then arrival time, then process ID
        PriorityQueue<ProcessSpec> readyQueue = new PriorityQueue<>(
            Comparator.comparingInt(ProcessSpec::getBurstTime)
                      .thenComparingInt(ProcessSpec::getArrivalTime)
                      .thenComparing(ProcessSpec::getProcessId)
        );

        List<ExecutionSegment> timeline = new ArrayList<>();
        int currentTime = 0;
        int nextProcessIndex = 0;

        while (nextProcessIndex < remainingProcesses.size() || !readyQueue.isEmpty()) {
            // Add all processes that have arrived up to currentTime to the ready queue
            while (nextProcessIndex < remainingProcesses.size() && 
                   remainingProcesses.get(nextProcessIndex).getArrivalTime() <= currentTime) {
                readyQueue.add(remainingProcesses.get(nextProcessIndex));
                nextProcessIndex++;
            }

            if (readyQueue.isEmpty()) {
                // CPU is idle, advance to next process arrival time
                ProcessSpec nextProcess = remainingProcesses.get(nextProcessIndex);
                int nextArrivalTime = nextProcess.getArrivalTime();
                timeline.add(new ExecutionSegment(ExecutionSegment.IDLE_PROCESS_ID, currentTime, nextArrivalTime));
                currentTime = nextArrivalTime;
            } else {
                // Execute the process with the shortest burst time
                ProcessSpec currentProcess = readyQueue.poll();
                int startTime = currentTime;
                int endTime = startTime + currentProcess.getBurstTime();
                timeline.add(new ExecutionSegment(currentProcess.getProcessId(), startTime, endTime));
                currentTime = endTime;
            }
        }

        return MetricsCalculator.calculateResult(workload, timeline, "SJF");
    }
}
