package com.orchestrivo.model;

import com.orchestrivo.domain.ProcessSpec;
import com.orchestrivo.domain.Workload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SnapshotVersioningTest {

    @Test
    @DisplayName("Modern snapshot serializes with schemaVersion 1 and computed metadata")
    void testModernSnapshotSerialization() {
        List<ProcessSpec> processes = List.of(
                new ProcessSpec("P1", 0, 10),
                new ProcessSpec("P2", 2, 5),
                new ProcessSpec("P3", 4, 8)
        );
        Workload workload = new Workload(processes);
        long seed = 987654321L;

        String json = WorkloadSnapshot.serialize(workload, seed);
        assertNotNull(json);
        assertTrue(json.contains("\"schemaVersion\":1"));
        assertTrue(json.contains("\"seed\":987654321"));
        assertTrue(json.contains("\"processCount\":3"));
        assertTrue(json.contains("\"totalBurstTime\":23"));
        assertTrue(json.contains("\"minArrival\":0"));
        assertTrue(json.contains("\"maxArrival\":4"));

        WorkloadSnapshot deserialized = WorkloadSnapshot.deserialize(json);
        assertEquals(1, deserialized.getSchemaVersion());
        assertEquals(seed, deserialized.getSeed());
        assertEquals(3, deserialized.getProcessCount());
        assertEquals(23, deserialized.getTotalBurstTime());
        assertEquals(0, deserialized.getMinArrival());
        assertEquals(4, deserialized.getMaxArrival());
        assertEquals(processes, deserialized.getProcesses());
    }

    @Test
    @DisplayName("Legacy unversioned JSON array deserializes gracefully as schema version 1")
    void testLegacyArrayBackwardsCompatibility() {
        String legacyJson = "[{\"processId\":\"P1\",\"arrivalTime\":0,\"burstTime\":15},{\"processId\":\"P2\",\"arrivalTime\":3,\"burstTime\":7}]";

        WorkloadSnapshot snapshot = WorkloadSnapshot.deserialize(legacyJson);
        assertNotNull(snapshot);
        assertEquals(1, snapshot.getSchemaVersion(), "Legacy array should be normalized to schemaVersion 1");
        assertNull(snapshot.getSeed(), "Legacy snapshot has no seed");
        assertEquals(2, snapshot.getProcessCount());
        assertEquals(22, snapshot.getTotalBurstTime());
        assertEquals(0, snapshot.getMinArrival());
        assertEquals(3, snapshot.getMaxArrival());

        List<ProcessSpec> procs = snapshot.getProcesses();
        assertEquals(2, procs.size());
        assertEquals("P1", procs.get(0).getProcessId());
        assertEquals(15, procs.get(0).getBurstTime());
        assertEquals("P2", procs.get(1).getProcessId());
        assertEquals(7, procs.get(1).getBurstTime());
    }

    @Test
    @DisplayName("areSnapshotsEquivalent correctly equates legacy array and modern versioned snapshot")
    void testSnapshotEquivalenceCrossFormat() {
        String legacyJson = "[{\"processId\":\"P1\",\"arrivalTime\":1,\"burstTime\":6}]";
        String modernJson = "{\"schemaVersion\":1,\"seed\":42,\"processCount\":1,\"totalBurstTime\":6,\"minArrival\":1,\"maxArrival\":1,\"processes\":[{\"processId\":\"P1\",\"arrivalTime\":1,\"burstTime\":6}]}";

        assertTrue(WorkloadSnapshot.areSnapshotsEquivalent(legacyJson, modernJson));
        assertTrue(WorkloadSnapshot.areSnapshotsEquivalent(modernJson, legacyJson));
    }

    @Test
    @DisplayName("Rejects malformed or corrupted JSON snapshots")
    void testRejectsCorruptedJson() {
        assertThrows(IllegalArgumentException.class, () -> WorkloadSnapshot.deserialize("not-a-json"));
        assertThrows(IllegalArgumentException.class, () -> WorkloadSnapshot.deserialize("{\"schemaVersion\":1, broken"));
    }

    @Test
    @DisplayName("Rejects snapshot with invalid process invariants (negative arrival, non-positive burst, empty ID)")
    void testRejectsInvalidProcessInvariants() {
        String negativeArrival = "{\"schemaVersion\":1,\"processes\":[{\"processId\":\"P1\",\"arrivalTime\":-5,\"burstTime\":10}]}";
        assertThrows(IllegalArgumentException.class, () -> WorkloadSnapshot.deserialize(negativeArrival));

        String zeroBurst = "{\"schemaVersion\":1,\"processes\":[{\"processId\":\"P1\",\"arrivalTime\":0,\"burstTime\":0}]}";
        assertThrows(IllegalArgumentException.class, () -> WorkloadSnapshot.deserialize(zeroBurst));

        String emptyId = "{\"schemaVersion\":1,\"processes\":[{\"processId\":\"   \",\"arrivalTime\":0,\"burstTime\":5}]}";
        assertThrows(IllegalArgumentException.class, () -> WorkloadSnapshot.deserialize(emptyId));
    }
}
