# Orchestrivo — Stage 4.2 Benchmark Methodology & Measurement Protocol

## 1. Executive Summary & Purpose

The Orchestrivo benchmarking framework is designed to provide reproducible, empirically verified performance characterization of CPU scheduling algorithms (FCFS, Non-Preemptive SJF, Preemptive SRTF, and Round Robin) operating over discrete process workloads.

In distributed and high-throughput execution engines, performance claims are frequently invalidated by methodological flaws:
1. **JIT compilation artifacts:** Measuring un-warmed JVM byte-code execution where interpretation and compilation overhead mask algorithmic complexity.
2. **Clock resolution truncation:** Measuring sub-millisecond execution times with millisecond-granularity timers (`System.currentTimeMillis()`), resulting in coarse zero-millisecond or clamped artificial one-millisecond records.
3. **Statistical oversimplification:** Reporting arithmetic means without variance, median, or percentile distribution, which hides latency tail effects (P90/P99).
4. **Invalid cross-comparison:** Comparing algorithms across differing workload snapshots without verifying workload invariants, producing invalid speedup conclusions.

This document formalizes the measurement protocol, clock selection, statistical aggregation, comparability rules, and pairwise before/after comparison engine implemented in Orchestrivo Stage 4.2.

---

## 2. In-Process JVM Benchmark Harness Architecture

### 2.1 Execution Model
The benchmarking subsystem (`BenchmarkCampaignRunner`, `BenchmarkService`) executes scheduling algorithms directly within the host JVM to isolate algorithmic dispatch and state manipulation from network transport and disk I/O latency.

```
+-----------------------------------------------------------------------------------+
|                            BenchmarkCampaignRunner                                |
|                                                                                   |
|  +---------------------------+             +----------------------------------+   |
|  |     Workload Dataset      |             |         Warm-Up Phase            |   |
|  | - Process Specifications  | ----------> | - Iterations: 1..20              |   |
|  | - Deterministic Seed      |             | - JIT C1/C2 tiered compilation   |   |
|  | - Schema Version 1 Invar. |             | - Results completely discarded   |   |
|  +---------------------------+             +----------------------------------+   |
|                                                              |                    |
|                                                              v                    |
|  +---------------------------+             +----------------------------------+   |
|  |   Statistical Aggregator  |             |        Measurement Phase         |   |
|  | - Mean, Median, Min, Max  | <---------- | - Iterations: 3..100             |   |
|  | - StdDev, P90, P99        |             | - Monotonic System.nanoTime()    |   |
|  | - Real-clock Throughput   |             | - Nanosecond resolution capture  |   |
|  +---------------------------+             +----------------------------------+   |
+-----------------------------------------------------------------------------------+
```

### 2.2 Warm-Up Protocol & JIT Stabilization
The Java Virtual Machine employs HotSpot tiered compilation (Levels 1–3: C1 compiler with profiling; Level 4: C2 optimizing compiler). Initial method invocations incur significant execution penalties:
- Bytecode interpretation.
- Profiling and branch counter increments.
- On-Stack Replacement (OSR) compilation pauses.
- Class loading and memory allocation initialization.

#### Protocol Rules:
1. **Warm-Up Iterations:** The harness executes between 1 and 20 untimed warm-up passes (default: 5) through the algorithm implementation using identical process spec structures.
2. **Discard Policy:** All warm-up invocation durations and schedule outputs are strictly discarded from final sample aggregations.
3. **Memory Isolation:** Prior to transitioning from warm-up to measurement, temporary collection allocations from warm-up are unreferenced to minimize garbage collection interference during measured runs.

---

## 3. High-Resolution Timing & Clock Selection

### 3.1 Monotonic Clock vs Wall Clock
Orchestrivo strictly differentiates between simulated scheduling time and physical engine execution duration:

| Clock Domain | Source | Target Metric | Characteristics |
|---|---|---|---|
| **Simulated Virtual Time** | Algorithm logic (`currentTime` ticks) | Waiting Time, Turnaround Time, Response Time | Discrete virtual units defined by arrival and burst time inputs. Independent of CPU clock frequency. |
| **Physical Wall-Clock Engine Time** | `System.nanoTime()` | `execution_duration_nanos`, Execution Throughput | Monotonic nanosecond timestamp. Unaffected by system clock adjustments, leap seconds, or NTP shifts. |

### 3.2 Elimination of Millisecond Clamping & Truncation
In previous versions, execution time was recorded using integer millisecond precision:
```java
// LEGACY (REPLACED): Caused sub-millisecond runs to collapse to 0 ms or clamp to 1 ms
long durationMs = Math.max(1, (System.currentTimeMillis() - startMs));
```

