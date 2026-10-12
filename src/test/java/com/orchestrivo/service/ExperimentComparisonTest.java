package com.orchestrivo.service;

import com.orchestrivo.domain.AggregateMetrics;
import com.orchestrivo.domain.ScheduleResult;
import com.orchestrivo.model.ComparisonResult;
import com.orchestrivo.model.ExperimentRunRecord;
import com.orchestrivo.persistence.ExperimentRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ExperimentComparisonTest {

    private ExperimentRunRepository runRepository;
    private ExperimentService experimentService;

    @BeforeEach
    void setUp() {
        runRepository = mock(ExperimentRunRepository.class);
        experimentService = new ExperimentService(runRepository);
    }

    private ExperimentRunRecord createRun(long id, long userId, String algo, String snapshot, double wait, double tat, double tput, int cs) {
        ExperimentRunRecord run = new ExperimentRunRecord();
        run.setId(id);
        run.setOwnerUserId(userId);
        run.setWorkloadId(1L);
        run.setAlgorithmName(algo);
        run.setInputSnapshot(snapshot);
        run.setRunStatus("SUCCEEDED");
        run.setValidationStatus("VALID");

        AggregateMetrics am = new AggregateMetrics(wait, tat, wait, 20, 20, 0, tput, cs);
        run.setResult(new ScheduleResult(Collections.emptyList(), Collections.emptyList(), am, algo));
        return run;
    }

    @Test
    void testCompareCompatibleRunsWithHighlights() {
        String snapshot = "[{\"processId\":\"P1\",\"arrivalTime\":0,\"burstTime\":5}]";
        ExperimentRunRecord run1 = createRun(1L, 10L, "FCFS", snapshot, 4.0, 9.0, 0.2, 0);
        ExperimentRunRecord run2 = createRun(2L, 10L, "SJF", snapshot, 2.5, 7.5, 0.3, 1);

        when(runRepository.findById(1L)).thenReturn(Optional.of(run1));
        when(runRepository.findById(2L)).thenReturn(Optional.of(run2));

        ComparisonResult result = experimentService.compareExperiments(Arrays.asList(1L, 2L), 10L);

        assertNotNull(result);
        assertTrue(result.isCompatible(), "Identical snapshots should be compatible");
        assertEquals(2, result.getRuns().size());
        assertEquals(2L, result.getMinWaitingRunId(), "SJF should have minimum waiting time");
        assertEquals(2L, result.getMinTurnaroundRunId(), "SJF should have minimum turnaround time");
        assertEquals(2L, result.getMaxThroughputRunId(), "SJF should have maximum throughput");
        assertEquals(1L, result.getMinContextSwitchRunId(), "FCFS should have minimum context switches");
        assertNotNull(result.getTradeOffNotes());
        assertTrue(result.getTradeOffNotes().contains("Trade-Off"));
    }

    @Test
    void testCompareIncompatibleSnapshotsFlagged() {
        String snap1 = "[{\"processId\":\"P1\",\"arrivalTime\":0,\"burstTime\":5}]";
        String snap2 = "[{\"processId\":\"P1\",\"arrivalTime\":0,\"burstTime\":10}]"; // Different burst!

        ExperimentRunRecord run1 = createRun(1L, 10L, "FCFS", snap1, 4.0, 9.0, 0.2, 0);
        ExperimentRunRecord run2 = createRun(2L, 10L, "SJF", snap2, 2.5, 7.5, 0.3, 1);

        when(runRepository.findById(1L)).thenReturn(Optional.of(run1));
        when(runRepository.findById(2L)).thenReturn(Optional.of(run2));

        ComparisonResult result = experimentService.compareExperiments(Arrays.asList(1L, 2L), 10L);

        assertNotNull(result);
        assertFalse(result.isCompatible(), "Different workload inputs must be flagged as incompatible");
        assertTrue(result.getCompatibilityMessage().contains("Incompatible workloads"));
    }

    @Test
    void testCompareExceedingMaxRunsLimit() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L);
        assertThrows(IllegalArgumentException.class, () ->
                experimentService.compareExperiments(ids, 10L));
    }

    @Test
    void testCompareEmptyRunsList() {
        assertThrows(IllegalArgumentException.class, () ->
                experimentService.compareExperiments(Collections.emptyList(), 10L));
    }

    @Test
    void testCompareUnauthorizedUser() {
        String snapshot = "[]";
        ExperimentRunRecord run1 = createRun(1L, 999L, "FCFS", snapshot, 4.0, 9.0, 0.2, 0); // owned by 999
        when(runRepository.findById(1L)).thenReturn(Optional.of(run1));

        assertThrows(SecurityException.class, () ->
                experimentService.compareExperiments(List.of(1L), 10L));
    }
}
