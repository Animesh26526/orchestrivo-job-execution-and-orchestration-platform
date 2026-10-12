package com.orchestrivo.service;

import com.orchestrivo.domain.AggregateMetrics;
import com.orchestrivo.domain.ScheduleResult;
import com.orchestrivo.model.ComparisonMode;
import com.orchestrivo.model.ComparisonResult;
import com.orchestrivo.model.ExperimentRunRecord;
import com.orchestrivo.persistence.ExperimentRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExperimentComparisonModesTest {

    private ExperimentRunRepository runRepository;
    private ExperimentService experimentService;

    @BeforeEach
    void setUp() {
        runRepository = mock(ExperimentRunRepository.class);
        experimentService = new ExperimentService(runRepository);
    }

    private ExperimentRunRecord createMockRun(Long id, long userId, long workloadId, String snapshot,
                                              String algo, double avgWait, double avgTat, double tput) {
        ExperimentRunRecord run = new ExperimentRunRecord();
        run.setId(id);
        run.setOwnerUserId(userId);
        run.setWorkloadId(workloadId);
        run.setAlgorithmName(algo);
        run.setInputSnapshot(snapshot);
        run.setRunStatus("SUCCEEDED");
        run.setValidationStatus("VALID");

        AggregateMetrics metrics = new AggregateMetrics(avgWait, avgTat, 5.0, 100, 100, 0, tput, 2);
        run.setResult(new ScheduleResult(Collections.emptyList(), Collections.emptyList(), metrics, algo));
        return run;
    }

    @Test
    @DisplayName("Mode A: Identical snapshots with different algorithms evaluates to MODE_A_CONTROLLED with winner highlights")
    void testModeAControlledComparison() {
        String snapshot = "{\"processes\":[{\"pid\":\"P1\",\"burst\":10}]}";
        ExperimentRunRecord run1 = createMockRun(1L, 10L, 100L, snapshot, "FCFS", 12.0, 22.0, 0.45);
        ExperimentRunRecord run2 = createMockRun(2L, 10L, 100L, snapshot, "SJF", 8.0, 18.0, 0.55);

        when(runRepository.findById(1L)).thenReturn(Optional.of(run1));
        when(runRepository.findById(2L)).thenReturn(Optional.of(run2));

        ComparisonResult result = experimentService.compareExperiments(List.of(1L, 2L), 10L);

        assertEquals(ComparisonMode.MODE_A_CONTROLLED, result.getMode());
        assertTrue(result.isCompatible());
        assertTrue(result.getCompatibilityMessage().contains("Mode A (Controlled)"));
        assertEquals(2L, result.getMinWaitingRunId());
        assertEquals(2L, result.getMinTurnaroundRunId());
        assertEquals(2L, result.getMaxThroughputRunId());

        // Baseline delta for run 1 (reference)
        assertEquals("Baseline (Ref)", result.getWaitDelta(run1));
        // SJF waiting time: 8.0 vs 12.0 -> (8 - 12)/12 = -33.3%
        assertEquals("-33.3%", result.getWaitDelta(run2));
        // SJF throughput: 0.55 vs 0.45 -> (0.55 - 0.45)/0.45 = +22.2%
        assertEquals("+22.2%", result.getThroughputDelta(run2));
    }

    @Test
    @DisplayName("Mode B: Same algorithm evaluated across different workloads evaluates to MODE_B_WORKLOAD_SCALING")
    void testModeBWorkloadScalingComparison() {
        String snapshot1 = "{\"processes\":[{\"pid\":\"P1\",\"burst\":10}]}";
        String snapshot2 = "{\"processes\":[{\"pid\":\"P1\",\"burst\":10},{\"pid\":\"P2\",\"burst\":20}]}";
        ExperimentRunRecord run1 = createMockRun(1L, 10L, 101L, snapshot1, "ROUND_ROBIN", 5.0, 15.0, 0.6);
        ExperimentRunRecord run2 = createMockRun(2L, 10L, 102L, snapshot2, "ROUND_ROBIN", 12.0, 28.0, 0.3);

        when(runRepository.findById(1L)).thenReturn(Optional.of(run1));
        when(runRepository.findById(2L)).thenReturn(Optional.of(run2));

        ComparisonResult result = experimentService.compareExperiments(List.of(1L, 2L), 10L);

        assertEquals(ComparisonMode.MODE_B_WORKLOAD_SCALING, result.getMode());
        assertTrue(result.isCompatible());
        assertTrue(result.getCompatibilityMessage().contains("Mode B (Workload Scaling)"));
        assertEquals("+140.0%", result.getWaitDelta(run2));
    }

    @Test
    @DisplayName("Mode C: Differing algorithms and differing workloads evaluates to MODE_C_EXPLORATORY")
    void testModeCExploratoryComparison() {
        String snapshot1 = "{\"processes\":[{\"pid\":\"P1\",\"burst\":5}]}";
        String snapshot2 = "{\"processes\":[{\"pid\":\"P1\",\"burst\":50}]}";
        ExperimentRunRecord run1 = createMockRun(1L, 10L, 101L, snapshot1, "FCFS", 5.0, 10.0, 0.8);
        ExperimentRunRecord run2 = createMockRun(2L, 10L, 102L, snapshot2, "SRTF", 25.0, 45.0, 0.2);

        when(runRepository.findById(1L)).thenReturn(Optional.of(run1));
        when(runRepository.findById(2L)).thenReturn(Optional.of(run2));

        ComparisonResult result = experimentService.compareExperiments(List.of(1L, 2L), 10L);

        assertEquals(ComparisonMode.MODE_C_EXPLORATORY, result.getMode());
        assertFalse(result.isCompatible());
        assertTrue(result.getCompatibilityMessage().contains("Incompatible workloads"));
        assertTrue(result.getCompatibilityMessage().contains("Mode C (Exploratory)"));
    }

    @Test
    @DisplayName("formatDelta handles parity, zero baseline, and NaN safely")
    void testDeltaEdgeCases() {
        assertEquals("0.0% (parity)", ComparisonResult.formatDelta(10.0, 10.0, false));
        assertEquals("0.0% (parity)", ComparisonResult.formatDelta(10.002, 10.0, false));
        assertEquals("+10.0%", ComparisonResult.formatDelta(11.0, 10.0, true));
        assertEquals("-20.0%", ComparisonResult.formatDelta(8.0, 10.0, false));
        assertEquals("0.0%", ComparisonResult.formatDelta(0.0, 0.0, true));
        assertEquals("+N/A", ComparisonResult.formatDelta(5.0, 0.0, true));
        assertEquals("N/A", ComparisonResult.formatDelta(Double.NaN, 10.0, true));
    }
}
