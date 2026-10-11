package com.orchestrivo.util;

import com.orchestrivo.domain.AggregateMetrics;
import com.orchestrivo.domain.ExecutionSegment;
import com.orchestrivo.domain.ProcessMetrics;
import com.orchestrivo.domain.ProcessSpec;
import com.orchestrivo.domain.ScheduleResult;
import com.orchestrivo.domain.Workload;
import com.orchestrivo.model.ExperimentRunRecord;
import com.orchestrivo.model.WorkloadRecord;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CsvExporterTest {

    @Test
    void testFormulaInjectionMitigationCwe1236() {
        // Values starting with =, +, -, @ must be neutralized with a leading apostrophe
        assertEquals("'=CMD()", CsvExporter.escapeField("=CMD()"));
        assertEquals("'+1234", CsvExporter.escapeField("+1234"));
        assertEquals("'-5678", CsvExporter.escapeField("-5678"));
        assertEquals("'@SUM(A1:A5)", CsvExporter.escapeField("@SUM(A1:A5)"));
    }

    @Test
    void testStandardCsvEscapingQuotesAndCommas() {
        // Commas, double quotes, and newlines must be enclosed in double quotes and inner quotes doubled
        assertEquals("\"Hello, World\"", CsvExporter.escapeField("Hello, World"));
        assertEquals("\"Say \"\"Hello\"\"\"", CsvExporter.escapeField("Say \"Hello\""));
        assertEquals("\"Line1\nLine2\"", CsvExporter.escapeField("Line1\nLine2"));
        assertEquals("", CsvExporter.escapeField(null));
        assertEquals("NormalText", CsvExporter.escapeField("NormalText"));
    }

    @Test
    void testExportWorkload() throws IOException {
        WorkloadRecord record = new WorkloadRecord();
        record.setName("Sample");
        ProcessSpec p1 = new ProcessSpec("P1", 0, 5);
        ProcessSpec p2 = new ProcessSpec("=DANGEROUS", 2, 8);
        record.setWorkload(new Workload(Arrays.asList(p1, p2)));

        StringWriter sw = new StringWriter();
        CsvExporter.exportWorkload(record, sw);
        String output = sw.toString();

        assertTrue(output.startsWith("process_id,arrival_time,burst_time\r\n"));
        assertTrue(output.contains("P1,0,5\r\n"));
        assertTrue(output.contains("'=DANGEROUS,2,8\r\n"), "Formula injection should be sanitized in workload export");
    }

    @Test
    void testExportExperimentRun() throws IOException {
        ExperimentRunRecord run = new ExperimentRunRecord();
        run.setId(42L);
        run.setWorkloadId(10L);
        run.setAlgorithmName("FCFS");
        run.setAlgorithmParameters("{}");
        run.setRunStatus("SUCCEEDED");
        run.setValidationStatus("VALID");
        run.setCreatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));

        ExecutionSegment seg = new ExecutionSegment("P1", 0, 5);
        ProcessMetrics pm = new ProcessMetrics("P1", 0, 5, 5, 0, 0);
        AggregateMetrics am = new AggregateMetrics(0.0, 5.0, 0.0, 5, 5, 0, 0.2, 0);
        ScheduleResult result = new ScheduleResult(List.of(seg), List.of(pm), am, "FCFS");
        run.setResult(result);

        StringWriter sw = new StringWriter();
        CsvExporter.exportExperimentRun(run, sw);
        String output = sw.toString();

        assertTrue(output.contains("# ORCHESTRIVO EXPERIMENT RUN REPORT"));
        assertTrue(output.contains("42,10,FCFS,{},SUCCEEDED,VALID"));
        assertTrue(output.contains("# AGGREGATE METRICS"));
        assertTrue(output.contains("0.0,5.0,0.0,0.2,0,0,5,5"));
        assertTrue(output.contains("# PER PROCESS RESULTS"));
        assertTrue(output.contains("P1,0,5,0,5,0"));
        assertTrue(output.contains("# EXECUTION TIMELINE"));
        assertTrue(output.contains("1,P1,0,5"));
    }

    @Test
    void testExportComparison() throws IOException {
        ExperimentRunRecord run1 = new ExperimentRunRecord();
        run1.setId(1L);
        run1.setAlgorithmName("FCFS");
        run1.setValidationStatus("VALID");
        AggregateMetrics am1 = new AggregateMetrics(2.5, 7.5, 2.5, 15, 15, 0, 0.2, 0);
        run1.setResult(new ScheduleResult(Collections.emptyList(), Collections.emptyList(), am1, "FCFS"));

        ExperimentRunRecord run2 = new ExperimentRunRecord();
        run2.setId(2L);
        run2.setAlgorithmName("SJF");
        run2.setValidationStatus("VALID");
        AggregateMetrics am2 = new AggregateMetrics(1.8, 6.8, 1.8, 15, 15, 0, 0.2, 0);
        run2.setResult(new ScheduleResult(Collections.emptyList(), Collections.emptyList(), am2, "SJF"));

        StringWriter sw = new StringWriter();
        CsvExporter.exportComparison(Arrays.asList(run1, run2), sw);
        String output = sw.toString();

        assertTrue(output.startsWith("run_id,algorithm_name,algorithm_parameters,validation_status"));
        assertTrue(output.contains("1,FCFS,,VALID,2.5,7.5,2.5,0.2,0,0,15"));
        assertTrue(output.contains("2,SJF,,VALID,1.8,6.8,1.8,0.2,0,0,15"));
    }

    @Test
    void testExportBenchmarkRun() throws IOException {
        com.orchestrivo.model.BenchmarkRunRecord bench = new com.orchestrivo.model.BenchmarkRunRecord();
        bench.setId(99L);
        bench.setAlgorithmName("ROUND_ROBIN");
        bench.setProcessCount(50);
        bench.setWarmupIterations(5);
        bench.setMeasuredIterations(10);
        bench.setSeed(42L);
        bench.setCreatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
        bench.setMedianNanos(150_000.0);
        bench.setMinNanos(140_000L);
        bench.setMaxNanos(200_000L);
        bench.setStdDevNanos(15_000.0);
        bench.setMeasurements(List.of(
                new com.orchestrivo.model.BenchmarkMeasurement(1, 140_000L),
                new com.orchestrivo.model.BenchmarkMeasurement(2, 150_000L),
                new com.orchestrivo.model.BenchmarkMeasurement(3, 160_000L)
        ));

        StringWriter sw = new StringWriter();
        CsvExporter.exportBenchmarkRun(bench, sw);
        String output = sw.toString();

        assertTrue(output.contains("# ORCHESTRIVO BENCHMARK RUN REPORT"));
        assertTrue(output.contains("99,ROUND_ROBIN,50,5,10,42"));
        assertTrue(output.contains("# BENCHMARK EXECUTION SUMMARY"));
        assertTrue(output.contains("140000,200000"));
        assertTrue(output.contains("# ITERATION SAMPLES"));
        assertTrue(output.contains("1,140000,0.1400"));
    }
}