In Stage 4.2, all scheduling runs capture exact nanosecond intervals:
```java
// CURRENT STAGE 4.2 IMPLEMENTATION: High-resolution monotonic timing
long startNano = System.nanoTime();
ScheduleResult result = strategy.schedule(processes, context);
long durationNanos = Math.max(1L, System.nanoTime() - startNano);
long durationMs = durationNanos / 1_000_000L;
```
- In database persistence (`execution_jobs`, `benchmark_runs`), both `execution_duration_ms` (integer for backward compatibility) and `execution_duration_nanos` (BIGINT for high-resolution analysis) are recorded.
- In UI presentation, durations under 10 ms are formatted with microsecond precision (`X.XXX ms`), enabling clear visibility into algorithm efficiency improvements without inserting artificial `Thread.sleep()` pauses.

---

## 4. Statistical Distribution & Aggregation Math

Let $S = [t_1, t_2, \dots, t_N]$ represent the sorted sequence of measured execution durations (in nanoseconds) across $N$ measured iterations ($N \ge 3$).

### 4.1 Central Tendency Metrics
- **Arithmetic Mean ($\mu$):**
  $$\mu = \frac{1}{N} \sum_{i=1}^{N} t_i$$
- **Median ($M$):**
  $$M = \begin{cases} t_{(N+1)/2} & \text{if } N \text{ is odd} \\ \frac{t_{N/2} + t_{(N/2)+1}}{2} & \text{if } N \text{ is even} \end{cases}$$
  *Note:* The median is prioritized over the arithmetic mean in before/after comparisons because it is robust against outlier pauses caused by OS thread scheduling and JVM garbage collection safepoints.

### 4.2 Dispersion & Jitter Metrics
- **Minimum & Maximum:**
  $$\text{Min} = t_1, \quad \text{Max} = t_N$$
- **Standard Deviation ($\sigma$):**
  $$\sigma = \sqrt{\frac{1}{N} \sum_{i=1}^{N} (t_i - \mu)^2}$$
  $\sigma$ quantifies execution jitter. Lower standard deviation demonstrates consistent dispatch performance.

### 4.3 Percentiles (P90 & P99)
Percentiles identify tail latency behavior under load. Calculated via 1-indexed nearest rank:
$$\text{Rank}_P = \left\lceil \frac{P}{100} \times N \right\rceil$$
$$P90 = t_{\min(N, \text{Rank}_{90})}, \quad P99 = t_{\min(N, \text{Rank}_{99})}$$

### 4.4 Engine Throughput
Throughput measures the operations processed per unit of physical wall-clock time:
$$\text{Throughput (ops/sec)} = \frac{N \times 10^9}{\sum_{i=1}^{N} t_i \text{ (nanoseconds)}}$$

---

## 5. Workload Invariants & Snapshot Versioning

To ensure scientific validity, performance benchmarks must run against identical input workloads.

### 5.1 Snapshot Schema Version 1
Workload snapshots are serialized with metadata and integrity checksums:
```json
{
  "schemaVersion": 1,
  "seed": 184729104820194,
  "processCount": 10,
  "totalBurstTime": 58,
  "minArrival": 0,
  "maxArrival": 12,
  "processes": [
    {"processId": "P1", "arrivalTime": 0, "burstTime": 10, "priority": 1},
    {"processId": "P2", "arrivalTime": 2, "burstTime": 5, "priority": 2}
  ]
}
```

### 5.2 Equivalence Verification
The system enforces equivalence checking (`WorkloadSnapshot.areSnapshotsEquivalent`) before enabling pairwise comparison:
1. `processCount` must match exactly.
2. For each process index $k$, `arrivalTime` and `burstTime` must be identical.
3. Legacy unversioned snapshots (`[{"processId":"P1",...}]`) are parsed transparently and upgraded to Schema Version 1 during comparison analysis.

---

## 6. Pairwise Before/After Comparison Engine

The pairwise benchmark comparison module evaluates an established **Baseline Run** against a newly evaluated **Candidate Run** (`/benchmarks/compare`).

### 6.1 Run Roles & Identification
- `BASELINE`: A benchmark run captured prior to code, algorithm, or configuration optimization.
- `CANDIDATE`: A benchmark run captured after optimization, executed on matching hardware and workload.
- `STANDARD`: General exploratory runs not tied to a specific pairwise experiment.
- `benchmark_group`: An optional grouping string (e.g., `OPT-BATCH-RR`) that links related runs together.

