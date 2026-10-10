package com.orchestrivo.scheduling.strategies;

import com.orchestrivo.domain.*;
import com.orchestrivo.validation.ScheduleValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoundRobinStrategyTest {
    private RoundRobinStrategy strategy;
    private ScheduleValidator validator;
    private SchedulingConfig config;

    @BeforeEach
    void setUp() {
        strategy = new RoundRobinStrategy();
        validator = new ScheduleValidator();
        config = new SchedulingConfig(AlgorithmType.ROUND_ROBIN, 2); // Quantum = 2
    }

    @Test
    void testRoundRobin() {
        Workload workload = new Workload(Arrays.asList(
                new ProcessSpec("P1", 0, 5),
                new ProcessSpec("P2", 1, 3),
                new ProcessSpec("P3", 2, 1)
        ));

        ScheduleResult result = strategy.schedule(workload, config);
        ValidationResult vr = validator.validate(workload, result);

        assertTrue(vr.isValid(), "Result should be valid: " + vr.getErrors());
        
        // Quantum 2
        // t=0: P1 arrives. Q=[P1]. Pop P1. Runs 0-2.
        // t=1: P2 arrives.
        // t=2: P1 preempted. Q gets P2, then P3 (arrives at 2), then P1 (added back).
        // Wait, standard RR tie breaker when arrival at t=2 and preemption at t=2:
        // Processes arriving during the interval (or exactly at end) are queued BEFORE the preempted process.
        // So at t=1, P2 is added to an intermediate arrival queue. 
        // At t=2, P3 arrives.
        // Our implementation adds all arrivals up to t=2 before requeuing P1.
        // So Q becomes: [P2, P3, P1]
        // t=2: Pop P2. Runs 2-4.
        // Q: [P3, P1] -> requeue P2 -> [P3, P1, P2]
        // t=4: Pop P3. Runs 4-5. Finishes.
        // Q: [P1, P2]
        // t=5: Pop P1. Runs 5-7.
        // Q: [P2] -> requeue P1 -> [P2, P1]
        // t=7: Pop P2. Runs 7-8. Finishes.
        // Q: [P1]
        // t=8: Pop P1. Runs 8-9. Finishes.
        // Total duration = 9.
        
        // Segments: P1 (0-2), P2 (2-4), P3 (4-5), P1 (5-7), P2 (7-8), P1 (8-9)
        assertEquals(6, result.getExecutionTimeline().size());
        assertEquals("P1", result.getExecutionTimeline().get(0).getProcessId());
        assertEquals("P2", result.getExecutionTimeline().get(1).getProcessId());
        assertEquals("P3", result.getExecutionTimeline().get(2).getProcessId());
        assertEquals("P1", result.getExecutionTimeline().get(3).getProcessId());
        assertEquals("P2", result.getExecutionTimeline().get(4).getProcessId());
        assertEquals("P1", result.getExecutionTimeline().get(5).getProcessId());
    }
}
