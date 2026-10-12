package com.orchestrivo.scheduling;

import com.orchestrivo.domain.*;
import com.orchestrivo.scheduling.strategies.*;
import com.orchestrivo.validation.ScheduleValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SchedulingIntegrationTest {
    private ScheduleValidator validator;
    private List<SchedulingStrategy> strategies;

    @BeforeEach
    void setUp() {
        validator = new ScheduleValidator();
        strategies = Arrays.asList(
                new FcfsStrategy(),
                new SjfStrategy(),
                new SrtfStrategy(),
                new RoundRobinStrategy()
        );
    }

    private void runAllAndValidate(Workload workload, String caseName) {
        for (SchedulingStrategy strategy : strategies) {
            SchedulingConfig config = new SchedulingConfig(
                    strategy instanceof RoundRobinStrategy ? AlgorithmType.ROUND_ROBIN : AlgorithmType.FCFS, 
                    3
            ); // quantum = 3 for RR
            ScheduleResult result = strategy.schedule(workload, config);
            ValidationResult vr = validator.validate(workload, result);
            assertTrue(vr.isValid(), strategy.getClass().getSimpleName() + " failed on " + caseName + ": " + vr.getErrors());
        }
    }

    @Test
    void testEmptyWorkload() {
        runAllAndValidate(new Workload(Collections.emptyList()), "Empty");
    }

    @Test
    void testSingleProcess() {
        runAllAndValidate(new Workload(Collections.singletonList(new ProcessSpec("P1", 0, 5))), "Single");
    }

    @Test
    void testSimultaneousArrivals() {
        runAllAndValidate(new Workload(Arrays.asList(
                new ProcessSpec("P1", 0, 5),
                new ProcessSpec("P2", 0, 3),
                new ProcessSpec("P3", 0, 7)
        )), "Simultaneous Arrivals");
    }

    @Test
    void testIdleBeforeFirstArrival() {
        runAllAndValidate(new Workload(Arrays.asList(
                new ProcessSpec("P1", 10, 5),
                new ProcessSpec("P2", 12, 3)
        )), "Idle Before First Arrival");
    }

    @Test
    void testMultipleIdleGaps() {
        runAllAndValidate(new Workload(Arrays.asList(
                new ProcessSpec("P1", 0, 2),
                new ProcessSpec("P2", 5, 2),
                new ProcessSpec("P3", 10, 2)
        )), "Multiple Idle Gaps");
    }

    @Test
    void testProcessCompletingExactlyAtAnotherArrival() {
        runAllAndValidate(new Workload(Arrays.asList(
                new ProcessSpec("P1", 0, 5),
                new ProcessSpec("P2", 5, 3)
        )), "Completing at Arrival");
    }

    @Test
    void testEqualBurstTimes() {
        runAllAndValidate(new Workload(Arrays.asList(
                new ProcessSpec("P1", 0, 5),
                new ProcessSpec("P2", 2, 5),
                new ProcessSpec("P3", 4, 5)
        )), "Equal Burst Times");
    }

    @Test
    void testDeterministicRandomizedWorkloads() {
        Random rand = new Random(42); // Fixed seed
        for (int i = 0; i < 20; i++) {
            List<ProcessSpec> processes = new ArrayList<>();
            int numProcs = rand.nextInt(20) + 1; // 1 to 20
            for (int p = 0; p < numProcs; p++) {
                int arrival = rand.nextInt(100);
                int burst = rand.nextInt(50) + 1;
                processes.add(new ProcessSpec("P" + p, arrival, burst));
            }
            runAllAndValidate(new Workload(processes), "Random Workload " + i);
        }
    }
}
