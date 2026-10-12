package com.orchestrivo.util;

import com.orchestrivo.domain.ProcessSpec;
import com.orchestrivo.model.WorkloadRecord;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class WorkloadGeneratorTest {

    @Test
    void testDeterministicReproducibilityAcrossSameSeed() {
        WorkloadGenerator.GeneratorConfig config1 = new WorkloadGenerator.GeneratorConfig(
                "Synthetic-1", 10, 0, 20, 1, 15, 12345L
        );
        WorkloadGenerator.GeneratorConfig config2 = new WorkloadGenerator.GeneratorConfig(
                "Synthetic-2", 10, 0, 20, 1, 15, 12345L
        );

        List<ProcessSpec> run1 = WorkloadGenerator.generateProcesses(config1);
        List<ProcessSpec> run2 = WorkloadGenerator.generateProcesses(config2);

        assertEquals(run1.size(), run2.size());
        for (int i = 0; i < run1.size(); i++) {
            ProcessSpec p1 = run1.get(i);
            ProcessSpec p2 = run2.get(i);
            assertEquals(p1.getProcessId(), p2.getProcessId());
            assertEquals(p1.getArrivalTime(), p2.getArrivalTime());
            assertEquals(p1.getBurstTime(), p2.getBurstTime());
        }
    }

    @Test
    void testDifferentSeedsProduceDifferentWorkloads() {
        WorkloadGenerator.GeneratorConfig config1 = new WorkloadGenerator.GeneratorConfig(
                "Synthetic-1", 15, 0, 50, 1, 20, 1111L
        );
        WorkloadGenerator.GeneratorConfig config2 = new WorkloadGenerator.GeneratorConfig(
                "Synthetic-2", 15, 0, 50, 1, 20, 9999L
        );

        List<ProcessSpec> run1 = WorkloadGenerator.generateProcesses(config1);
        List<ProcessSpec> run2 = WorkloadGenerator.generateProcesses(config2);

        boolean foundDifference = false;
        for (int i = 0; i < run1.size(); i++) {
            if (run1.get(i).getArrivalTime() != run2.get(i).getArrivalTime() ||
                run1.get(i).getBurstTime() != run2.get(i).getBurstTime()) {
                foundDifference = true;
                break;
            }
        }
        assertTrue(foundDifference, "Different seeds should yield distinct workloads");
    }

    @Test
    void testProcessesRespectRangeBounds() {
        int minArrival = 5;
        int maxArrival = 15;
        int minBurst = 3;
        int maxBurst = 8;
        int count = 50;

        WorkloadGenerator.GeneratorConfig config = new WorkloadGenerator.GeneratorConfig(
                "Bounded", count, minArrival, maxArrival, minBurst, maxBurst, 42L
        );

        List<ProcessSpec> list = WorkloadGenerator.generateProcesses(config);

        assertEquals(count, list.size());
        for (int i = 0; i < list.size(); i++) {
            ProcessSpec p = list.get(i);
            assertEquals("P" + (i + 1), p.getProcessId());
            assertTrue(p.getArrivalTime() >= minArrival && p.getArrivalTime() <= maxArrival,
                    "Arrival time must be in range: " + p.getArrivalTime());
            assertTrue(p.getBurstTime() >= minBurst && p.getBurstTime() <= maxBurst,
                    "Burst time must be in range: " + p.getBurstTime());
        }
    }

    @Test
    void testGenerateWorkloadRecord() {
        WorkloadGenerator.GeneratorConfig config = new WorkloadGenerator.GeneratorConfig(
                "RecordTest", 5, 0, 10, 1, 5, 77L
        );

        WorkloadRecord record = WorkloadGenerator.generateWorkloadRecord(100L, config);

        assertNotNull(record);
        assertEquals("RecordTest", record.getName());
        assertEquals(100L, record.getOwnerUserId());
        assertEquals("GENERATED", record.getSourceType());
        assertEquals(77L, record.getGenerationSeed());
        assertNotNull(record.getWorkload());
        assertEquals(5, record.getWorkload().getProcesses().size());
    }

    @Test
    void testConfigValidationRejectsInvalidBounds() {
        assertThrows(IllegalArgumentException.class, () ->
                WorkloadGenerator.validateConfig(null));

        assertThrows(IllegalArgumentException.class, () ->
                WorkloadGenerator.validateConfig(new WorkloadGenerator.GeneratorConfig("", 5, 0, 10, 1, 5, 1L)));

        // process count < 1
        assertThrows(IllegalArgumentException.class, () ->
                WorkloadGenerator.validateConfig(new WorkloadGenerator.GeneratorConfig("Name", 0, 0, 10, 1, 5, 1L)));

        // process count > 200
        assertThrows(IllegalArgumentException.class, () ->
                WorkloadGenerator.validateConfig(new WorkloadGenerator.GeneratorConfig("Name", 201, 0, 10, 1, 5, 1L)));

        // negative minArrival
        assertThrows(IllegalArgumentException.class, () ->
                WorkloadGenerator.validateConfig(new WorkloadGenerator.GeneratorConfig("Name", 5, -1, 10, 1, 5, 1L)));

        // maxArrival < minArrival
        assertThrows(IllegalArgumentException.class, () ->
                WorkloadGenerator.validateConfig(new WorkloadGenerator.GeneratorConfig("Name", 5, 10, 5, 1, 5, 1L)));

        // minBurst <= 0
        assertThrows(IllegalArgumentException.class, () ->
                WorkloadGenerator.validateConfig(new WorkloadGenerator.GeneratorConfig("Name", 5, 0, 10, 0, 5, 1L)));

        // maxBurst < minBurst
        assertThrows(IllegalArgumentException.class, () ->
                WorkloadGenerator.validateConfig(new WorkloadGenerator.GeneratorConfig("Name", 5, 0, 10, 10, 5, 1L)));
    }
}
