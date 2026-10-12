package com.orchestrivo.validation;

import com.orchestrivo.domain.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduleValidatorTest {
    private final ScheduleValidator validator = new ScheduleValidator();

    @Test
    void testValidSchedule() {
        Workload workload = new Workload(Collections.singletonList(
                new ProcessSpec("P1", 0, 5)
        ));

        List<ExecutionSegment> timeline = Collections.singletonList(
                new ExecutionSegment("P1", 0, 5)
        );

        List<ProcessMetrics> metrics = Collections.singletonList(
                new ProcessMetrics("P1", 0, 5, 5, 0, 0)
        );

        AggregateMetrics agg = new AggregateMetrics(0, 5, 0, 5, 5, 0, 0.2, 0); // Context switches from null to P1 is 0
        ScheduleResult result = new ScheduleResult(timeline, metrics, agg, "TEST");

        ValidationResult vr = validator.validate(workload, result);
        assertTrue(vr.isValid(), "Should be valid: " + vr.getErrors());
    }

    @Test
    void testInvalidExecutionBeforeArrival() {
        Workload workload = new Workload(Collections.singletonList(
                new ProcessSpec("P1", 2, 5)
        ));

        List<ExecutionSegment> timeline = Collections.singletonList(
                new ExecutionSegment("P1", 0, 5)
        );
        List<ProcessMetrics> metrics = Collections.singletonList(
                new ProcessMetrics("P1", 0, 5, 3, -2, -2) 
        );
        ScheduleResult result = new ScheduleResult(timeline, metrics, null, "TEST");

        ValidationResult vr = validator.validate(workload, result);
        assertFalse(vr.isValid());
        assertTrue(vr.getErrors().stream().anyMatch(e -> e.contains("executed before its arrival time")));
    }
    
    @Test
    void testInvalidBurstTime() {
        Workload workload = new Workload(Collections.singletonList(
                new ProcessSpec("P1", 0, 5)
        ));

        List<ExecutionSegment> timeline = Collections.singletonList(
                new ExecutionSegment("P1", 0, 4)
        );
        List<ProcessMetrics> metrics = Collections.singletonList(
                new ProcessMetrics("P1", 0, 4, 4, -1, 0)
        );
        ScheduleResult result = new ScheduleResult(timeline, metrics, null, "TEST");

        ValidationResult vr = validator.validate(workload, result);
        assertFalse(vr.isValid());
        assertTrue(vr.getErrors().stream().anyMatch(e -> e.contains("expected 5")));
    }

    @Test
    void testMissingMetrics() {
        Workload workload = new Workload(Collections.singletonList(
                new ProcessSpec("P1", 0, 5)
        ));

        List<ExecutionSegment> timeline = Collections.singletonList(
                new ExecutionSegment("P1", 0, 5)
        );
        List<ProcessMetrics> metrics = Collections.emptyList(); // Missing
        
        ScheduleResult result = new ScheduleResult(timeline, metrics, null, "TEST");

        ValidationResult vr = validator.validate(workload, result);
        assertFalse(vr.isValid());
        assertTrue(vr.getErrors().stream().anyMatch(e -> e.contains("does not match workload size")));
    }

    @Test
    void testAggregateMetricsIncorrect() {
        Workload workload = new Workload(Collections.singletonList(
                new ProcessSpec("P1", 0, 5)
        ));

        List<ExecutionSegment> timeline = Collections.singletonList(
                new ExecutionSegment("P1", 0, 5)
        );

        List<ProcessMetrics> metrics = Collections.singletonList(
                new ProcessMetrics("P1", 0, 5, 5, 0, 0)
        );

        // Incorrect aggregate metrics
        AggregateMetrics agg = new AggregateMetrics(99, 5, 0, 5, 5, 0, 0.2, 0); 
        ScheduleResult result = new ScheduleResult(timeline, metrics, agg, "TEST");

        ValidationResult vr = validator.validate(workload, result);
        assertFalse(vr.isValid());
        assertTrue(vr.getErrors().stream().anyMatch(e -> e.contains("Aggregate average waiting time incorrect")));
    }

    @Test
    void testOverlappingSegments() {
        Workload workload = new Workload(Arrays.asList(
                new ProcessSpec("P1", 0, 5),
                new ProcessSpec("P2", 1, 5)
        ));

        // Overlapping segments
        List<ExecutionSegment> timeline = Arrays.asList(
                new ExecutionSegment("P1", 0, 5),
                new ExecutionSegment("P2", 3, 8) 
        );

        List<ProcessMetrics> metrics = Arrays.asList(
                new ProcessMetrics("P1", 0, 5, 5, 0, 0),
                new ProcessMetrics("P2", 3, 8, 7, 2, 2)
        );

        AggregateMetrics agg = new AggregateMetrics(1, 6, 1, 8, 10, 0, 2.0/8.0, 1);
        ScheduleResult result = new ScheduleResult(timeline, metrics, agg, "TEST");

        ValidationResult vr = validator.validate(workload, result);
        assertFalse(vr.isValid());
        assertTrue(vr.getErrors().stream().anyMatch(e -> e.contains("Overlap detected")));
    }
}
