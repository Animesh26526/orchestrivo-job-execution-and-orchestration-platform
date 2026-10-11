package com.orchestrivo.validation;

import com.orchestrivo.domain.AlgorithmType;
import com.orchestrivo.service.BenchmarkService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class BenchmarkValidationTest {

    private final BenchmarkService service = new BenchmarkService();

    @Test
    @DisplayName("Valid normal interior configuration passes validation without exception")
    void testValidInteriorConfiguration() {
        assertDoesNotThrow(() ->
                service.validateConfig(AlgorithmType.ROUND_ROBIN, 50, 10, 50, 4)
        );
        assertDoesNotThrow(() ->
                service.validateConfig(AlgorithmType.FCFS, 100, 5, 20, null)
        );
    }

    @Test
    @DisplayName("Exact minimum boundary values pass validation")
    void testExactMinimumBoundaries() {
        assertDoesNotThrow(() ->
                service.validateConfig(
                        AlgorithmType.ROUND_ROBIN,
                        BenchmarkService.MIN_PROCESS_COUNT,
                        BenchmarkService.MIN_WARMUP,
                        BenchmarkService.MIN_REPETITIONS,
                        BenchmarkService.MIN_QUANTUM
                )
        );
    }

    @Test
    @DisplayName("Exact maximum boundary values pass validation")
    void testExactMaximumBoundaries() {
        assertDoesNotThrow(() ->
                service.validateConfig(
                        AlgorithmType.ROUND_ROBIN,
                        BenchmarkService.MAX_PROCESS_COUNT,
                        BenchmarkService.MAX_WARMUP,
                        BenchmarkService.MAX_REPETITIONS,
                        BenchmarkService.MAX_QUANTUM
                )
        );
    }

    @Test
    @DisplayName("Process count one below minimum fails with actionable message")
    void testProcessCountOneBelowMinimum() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.validateConfig(AlgorithmType.FCFS, 0, 5, 10, null)
        );
        assertTrue(ex.getMessage().contains("Process count must be between 1 and 500"), ex.getMessage());
    }

    @Test
    @DisplayName("Process count one above maximum fails with actionable message")
    void testProcessCountOneAboveMaximum() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.validateConfig(AlgorithmType.FCFS, 501, 5, 10, null)
        );
        assertTrue(ex.getMessage().contains("Process count must be between 1 and 500"), ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {-10, -1, 502, 1000, 100_000})
    @DisplayName("Invalid process counts fail safely")
    void testInvalidProcessCounts(int invalidCount) {
        assertThrows(IllegalArgumentException.class, () ->
                service.validateConfig(AlgorithmType.FCFS, invalidCount, 5, 10, null)
        );
    }

    @Test
    @DisplayName("Warmup iterations one below minimum fails with actionable message")
    void testWarmupOneBelowMinimum() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.validateConfig(AlgorithmType.FCFS, 10, 0, 10, null)
        );
        assertTrue(ex.getMessage().contains("Warm-up iterations must be between 1 and 100"), ex.getMessage());
    }

    @Test
    @DisplayName("Warmup iterations one above maximum fails with actionable message")
    void testWarmupOneAboveMaximum() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.validateConfig(AlgorithmType.FCFS, 10, 101, 10, null)
        );
        assertTrue(ex.getMessage().contains("Warm-up iterations must be between 1 and 100"), ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {-5, 0, 102, 500, 10_000})
    @DisplayName("Invalid warmup iterations fail safely")
    void testInvalidWarmupIterations(int invalidWarmup) {
        assertThrows(IllegalArgumentException.class, () ->
                service.validateConfig(AlgorithmType.FCFS, 10, invalidWarmup, 10, null)
        );
    }

    @Test
    @DisplayName("Measured iterations one below minimum fails with actionable message")
    void testMeasuredOneBelowMinimum() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.validateConfig(AlgorithmType.FCFS, 10, 5, 0, null)
        );
        assertTrue(ex.getMessage().contains("Measured iterations must be between 1 and 1000"), ex.getMessage());
    }

    @Test
    @DisplayName("Measured iterations one above maximum fails with actionable message")
    void testMeasuredOneAboveMaximum() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.validateConfig(AlgorithmType.FCFS, 10, 5, 1001, null)
        );
        assertTrue(ex.getMessage().contains("Measured iterations must be between 1 and 1000"), ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {-10, 0, 1002, 5000, 50_000})
    @DisplayName("Invalid measured iterations fail safely")
    void testInvalidMeasuredIterations(int invalidReps) {
        assertThrows(IllegalArgumentException.class, () ->
                service.validateConfig(AlgorithmType.FCFS, 10, 5, invalidReps, null)
        );
    }

    @Test
    @DisplayName("Round Robin quantum null fails with actionable message")
    void testRoundRobinNullQuantum() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.validateConfig(AlgorithmType.ROUND_ROBIN, 10, 5, 10, null)
        );
        assertTrue(ex.getMessage().contains("Time quantum is required"), ex.getMessage());
    }

    @Test
    @DisplayName("Round Robin quantum one below minimum fails with actionable message")
    void testRoundRobinQuantumOneBelowMinimum() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.validateConfig(AlgorithmType.ROUND_ROBIN, 10, 5, 10, 0)
        );
        assertTrue(ex.getMessage().contains("Time quantum must be between 1 and 100"), ex.getMessage());
    }

    @Test
    @DisplayName("Round Robin quantum one above maximum fails with actionable message")
    void testRoundRobinQuantumOneAboveMaximum() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.validateConfig(AlgorithmType.ROUND_ROBIN, 10, 5, 10, 101)
        );
        assertTrue(ex.getMessage().contains("Time quantum must be between 1 and 100"), ex.getMessage());
    }

    @Test
    @DisplayName("Null algorithm type fails with actionable message")
    void testNullAlgorithmType() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.validateConfig(null, 10, 5, 10, null)
        );
        assertTrue(ex.getMessage().contains("Algorithm must be specified"), ex.getMessage());
    }
}
