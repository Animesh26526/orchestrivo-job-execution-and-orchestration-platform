package com.orchestrivo.model;

import com.orchestrivo.model.MetricDefinition.Direction;

import java.util.*;

/**
 * Universal metrics catalog centralizing definitions, standard formulas,
 * directionality, and timing boundaries across Orchestrivo.
 */
public final class MetricsCatalog {

    private static final Map<String, MetricDefinition> DEFINITIONS = new LinkedHashMap<>();

    static {
        // --- 1. Scheduling Algorithm Theoretical Metrics ---
        register(new MetricDefinition(
                "avg_waiting_time",
                "Mean Waiting Time",
                "SCHEDULING_ALGORITHM",
                "simulation ticks",
                "Average(turnaround_time - burst_time) = Sum(WT) / N",
                Direction.LOWER_IS_BETTER,
                "FCFS, SJF, SRTF, ROUND_ROBIN",
                "Schedule timeline analysis (MetricsCalculator)",
                "Simulation timeline from first process start to completion",
                "Applies to simulated discrete clock ticks, not host JVM execution time."
        ));

        register(new MetricDefinition(
                "avg_turnaround_time",
                "Mean Turnaround Time",
                "SCHEDULING_ALGORITHM",
                "simulation ticks",
                "Average(completion_time - arrival_time) = Sum(TAT) / N",
                Direction.LOWER_IS_BETTER,
                "FCFS, SJF, SRTF, ROUND_ROBIN",
                "Schedule timeline analysis (MetricsCalculator)",
                "From process arrival to final completion in simulation ticks",
                "Heavily influenced by long bursts arriving early (convoy effect)."
        ));

        register(new MetricDefinition(
                "avg_response_time",
                "Mean Response Time",
                "SCHEDULING_ALGORITHM",
                "simulation ticks",
                "Average(first_start_time - arrival_time) = Sum(RT) / N",
                Direction.LOWER_IS_BETTER,
                "FCFS, SJF, SRTF, ROUND_ROBIN",
                "Schedule timeline analysis (MetricsCalculator)",
                "From arrival time to initial CPU allocation segment",
                "Critical for interactive workloads; FCFS and non-preemptive SJF show poor response time under high burst variance."
        ));

        register(new MetricDefinition(
                "total_duration",
                "Makespan (Total Duration)",
                "SCHEDULING_ALGORITHM",
                "simulation ticks",
                "max(timeline.end_time) - min(timeline.start_time)",
                Direction.LOWER_IS_BETTER,
                "FCFS, SJF, SRTF, ROUND_ROBIN",
                "Timeline span",
                "Simulation interval from start to last process completion",
                "Equal across non-preemptive algorithms if CPU is fully occupied without idle bubbles."
        ));

        register(new MetricDefinition(
                "context_switches",
                "Context Switches",
                "SCHEDULING_ALGORITHM",
                "switches",
                "Count of distinct non-idle process transitions",
                Direction.LOWER_IS_BETTER,
                "Preemptive: SRTF, ROUND_ROBIN",
                "Timeline boundary transitions",
                "Full simulation timeline",
                "In real operating systems each switch incurs register saving, cache invalidation, and kernel overhead."
        ));

        register(new MetricDefinition(
                "cpu_utilization",
                "CPU Utilization",
                "SCHEDULING_ALGORITHM",
                "percent (%)",
                "(cpu_busy_time / total_duration) * 100",
                Direction.HIGHER_IS_BETTER,
                "All algorithms",
                "Sum of non-idle segment durations vs total duration",
                "Simulation timeline",
                "Does not reflect host machine CPU load; models simulated CPU occupancy."
        ));

        register(new MetricDefinition(
                "simulated_throughput",
                "Simulated Scheduling Throughput",
                "SCHEDULING_ALGORITHM",
                "processes / tick",
                "completed_processes / total_duration",
                Direction.HIGHER_IS_BETTER,
                "All algorithms",
                "Completed processes divided by simulation makespan",
                "Simulation timeline",
                "Theoretical simulation throughput. NOT host Java execution throughput."
        ));

        // --- 2. Host Micro-Benchmark Implementation Metrics ---
        register(new MetricDefinition(
                "median_latency",
                "Median Algorithm Latency (P50)",
                "BENCHMARK_IMPLEMENTATION",
                "nanoseconds (ns) / microseconds (µs)",
                "P50 of sorted measured iteration durations",
                Direction.LOWER_IS_BETTER,
                "All scheduler implementations",
                "System.nanoTime() around strategy.schedule(...)",
                "Algorithm-only execution; strictly excludes warmup iterations and workload generation",
                "Robust against outlier pauses (e.g., GC pauses, OS thread context switches)."
        ));

        register(new MetricDefinition(
                "p95_latency",
                "95th Percentile Latency (P95)",
                "BENCHMARK_IMPLEMENTATION",
                "nanoseconds (ns) / microseconds (µs)",
                "Rank ceil(0.95 * N) in sorted sample list",
                Direction.LOWER_IS_BETTER,
                "All scheduler implementations",
                "System.nanoTime() iteration durations",
                "Measured sample window",
                "Only statistically meaningful when sample size N >= 20. Flagged as small sample when N < 20."
        ));

        register(new MetricDefinition(
                "mean_latency",
                "Mean Latency",
                "BENCHMARK_IMPLEMENTATION",
                "nanoseconds (ns) / microseconds (µs)",
                "Sum(duration_nanos) / N_measured",
                Direction.LOWER_IS_BETTER,
                "All scheduler implementations",
                "System.nanoTime() per-run measurements",
                "Measured sample window",
                "Sensitive to extreme tail outliers compared to median."
        ));

        register(new MetricDefinition(
                "std_dev_latency",
                "Latency Standard Deviation",
                "BENCHMARK_IMPLEMENTATION",
                "nanoseconds (ns) / microseconds (µs)",
                "sqrt(Sum((x - mean)^2) / (N - 1))",
                Direction.LOWER_IS_BETTER,
                "All scheduler implementations",
                "Per-iteration sample deviations",
                "Measured sample window",
                "Quantifies execution jitter and HotSpot JIT stability."
        ));

        register(new MetricDefinition(
                "cv_percent",
                "Coefficient of Variation (CV%)",
                "BENCHMARK_IMPLEMENTATION",
                "percent (%)",
                "(std_dev / mean) * 100",
                Direction.LOWER_IS_BETTER,
                "All scheduler implementations",
                "Relative variability calculation",
                "Measured sample window",
                "Values < 10% indicate high measurement stability and low environmental noise."
        ));

        register(new MetricDefinition(
                "actual_batch_throughput",
                "Actual Batch Wall-Clock Throughput",
                "BENCHMARK_IMPLEMENTATION",
                "runs / sec",
                "N_valid / (batch_elapsed_nanos / 1e9)",
                Direction.HIGHER_IS_BETTER,
                "Benchmark batch executions",
                "Wall-clock batch timer (System.nanoTime() covering measured iteration loop)",
                "Entire measured batch loop duration",
                "True empirical rate of completed scheduling simulations per real second on the host."
        ));

        register(new MetricDefinition(
                "latency_derived_throughput_estimate",
                "Latency-Derived Throughput Estimate",
                "BENCHMARK_IMPLEMENTATION",
                "runs / sec (estimate)",
                "1e9 / median_nanos",
                Direction.HIGHER_IS_BETTER,
                "Synthetic reciprocal estimate",
                "Mathematical reciprocal of median latency",
                "Single iteration median",
                "Theoretical upper bound reciprocal. Excludes batch loop overhead, validation, and cache effects."
        ));

        register(new MetricDefinition(
                "speedup_ratio",
                "Latency Speedup Ratio",
                "BENCHMARK_IMPLEMENTATION",
                "ratio (x)",
                "baseline_latency / candidate_latency",
                Direction.HIGHER_IS_BETTER,
                "Pairwise A/B comparisons",
                "Ratio of median latencies",
                "Direct pairwise comparison",
                "Values > 1.00x represent candidate speedup; < 1.00x represent regression."
        ));

        // --- 3. Operational Queue & Persistence Metrics ---
        register(new MetricDefinition(
                "queue_wait_ms",
                "Queue Wait Time",
                "OPERATIONAL_QUEUE",
                "milliseconds (ms)",
                "TIMESTAMPDIFF(MICROSECOND, created_at, started_at) / 1000",
                Direction.LOWER_IS_BETTER,
                "Asynchronous execution jobs",
                "Database timestamps (created_at to started_at)",
                "From HTTP submission to background worker claim",
                "Measures queue buffering delay before execution begins."
        ));

        register(new MetricDefinition(
                "execution_duration_ms",
                "Job Execution Duration",
                "OPERATIONAL_QUEUE",
                "milliseconds (ms) / microseconds (µs)",
                "execution_duration_nanos / 1e6",
                Direction.LOWER_IS_BETTER,
                "Asynchronous execution jobs",
                "Worker thread System.nanoTime() execution block",
                "From worker job dispatch to validation and database persistence",
                "Includes scheduling calculation, ScheduleValidator audit, and result serialization."
        ));

        register(new MetricDefinition(
                "queue_depth",
                "Queue Depth (Pending Jobs)",
                "OPERATIONAL_QUEUE",
                "jobs",
                "Count of jobs where status = 'QUEUED'",
                Direction.LOWER_IS_BETTER,
                "Persistent queue telemetry",
                "Database execution_jobs table query",
                "Instantaneous point-in-time snapshot",
                "Reflects current backlog pending worker pickup."
        ));
    }

