package com.orchestrivo.util;

import com.orchestrivo.domain.AggregateMetrics;
import com.orchestrivo.domain.ExecutionSegment;
import com.orchestrivo.domain.ProcessMetrics;
import com.orchestrivo.domain.ProcessSpec;
import com.orchestrivo.model.ExperimentRunRecord;
import com.orchestrivo.model.WorkloadRecord;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

public class CsvExporter {

    public static String escapeField(Object value) {
        if (value == null) {
            return "";
        }
        String s = value.toString();

        // Mitigation for CSV / Formula Injection (CWE-1236)
        // If the value starts with formula characters, prepend single quote
        if (!s.isEmpty()) {
            char first = s.charAt(0);
            if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r') {
                s = "'" + s;
            }
        }

        // Standard CSV escaping if field contains comma, quote, or newline
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }

        return s;
    }

    public static void exportWorkload(WorkloadRecord record, Writer writer) throws IOException {
        writer.write("process_id,arrival_time,burst_time\r\n");
        if (record != null && record.getWorkload() != null) {
            for (ProcessSpec p : record.getWorkload().getProcesses()) {
                writer.write(escapeField(p.getProcessId()) + ",");
                writer.write(escapeField(p.getArrivalTime()) + ",");
                writer.write(escapeField(p.getBurstTime()) + "\r\n");
            }
        }
        writer.flush();
    }

    public static void exportExperimentRun(ExperimentRunRecord run, Writer writer) throws IOException {
        writer.write("# ORCHESTRIVO EXPERIMENT RUN REPORT\r\n");
        writer.write("run_id,workload_id,algorithm_name,algorithm_parameters,run_status,validation_status,created_at\r\n");
        writer.write(escapeField(run.getId()) + ",");
        writer.write(escapeField(run.getWorkloadId()) + ",");
        writer.write(escapeField(run.getAlgorithmName()) + ",");
        writer.write(escapeField(run.getAlgorithmParameters()) + ",");
        writer.write(escapeField(run.getRunStatus()) + ",");
        writer.write(escapeField(run.getValidationStatus()) + ",");
        writer.write(escapeField(run.getCreatedAt()) + "\r\n\r\n");

        if (run.getResult() != null && run.getResult().getAggregateMetrics() != null) {
            AggregateMetrics am = run.getResult().getAggregateMetrics();
            writer.write("# AGGREGATE METRICS\r\n");
            writer.write("avg_waiting_time,avg_turnaround_time,avg_response_time,simulated_throughput,context_switches,cpu_idle_time,cpu_busy_time,total_duration\r\n");
            writer.write(escapeField(am.getAverageWaitingTime()) + ",");
            writer.write(escapeField(am.getAverageTurnaroundTime()) + ",");
            writer.write(escapeField(am.getAverageResponseTime()) + ",");
            writer.write(escapeField(am.getThroughput()) + ",");
            writer.write(escapeField(am.getContextSwitchCount()) + ",");
            writer.write(escapeField(am.getCpuIdleTime()) + ",");
            writer.write(escapeField(am.getCpuBusyTime()) + ",");
            writer.write(escapeField(am.getTotalDuration()) + "\r\n\r\n");
        }

        if (run.getResult() != null && run.getResult().getProcessMetrics() != null) {
            writer.write("# PER PROCESS RESULTS\r\n");
            writer.write("process_id,first_start_time,completion_time,waiting_time,turnaround_time,response_time\r\n");
            for (ProcessMetrics pm : run.getResult().getProcessMetrics()) {
                writer.write(escapeField(pm.getProcessId()) + ",");
                writer.write(escapeField(pm.getFirstStartTime()) + ",");
                writer.write(escapeField(pm.getCompletionTime()) + ",");
                writer.write(escapeField(pm.getWaitingTime()) + ",");
                writer.write(escapeField(pm.getTurnaroundTime()) + ",");
                writer.write(escapeField(pm.getResponseTime()) + "\r\n");
            }
            writer.write("\r\n");
        }

        if (run.getResult() != null && run.getResult().getExecutionTimeline() != null) {
            writer.write("# EXECUTION TIMELINE\r\n");
            writer.write("segment_sequence,process_id,start_time,end_time\r\n");
            int seq = 1;
            for (ExecutionSegment seg : run.getResult().getExecutionTimeline()) {
                writer.write(escapeField(seq++) + ",");
                writer.write(escapeField(seg.getProcessId()) + ",");
                writer.write(escapeField(seg.getStartTime()) + ",");
                writer.write(escapeField(seg.getEndTime()) + "\r\n");
            }
        }
        writer.flush();
    }

    public static void exportComparison(List<ExperimentRunRecord> runs, Writer writer) throws IOException {
        writer.write("run_id,algorithm_name,algorithm_parameters,validation_status,avg_waiting_time,avg_turnaround_time,avg_response_time,simulated_throughput,context_switches,cpu_idle_time,total_duration\r\n");
        if (runs != null) {
            for (ExperimentRunRecord run : runs) {
                writer.write(escapeField(run.getId()) + ",");
                writer.write(escapeField(run.getAlgorithmName()) + ",");
                writer.write(escapeField(run.getAlgorithmParameters()) + ",");
                writer.write(escapeField(run.getValidationStatus()) + ",");

                if (run.getResult() != null && run.getResult().getAggregateMetrics() != null) {
                    AggregateMetrics am = run.getResult().getAggregateMetrics();
                    writer.write(escapeField(am.getAverageWaitingTime()) + ",");
                    writer.write(escapeField(am.getAverageTurnaroundTime()) + ",");
                    writer.write(escapeField(am.getAverageResponseTime()) + ",");
                    writer.write(escapeField(am.getThroughput()) + ",");
                    writer.write(escapeField(am.getContextSwitchCount()) + ",");
                    writer.write(escapeField(am.getCpuIdleTime()) + ",");
                    writer.write(escapeField(am.getTotalDuration()) + "\r\n");
                } else {
                    writer.write(",,,,,,\r\n");
                }
            }
        }
        writer.flush();
    }

    public static void exportBenchmarkRun(com.orchestrivo.model.BenchmarkRunRecord run, Writer writer) throws IOException {
        writer.write("# ORCHESTRIVO BENCHMARK RUN REPORT\r\n");
        writer.write("id,algorithm_name,process_count,warmup_iterations,measured_iterations,seed,created_at\r\n");
        writer.write(escapeField(run.getId()) + ",");
        writer.write(escapeField(run.getAlgorithmName()) + ",");
        writer.write(escapeField(run.getProcessCount()) + ",");
        writer.write(escapeField(run.getWarmupIterations()) + ",");
        writer.write(escapeField(run.getMeasuredIterations()) + ",");
        writer.write(escapeField(run.getSeed()) + ",");
        writer.write(escapeField(run.getCreatedAt()) + "\r\n\r\n");

        writer.write("# BENCHMARK EXECUTION SUMMARY\r\n");
        writer.write("median_nanos,mean_nanos,min_nanos,max_nanos,p95_nanos,std_dev_nanos,cv_percent,mean_millis\r\n");
        writer.write(escapeField(run.getMedianNanos()) + ",");
        writer.write(escapeField(run.getMeanNanos()) + ",");
        writer.write(escapeField(run.getMinNanos()) + ",");
        writer.write(escapeField(run.getMaxNanos()) + ",");
        writer.write(escapeField(run.getP95Nanos()) + ",");
        writer.write(escapeField(run.getStdDevNanos()) + ",");
        writer.write(escapeField(String.format(java.util.Locale.US, "%.2f", run.getCoefficientOfVariation())) + ",");
        writer.write(escapeField(String.format(java.util.Locale.US, "%.4f", run.getMeanMillis())) + "\r\n\r\n");

        if (run.getMeasurements() != null && !run.getMeasurements().isEmpty()) {
            writer.write("# ITERATION SAMPLES\r\n");
            writer.write("iteration,duration_nanos,duration_millis\r\n");
            for (com.orchestrivo.model.BenchmarkMeasurement m : run.getMeasurements()) {
                writer.write(escapeField(m.getIterationNumber()) + ",");
                writer.write(escapeField(m.getDurationNanos()) + ",");
                writer.write(escapeField(String.format(java.util.Locale.US, "%.4f", m.getDurationMillis())) + "\r\n");
            }
        }
        writer.flush();
    }
}
