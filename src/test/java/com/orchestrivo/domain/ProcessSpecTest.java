package com.orchestrivo.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProcessSpecTest {

    @Test
    void testValidProcessSpec() {
        ProcessSpec spec = new ProcessSpec("P1", 0, 5);
        assertEquals("P1", spec.getProcessId());
        assertEquals(0, spec.getArrivalTime());
        assertEquals(5, spec.getBurstTime());
    }

    @Test
    void testReservedProcessIdRejected() {
        IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> new ProcessSpec("IDLE", 0, 5));
        assertTrue(e1.getMessage().contains("reserved"));

        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> new ProcessSpec("idle", 0, 5));
        assertTrue(e2.getMessage().contains("reserved"));
        
        IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class, () -> new ProcessSpec("iDlE", 0, 5));
        assertTrue(e3.getMessage().contains("reserved"));
    }

    @Test
    void testInvalidArrivalAndBurstTime() {
        assertThrows(IllegalArgumentException.class, () -> new ProcessSpec("P1", -1, 5));
        assertThrows(IllegalArgumentException.class, () -> new ProcessSpec("P1", 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ProcessSpec("P1", 0, -5));
    }
}