### 6.2 Comparison Formulas & Division-by-Zero Guards

Let $B$ represent the Baseline run and $C$ represent the Candidate run:

1. **Speedup Factor:**
   $$\text{Speedup} = \begin{cases} \frac{B.\text{MedianNanos}}{C.\text{MedianNanos}} & \text{if } C.\text{MedianNanos} > 0 \\ 1.00 & \text{if } C.\text{MedianNanos} = 0 \end{cases}$$
   - A speedup of $1.50\times$ indicates the Candidate execution is 50% faster (runs in $66.7\%$ of baseline time).
   - A speedup of $0.80\times$ indicates a regression (Candidate is slower).

2. **Latency Reduction ($\Delta$ Median & $\Delta$ Mean):**
   $$\Delta \text{Median} = B.\text{MedianNanos} - C.\text{MedianNanos}$$
   $$\text{Reduction \%} = \frac{B.\text{MedianNanos} - C.\text{MedianNanos}}{B.\text{MedianNanos}} \times 100\%$$
   - Positive delta represents performance improvement (latency reduced).
   - Negative delta represents regression.

3. **Throughput Improvement ($\Delta$ Throughput):**
   $$\Delta \text{Throughput} = C.\text{Throughput} - B.\text{Throughput}$$
   $$\text{Throughput Delta \%} = \frac{C.\text{Throughput} - B.\text{Throughput}}{B.\text{Throughput}} \times 100\%$$

4. **Jitter Stability ($\Delta$ StdDev):**
   $$\Delta \sigma = B.\text{StdDevNanos} - C.\text{StdDevNanos}$$
   - Positive values indicate lower execution variance and higher predictability.

---

## 7. Experiment Comparison Modes (A, B, C)

In addition to benchmark runs, simulated scheduling experiments support three distinct comparison modes (`/experiments/compare`):

### Mode A: Controlled Algorithm Comparison
- **Precondition:** Identical workload snapshot ($W_A \equiv W_B$), different scheduling algorithms ($Alg_1 \ne Alg_2$).
- **Goal:** Scientifically evaluate which scheduling strategy yields superior waiting time, turnaround time, response time, or context switch efficiency on an identical arrival schedule.
- **UI Treatment:** `badge-success` "Mode A: Controlled Comparison". Automatically highlights winning algorithm per metric.

### Mode B: Workload Scaling Analysis
- **Precondition:** Identical algorithm ($Alg_1 \equiv Alg_2$), differing workloads ($W_A \ne W_B$).
- **Goal:** Analyze algorithmic scalability and throughput degradation as process count ($N$) or burst characteristics scale.
- **UI Treatment:** `badge-primary` "Mode B: Workload Scaling". Displays workload delta summary and caution against naive latency comparisons across different burst volumes.

### Mode C: Exploratory Historical Comparison
- **Precondition:** Differing workloads ($W_A \ne W_B$) and differing algorithms ($Alg_1 \ne Alg_2$).
- **Goal:** Multi-variable historical comparison across different experimental sessions.
- **UI Treatment:** `badge-warning` "Mode C: Exploratory Comparison". Displays informative delta pills without claiming direct algorithmic causality.

---

## 8. Verification & Test Evidence

The benchmarking harness and comparison engine are validated by automated test suites executed against the host environment:

| Test Class | Scope | Assertions Verified | Result |
|---|---|---|---|
| `BenchmarkBeforeAfterComparisonTest` | Pairwise comparison logic | Speedup calculations, zero-division guards, delta formulas, mismatched workload detection | **5/5 PASS** |
| `BenchmarkServiceTest` | Service orchestration | Benchmark persistence, warm-up iteration handling, host metadata capture | **5/5 PASS** |
| `BenchmarkValidationTest` | Validation rules | Warm-up boundary enforcement (1–20), measured boundary enforcement (3–100), process count bounds (5–500) | **28/28 PASS** |
| `SnapshotVersioningTest` | Snapshot integrity | Schema version 1 formatting, invariant checking, legacy array compatibility | **5/5 PASS** |
| `ReproducibilitySeedTest` | PRNG reproducibility | Deterministic process spec generation, seed replay fidelity | **3/3 PASS** |
| `ExecutionWorkerManagerTest` | Nanosecond timing | Microsecond precision capture, elimination of millisecond truncation | **4/4 PASS** |

Total verification coverage: **50 passing tests** across benchmark and measurement modules. Zero synthetic delays or simulated mock data utilized.
