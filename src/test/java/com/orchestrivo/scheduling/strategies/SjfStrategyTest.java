package com.orchestrivo.scheduling.strategies;

import com.orchestrivo.domain.*;
import com.orchestrivo.validation.ScheduleValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SjfStrategyTest {
    private SjfStrategy strategy;
    private ScheduleValidator validator;
    private SchedulingConfig config;

    @BeforeEach
    void setUp() {
        strategy = new SjfStrategy();
        validator = new ScheduleValidator();
        config = new SchedulingConfig(AlgorithmType.SJF);
    }

    @Test
    void testSjfOrdering() {
        Workload workload = new Workload(Arrays.asList(
                new ProcessSpec("P1", 0, 8),
                new ProcessSpec("P2", 1, 4),
                new ProcessSpec("P3", 2, 9),
                new ProcessSpec("P4", 3, 5)
        ));

        ScheduleResult result = strategy.schedule(workload, config);
        ValidationResult vr = validator.validate(workload, result);

        assertTrue(vr.isValid(), "Result should be valid: " + vr.getErrors());
        
        // P1 runs 0-8 (arrives at 0, starts immediately, non-preemptive)
        // At 8, P2(4), P3(9), P4(5) are ready. Shortest is P2.
        // P2 runs 8-12
        // At 12, P3(9), P4(5) ready. Shortest is P4.
        // P4 runs 12-17
        // P3 runs 17-26
        
        assertEquals(4, result.getExecutionTimeline().size());
        assertEquals("P1", result.getExecutionTimeline().get(0).getProcessId());
        assertEquals("P2", result.getExecutionTimeline().get(1).getProcessId());
        assertEquals("P4", result.getExecutionTimeline().get(2).getProcessId());
        assertEquals("P3", result.getExecutionTimeline().get(3).getProcessId());
        
        assertEquals(26, result.getAggregateMetrics().getTotalDuration());
    }
}
