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

public class FcfsStrategy implements SchedulingStrategy {

    @Override
    public ScheduleResult schedule(Workload workload, SchedulingConfig config) {
        if (workload.isEmpty()) {
            return MetricsCalculator.calculateResult(workload, new ArrayList<>(), "FCFS");
        }

        List<ProcessSpec> processes = new ArrayList<>(workload.getProcesses());
        // Sort by arrival time, then by process ID for tie-breaking
        processes.sort(Comparator.comparingInt(ProcessSpec::getArrivalTime)
                                 .thenComparing(ProcessSpec::getProcessId));

        List<ExecutionSegment> timeline = new ArrayList<>();
        int currentTime = 0;

        for (ProcessSpec process : processes) {
            if (currentTime < process.getArrivalTime()) {
                // CPU is idle
                timeline.add(new ExecutionSegment(ExecutionSegment.IDLE_PROCESS_ID, currentTime, process.getArrivalTime()));
                currentTime = process.getArrivalTime();
            }

            int startTime = currentTime;
            int endTime = startTime + process.getBurstTime();
            timeline.add(new ExecutionSegment(process.getProcessId(), startTime, endTime));
            
            currentTime = endTime;
        }

        return MetricsCalculator.calculateResult(workload, timeline, "FCFS");
    }
}
