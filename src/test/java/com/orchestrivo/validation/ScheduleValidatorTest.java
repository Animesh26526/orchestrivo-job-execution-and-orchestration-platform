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

        AggregateMetrics agg = new AggregateMetrics(0, 5, 0, 5, 5, 0, 0.2, 1);
        ScheduleResult result = new ScheduleResult(timeline, metrics, agg, "TEST");

        ValidationResult vr = validator.validate(workload, result);
        assertTrue(vr.isValid(), "Should be valid");
    }

    @Test
    void testInvalidExecutionBeforeArrival() {
        Workload workload = new Workload(Collections.singletonList(
                new ProcessSpec("P1", 2, 5)
        ));

        // P1 runs at 0 but arrives at 2
        List<ExecutionSegment> timeline = Collections.singletonList(
                new ExecutionSegment("P1", 0, 5)
        );
        List<ProcessMetrics> metrics = Collections.singletonList(
                new ProcessMetrics("P1", 0, 5, 3, -2, -2) // bogus metrics
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

        // P1 runs for 4 units instead of 5
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
}
