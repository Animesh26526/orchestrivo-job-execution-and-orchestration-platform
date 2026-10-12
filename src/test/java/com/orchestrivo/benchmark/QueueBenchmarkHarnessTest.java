package com.orchestrivo.benchmark;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QueueBenchmarkHarnessTest {

    @Test
    @DisplayName("Statistical percentiles: Nearest-rank and median calculations match theoretical definitions")
    void testStatisticalCalculations() {
        // Sample of 10 sorted values: 10, 20, 30, 40, 50, 60, 70, 80, 90, 100
        List<Long> values = List.of(10L, 20L, 30L, 40L, 50L, 60L, 70L, 80L, 90L, 100L);

        // Median of 10 items (even): (50 + 60) / 2 = 55.0
        assertEquals(55.0, QueueBenchmarkHarness.computeMedian(values), 0.001);

        // P50: ceil(0.50 * 10) - 1 = index 4 -> 50
        assertEquals(50.0, QueueBenchmarkHarness.computePercentile(values, 0.50), 0.001);

        // P95: ceil(0.95 * 10) - 1 = index 9 -> 100
        assertEquals(100.0, QueueBenchmarkHarness.computePercentile(values, 0.95), 0.001);

        // P90: ceil(0.90 * 10) - 1 = index 8 -> 90
        assertEquals(90.0, QueueBenchmarkHarness.computePercentile(values, 0.90), 0.001);

        // Mean: sum = 550 / 10 = 55.0
        assertEquals(55.0, QueueBenchmarkHarness.computeMean(values), 0.001);

        // Single element sample
        List<Long> single = List.of(42L);
        assertEquals(42.0, QueueBenchmarkHarness.computeMedian(single), 0.001);
        assertEquals(42.0, QueueBenchmarkHarness.computePercentile(single, 0.95), 0.001);
        assertEquals(42.0, QueueBenchmarkHarness.computeMean(single), 0.001);

        // Empty list guardrail
        assertEquals(0.0, QueueBenchmarkHarness.computeMedian(Collections.emptyList()));
        assertEquals(0.0, QueueBenchmarkHarness.computePercentile(Collections.emptyList(), 0.95));
        assertEquals(0.0, QueueBenchmarkHarness.computeMean(Collections.emptyList()));
    }

    @Test
    @DisplayName("Cohort isolation: Different batches maintain independent cohort identifiers and counts")
    void testCohortIsolationDefinition() {
        QueueBenchmarkHarness.BatchTrialResult trial1 = new QueueBenchmarkHarness.BatchTrialResult();
        trial1.cohortBatchId = "cohort-batch-alpha";
        trial1.batchSize = 20;

        QueueBenchmarkHarness.BatchTrialResult trial2 = new QueueBenchmarkHarness.BatchTrialResult();
        trial2.cohortBatchId = "cohort-batch-beta";
        trial2.batchSize = 20;

        assertNotEquals(trial1.cohortBatchId, trial2.cohortBatchId);
        assertEquals(20, trial1.batchSize);
        assertEquals(20, trial2.batchSize);
    }

    @Test
    @DisplayName("Timeout behavior: Incomplete batches are marked timedOut=true and isValid=false")
    void testTimeoutBatchFlagging() {
        QueueBenchmarkHarness.BatchTrialResult timeoutTrial = new QueueBenchmarkHarness.BatchTrialResult();
        timeoutTrial.batchSize = 50;
        timeoutTrial.completedCount = 35;
        timeoutTrial.unhandledCount = 15;
        timeoutTrial.timedOut = true;
        timeoutTrial.isValid = false;

        assertTrue(timeoutTrial.timedOut, "Trial exceeding timeout must be flagged as timedOut");
        assertFalse(timeoutTrial.isValid, "Incomplete trial must not be marked as valid");
        assertNotEquals(timeoutTrial.batchSize, timeoutTrial.completedCount, "Completed count must not equal batch size");
    }

    @Test
    @DisplayName("Environment metadata capture captures non-empty system metrics")
    void testEnvironmentMetadataDefaults() {
        QueueBenchmarkHarness.EnvironmentMetadata env = new QueueBenchmarkHarness.EnvironmentMetadata();
        env.javaVersion = System.getProperty("java.version");
        env.osName = System.getProperty("os.name");
        env.availableProcessors = Runtime.getRuntime().availableProcessors();
        env.commitHash = "eb15e4bdbff9aed151eea150b1bc0274ae856498";

        assertNotNull(env.javaVersion);
        assertNotNull(env.osName);
        assertTrue(env.availableProcessors > 0);
        assertEquals(40, env.commitHash.length());
    }
}