    private MetricsCatalog() {}

    private static void register(MetricDefinition def) {
        DEFINITIONS.put(def.getKey(), def);
    }

    public static Optional<MetricDefinition> get(String key) {
        if (key == null) return Optional.empty();
        return Optional.ofNullable(DEFINITIONS.get(key.trim().toLowerCase()));
    }

    public static List<MetricDefinition> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(DEFINITIONS.values()));
    }

    public static List<MetricDefinition> getByCategory(String category) {
        if (category == null) return Collections.emptyList();
        List<MetricDefinition> list = new ArrayList<>();
        for (MetricDefinition d : DEFINITIONS.values()) {
            if (category.equalsIgnoreCase(d.getCategory())) {
                list.add(d);
            }
        }
        return Collections.unmodifiableList(list);
    }

    /**
     * Standard percentage delta formula:
     * - Lower is better: Relative Reduction (%) = ((Baseline - Candidate) / Baseline) * 100
     * - Higher is better: Relative Improvement (%) = ((Candidate - Baseline) / Baseline) * 100
     * Returns 0.0 when baseline is 0 or NaN/Infinite.
     */
    public static double computeRelativeDeltaPct(double baseline, double candidate, boolean higherIsBetter) {
        if (Double.isNaN(baseline) || Double.isNaN(candidate) || Double.isInfinite(baseline) || Double.isInfinite(candidate)) {
            return 0.0;
        }
        if (Math.abs(baseline) < 1e-9) {
            return 0.0;
        }
        if (higherIsBetter) {
            return ((candidate - baseline) / baseline) * 100.0;
        } else {
            return ((baseline - candidate) / baseline) * 100.0;
        }
    }

    /**
     * Computes speedup ratio: Baseline / Candidate with division-by-zero protection.
     */
    public static double computeSpeedup(double baselineLatency, double candidateLatency) {
        if (candidateLatency <= 1e-9 || baselineLatency <= 1e-9) {
            return 1.0;
        }
        return baselineLatency / candidateLatency;
    }
}
