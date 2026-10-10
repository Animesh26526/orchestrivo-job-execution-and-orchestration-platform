package com.orchestrivo.scheduling.strategies;

import com.orchestrivo.domain.*;
import com.orchestrivo.validation.ScheduleValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SrtfStrategyTest {
    private SrtfStrategy strategy;
    private ScheduleValidator validator;
    private SchedulingConfig config;

    @BeforeEach
    void setUp() {
        strategy = new SrtfStrategy();
        validator = new ScheduleValidator();
        config = new SchedulingConfig(AlgorithmType.SRTF);
    }

    @Test
    void testPreemption() {
        Workload workload = new Workload(Arrays.asList(
                new ProcessSpec("P1", 0, 8),
                new ProcessSpec("P2", 1, 4),
                new ProcessSpec("P3", 2, 9),
                new ProcessSpec("P4", 3, 5)
        ));

        ScheduleResult result = strategy.schedule(workload, config);
        ValidationResult vr = validator.validate(workload, result);

        assertTrue(vr.isValid(), "Result should be valid: " + vr.getErrors());
        
        // Timeline trace:
        // t=0: P1 arrives (rem=8). Runs.
        // t=1: P2 arrives (rem=4). P1 has rem=7. P2 preempts P1. P2 runs.
        // t=2: P3 arrives (rem=9). P2 has rem=3. P2 continues.
        // t=3: P4 arrives (rem=5). P2 has rem=2. P2 continues.
        // t=5: P2 finishes. Ready: P1(7), P3(9), P4(5). P4 shortest. P4 runs.
        // t=10: P4 finishes. Ready: P1(7), P3(9). P1 shortest. P1 runs.
        // t=17: P1 finishes. Ready: P3(9). P3 runs.
        // t=26: P3 finishes.
        
        // Segments: P1 (0-1), P2 (1-5), P4 (5-10), P1 (10-17), P3 (17-26)
        assertEquals(5, result.getExecutionTimeline().size());
        
        assertEquals("P1", result.getExecutionTimeline().get(0).getProcessId());
        assertEquals("P2", result.getExecutionTimeline().get(1).getProcessId());
        assertEquals("P4", result.getExecutionTimeline().get(2).getProcessId());
        assertEquals("P1", result.getExecutionTimeline().get(3).getProcessId());
        assertEquals("P3", result.getExecutionTimeline().get(4).getProcessId());
        
        assertEquals(26, result.getAggregateMetrics().getTotalDuration());
        
        // Check context switches. We switch from null->P1 (ignored), P1->P2, P2->P4, P4->P1, P1->P3. That's 4 context switches.
        assertEquals(4, result.getAggregateMetrics().getContextSwitchCount());
    }
}
