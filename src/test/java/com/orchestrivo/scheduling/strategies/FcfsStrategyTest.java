package com.orchestrivo.scheduling.strategies;

import com.orchestrivo.domain.*;
import com.orchestrivo.validation.ScheduleValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class FcfsStrategyTest {

    private FcfsStrategy strategy;
    private ScheduleValidator validator;
    private SchedulingConfig config;

    @BeforeEach
    void setUp() {
        strategy = new FcfsStrategy();
        validator = new ScheduleValidator();
        config = new SchedulingConfig(AlgorithmType.FCFS);
    }

    @Test
    void testSingleProcess() {
        Workload workload = new Workload(Collections.singletonList(
                new ProcessSpec("P1", 0, 5)
        ));

        ScheduleResult result = strategy.schedule(workload, config);
        ValidationResult vr = validator.validate(workload, result);

        assertTrue(vr.isValid(), "Result should be valid: " + vr.getErrors());
        assertEquals(1, result.getExecutionTimeline().size());
        assertEquals(5, result.getAggregateMetrics().getTotalDuration());
        assertEquals(5, result.getProcessMetrics().get(0).getCompletionTime());
    }

    @Test
    void testMultipleProcessesArrivingAtZero() {
        Workload workload = new Workload(Arrays.asList(
                new ProcessSpec("P1", 0, 5),
                new ProcessSpec("P2", 0, 3),
                new ProcessSpec("P3", 0, 1)
        ));

        ScheduleResult result = strategy.schedule(workload, config);
        ValidationResult vr = validator.validate(workload, result);

        assertTrue(vr.isValid(), "Result should be valid: " + vr.getErrors());
        assertEquals(3, result.getExecutionTimeline().size());
        
        // P1 runs 0-5, P2 runs 5-8, P3 runs 8-9
        assertEquals(9, result.getAggregateMetrics().getTotalDuration());
        
        assertEquals(5, getMetrics(result, "P1").getCompletionTime());
        assertEquals(8, getMetrics(result, "P2").getCompletionTime());
        assertEquals(9, getMetrics(result, "P3").getCompletionTime());
        
        // Waiting time: P1:0, P2:5, P3:8
        assertEquals(13.0 / 3, result.getAggregateMetrics().getAverageWaitingTime(), 0.001);
    }

    @Test
    void testIdleTime() {
        Workload workload = new Workload(Arrays.asList(
                new ProcessSpec("P1", 2, 3),
                new ProcessSpec("P2", 8, 2)
        ));

        ScheduleResult result = strategy.schedule(workload, config);
        ValidationResult vr = validator.validate(workload, result);

        assertTrue(vr.isValid(), "Result should be valid: " + vr.getErrors());
        
        // Idle 0-2, P1 2-5, Idle 5-8, P2 8-10
        assertEquals(4, result.getExecutionTimeline().size());
        assertEquals(5, result.getAggregateMetrics().getCpuIdleTime());
        assertEquals(5, result.getAggregateMetrics().getCpuBusyTime());
        assertEquals(10, result.getAggregateMetrics().getTotalDuration());
    }
    
    @Test
    void testEmptyWorkload() {
        Workload workload = new Workload(Collections.emptyList());
        ScheduleResult result = strategy.schedule(workload, config);
        ValidationResult vr = validator.validate(workload, result);
        
        assertTrue(vr.isValid());
        assertTrue(result.getExecutionTimeline().isEmpty());
    }

    private ProcessMetrics getMetrics(ScheduleResult result, String pid) {
        return result.getProcessMetrics().stream()
                .filter(m -> m.getProcessId().equals(pid))
                .findFirst()
                .orElseThrow();
    }
}
