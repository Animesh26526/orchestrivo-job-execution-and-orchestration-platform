# Orchestrivo Benchmark Methodology

## 1. Scientific Philosophy and Protocol

Micro-benchmarking CPU scheduling algorithms on a managed, dynamic runtime like the Java Virtual Machine (JVM) presents subtle measurement challenges. Naive approaches often mistake JIT compilation artifacts, garbage collection pauses, or database transaction latencies for algorithmic performance.

Orchestrivo enforces a rigorous, scientific benchmarking protocol designed to measure pure algorithmic performance while exposing realistic system trade-offs.

---

## 2. Measurement Boundary Definition

To prevent external I/O and runtime overheads from contaminating algorithm performance metrics, Orchestrivo enforces a strict measurement boundary:

### 2.1 Excluded from the Timing Boundary (Pre- & Post-Processing)
- **Workload Generation**: Deterministic pseudo-random process generation (`WorkloadGenerator.generateProcesses(...)`) occurs prior to entering the benchmark harness.
- **JSON Serialization & Parsing**: Workload snapshots and process JSON representations are fully parsed and initialized into memory before timing begins.
- **Database Operations**: HikariCP connection acquisition, JDBC query execution, and persistence of benchmark runs occur strictly after all measured iterations complete.
- **HTTP / Servlet Lifecycle**: Request parameter validation, session authentication, and JSP rendering are completely isolated from benchmark timings.

### 2.2 Included in the Timing Boundary
- **Pure Algorithm Execution**: The invocation of `strategy.schedule(workload, schedulingConfig)` is the sole logic inside the timing loop.
- **Gantt Segment Construction & Schedule Validation**: State transitions, ready queue maintenance, preemption checks, and completion invariant verifications executed by the scheduling engine.

```text
[ Pre-Benchmark ]
  ├── 1. Generate ProcessSpec instances deterministically (Seed S)
  ├── 2. Allocate in-memory Workload & SchedulingConfig
  └── 3. Warm-up Phase (Unmeasured iterations W)
           ▼
[ Measured Boundary ] ─── (Repeated for N iterations)
  ┌────────────────────────────────────────────────────────┐
  │  start = System.nanoTime()                             │
  │  result = strategy.schedule(workload, config)          │
  │  duration = System.nanoTime() - start                  │
  │  recordSample(duration)                                │
  └────────────────────────────────────────────────────────┘
           ▼
[ Post-Benchmark ]
  ├── 1. Statistical Aggregation (Median, Mean, Min, Max, StdDev, Throughput)
  ├── 2. Invariant Validation (Zero overlapping CPU slices, work conservation)
  └── 3. Persistent Commit to MySQL (benchmark_runs & benchmark_measurements)
```

---

## 3. High-Resolution Timing & Clock Sources

Orchestrivo leverages `System.nanoTime()` for high-resolution timing:
- **Monotonic Clock**: `System.nanoTime()` is based on the host CPU's high-resolution performance counter (TSC or equivalent) and is guaranteed not to jump backwards during NTP clock adjustments.
- **Nanosecond Granularity**: Provides sufficient resolution to measure CPU scheduling algorithms executing in hundreds of microseconds or low single-digit milliseconds.
- **Wall-Clock vs. CPU Time**: Wall-clock duration reflects actual elapsed physical time. When micro-benchmarks execute on a single dedicated CPU core with zero sleep calls, wall-clock time accurately reflects execution latency while also detecting interruptions caused by host thread preemption or memory bus stalls.

---

## 4. JIT Warmup & Tiered Compilation Strategy

Modern HotSpot JVMs employ Tiered Compilation (Interpreter $\to$ C1 $\to$ C2):
1. **Interpreter & C1 (Tiers 1–3)**: Fast startup with method profiling counters.
2. **C2 (Tier 4)**: High-performance optimizing compiler featuring loop unrolling, escape analysis, and dead code elimination.

### Warmup Protocol
- Each benchmark configuration specifies an explicit `warmupIterations` count (default: 5 iterations).
- During the warmup phase, the algorithm runs identically on the target workload, priming branch prediction tables and triggering C2 compilation.
- **All warmup execution samples are discarded** and never included in the final statistics or persistent dataset.
- Following warmup, `measuredIterations` (default: 15–50 iterations) are captured sequentially into an in-memory sample buffer.

---

## 5. Dual Throughput Metrics: Actual Batch Throughput vs. Latency-Derived Estimate

A frequent mistake in distributed systems benchmarking is taking the median latency $P_{50}$ and inverting it ($1 / P_{50}$) as an assertion of system throughput. Orchestrivo measures both and explains their distinction:

### 5.1 Actual Batch Throughput ($R_{batch}$)
$$R_{batch} = \frac{N_{valid}}{\Delta t_{batch\_wall\_seconds}}$$
- **Definition**: The total number of valid measured runs divided by the actual wall-clock elapsed time required to execute the entire batch of iterations.
- **Real-World Meaning**: This metric includes memory allocation pressure, minor CPU cache line evictions, JIT safepoint polls, and operating system context switches between iterations. It represents what a host system actually sustains under sustained back-to-back load.

### 5.2 Latency-Derived Throughput Estimate ($R_{est}$)
$$R_{est} = \frac{1}{P_{50}} \times 10^9$$
- **Definition**: The reciprocal of the 50th percentile (median) execution latency.
- **Caveat**: This is a theoretical upper bound. It assumes that every subsequent run executes under ideal conditions with a warm L1 cache and zero inter-task jitter.

In Orchestrivo's verified benchmarks (e.g., Round Robin $q=6$ on 50 processes), $R_{batch} \approx 2,369.93\text{ jobs/s}$ while $R_{est} \approx 4,618.96\text{ jobs/s}$, clearly demonstrating that actual throughput is roughly $51\%$ of theoretical reciprocal throughput due to memory churn and scheduling overheads.

---

## 6. Statistical Rigor: Median (P50) vs. Mean

Orchestrivo employs non-parametric statistics for primary reporting:

1. **Why Median is Primary**:
   In micro-benchmarks on non-isolated consumer hardware (e.g., Windows 11 with background OS processes), latency distributions are non-Gaussian and exhibit positive skewness. A single background disk flush or interrupt can introduce a $20\times$ tail latency spike. The Median ($P_{50}$) is resilient against outliers and reflects true typical performance.
2. **Secondary Variance Metrics**:
   - **Min**: Captures optimal CPU cache locality and zero OS interference.
   - **Max**: Captures worst-case tail latency and cache miss / descheduling events.
   - **Standard Deviation ($\sigma$)**: Quantifies stability across runs.

---

## 7. Comparative Experiment Validity & Guardrails

When comparing a Baseline run against a Candidate run, Orchestrivo enforces strict compatibility criteria before classifying the comparison as a fair A/B experiment:

- **Workload Equivalence**: Number of processes ($N$), burst distributions, arrival parameters, and pseudo-random seed ($S$) must match.
- **Experimental Variable Identification**: Exactly one controlled variable is altered:
  - *Algorithm Comparison*: Same workload, comparing e.g., FCFS vs. SRTF.
  - *Parameter Tuning*: Same algorithm and workload, comparing e.g., Round Robin $q=3$ vs. $q=6$.
- **Exploratory Comparison Banner**: If workloads or seeds diverge, the UI displays an **Uncontrolled Workload Warning** and disables fair comparison claims.
