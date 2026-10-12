package com.orchestrivo.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExecutionSegmentTest {

    @Test
    void testIsIdleCaseInsensitive() {
        ExecutionSegment s1 = new ExecutionSegment("IDLE", 0, 5);
        assertTrue(s1.isIdle());

        ExecutionSegment s2 = new ExecutionSegment("idle", 5, 10);
        assertTrue(s2.isIdle());
        
        ExecutionSegment s3 = new ExecutionSegment("IdLe", 10, 15);
        assertTrue(s3.isIdle());

        ExecutionSegment s4 = new ExecutionSegment("P1", 15, 20);
        assertFalse(s4.isIdle());
    }
}
