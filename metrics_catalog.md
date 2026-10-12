# Orchestrivo Metrics Catalog

## 1. Overview and Design Principles

The Orchestrivo Metrics Catalog defines every operational, scheduling, queuing, and benchmarking metric evaluated by the platform. It establishes a formal, universal taxonomy mapping each metric to its exact mathematical formula, units, measurement boundary, directionality, and engineering trade-offs.

All delta calculations and ratio comparisons across baseline and candidate runs conform to the standardized definitions implemented in `com.orchestrivo.model.MetricsCatalog` and `com.orchestrivo.model.BenchmarkComparisonResult`.

---

## 2. Metric Directionality & Delta Formulas

### 2.1 Directionality Conventions
- **`LOWER_IS_BETTER`**: Metrics representing latency, waiting times, execution durations, overheads, or variance. Reductions indicate performance improvements.
- **`HIGHER_IS_BETTER`**: Metrics representing throughput, CPU utilization, completion volume, or cache hits. Increases indicate performance improvements.
- **`NEUTRAL`**: Informational parameters, counts, seeds, or configurations (e.g., process counts, time quanta).

### 2.2 Delta Formulas with Guardrails
To prevent divide-by-zero arithmetic errors when evaluating baseline vs. candidate runs:

1. **Absolute Delta**:
   $$\Delta_{abs} = Candidate - Baseline$$

2. **Relative Percentage Delta**:
   $$\Delta_{\%} = \begin{cases} 
   0.0 & \text{if } Baseline = 0 \text{ and } Candidate = 0 \\
   100.0 & \text{if } Baseline = 0 \text{ and } Candidate > 0 \\
   -100.0 & \text{if } Baseline = 0 \text{ and } Candidate < 0 \\
   \left( \frac{Candidate - Baseline}{|Baseline|} \right) \times 100 & \text{otherwise}
   \end{cases}$$

3. **Speedup Factor (for Latency)**:
   $$S = \begin{cases}
   1.0 & \text{if } Baseline \le 0 \text{ or } Candidate \le 0 \\
   \frac{Baseline}{Candidate} & \text{otherwise}
   \end{cases}$$
   *Interpretation*: $S > 1.0$ indicates an acceleration (Candidate is faster than Baseline). $S < 1.0$ indicates a slowdown.

---

## 3. Discrete CPU Scheduling Metrics

All discrete scheduling metrics are computed deterministically over a discrete time axis $[0, Makespan]$ by `ScheduleValidator` and the scheduling strategies in `com.orchestrivo.domain`.

| Metric Name | Symbol | Units | Measurement Boundary | Directionality | Formula / Definition | Technical Trade-Offs |
|---|---|---|---|---|---|---|
| **Arrival Time** | $A_i$ | Discrete ticks | Workload Input | `NEUTRAL` | Time tick when process $P_i$ enters the ready state. | Exogenous workload parameter. |
| **Burst Time** | $B_i$ | Discrete ticks | Workload Input | `NEUTRAL` | Total CPU execution time required by $P_i$. | Heavy bursts induce convoy effects under FCFS. |
| **Completion Time** | $C_i$ | Discrete ticks | Simulation Lifecycle | `LOWER_IS_BETTER` | Time tick when $P_i$ completes execution and leaves the system. | Preemption can delay individual process completion. |
| **Turnaround Time** | $TAT_i$ | Discrete ticks | Simulation Lifecycle | `LOWER_IS_BETTER` | $TAT_i = C_i - A_i$ | Minimizing average TAT is mathematically achieved by SJF/SRTF at the expense of fairness and potential starvation. |
| **Waiting Time** | $WT_i$ | Discrete ticks | Ready Queue Delay | `LOWER_IS_BETTER` | $WT_i = TAT_i - B_i$ | Long processes experience high WT under SRTF; short processes experience high WT under FCFS with heavy convoy heads. |
| **Response Time** | $RT_i$ | Discrete ticks | First Dispatch Delay | `LOWER_IS_BETTER` | $RT_i = FirstDispatchTime_i - A_i$ | Round Robin minimizes RT for interactive jobs; FCFS yields high RT under long bursts. |
| **CPU Utilization** | $U_{cpu}$ | Percentage (%) | Discrete Horizon | `HIGHER_IS_BETTER` | $U_{cpu} = \left(\frac{\sum B_i}{Makespan}\right) \times 100$ | Idling occurs when ready queue is empty while waiting for future arrivals. |
| **Context Switch Count** | $CS$ | Integer count | Discrete Transitions | `LOWER_IS_BETTER` | $\sum_{k=1}^{M-1} [Process(k) \ne Process(k+1)]$ | Small Round Robin quanta ($q \to 1$) minimize RT but cause extreme context-switch overhead in practical operating systems. |
| **Makespan** | $M$ | Discrete ticks | Global Completion | `LOWER_IS_BETTER` | $\max(C_i)$ | Work-conserving single-core CPU schedulers share identical makespans for identical arrival sets without idle gaps. |

---

## 4. Distributed Execution & Job Queue Metrics

Engine-level asynchronous job execution metrics tracked in MySQL (`execution_jobs`, HikariCP, and `ExecutionWorkerManager`).

| Metric Name | Units | Measurement Boundary | Directionality | Definition & Database Source | Failure Modes & Systems Impact |
|---|---|---|---|---|---|
| **Queue Latency** | Milliseconds | Ingestion to Worker Claim | `LOWER_IS_BETTER` | $t_{started\_at} - t_{created\_at}$ in `execution_jobs` | Queued jobs back up when worker pool saturates or concurrency limit is reached. |
| **Execution Duration** | Milliseconds | Worker Claim to Result Commit | `LOWER_IS_BETTER` | $t_{completed\_at} - t_{started\_at}$ in `execution_jobs` | Monitored by the lease renewal heartbeat thread. |
| **Attempt Count** | Integer | Execution Lifecycle | `LOWER_IS_BETTER` | `attempt_count` column | Incremented on crash recovery or worker failure; capped by `max_attempts` (default: 3). |
| **Lease Expiry Duration** | Seconds | Worker Claim Ownership | `NEUTRAL` | `lease_expires_at - CURRENT_TIMESTAMP` | Leases default to 30s. Heartbeat renews lease every 5s while worker is healthy. |
| **Fencing Token Rejection** | Count | State Transition Filter | `LOWER_IS_BETTER` | Stale or mismatched `claim_token` updates rejected by DB | Prevents zombie workers from writing results after lease expiration and recovery. |
| **Idempotency Key Collisions** | Count | Ingestion Filter | `NEUTRAL` | Duplicate `idempotency_key` submissions | Concurrent requests return the existing job record without re-enqueuing duplicates. |

---

## 5. Host JVM Micro-Benchmark Metrics

Scientific benchmark measurements captured via `BenchmarkService` and persisted in `benchmark_runs`.

| Metric Name | Symbol | Units | Measurement Boundary | Directionality | Formula / Definition | Technical Notes & Trade-Offs |
|---|---|---|---|---|---|---|
| **Median Latency (P50)** | $P_{50}$ | Nanoseconds / Microseconds | Pure Algorithm Execution | `LOWER_IS_BETTER` | 50th percentile of measured execution samples. | Resilient against outliers caused by host background OS jitter. |
| **Mean Latency** | $\mu$ | Nanoseconds / Microseconds | Pure Algorithm Execution | `LOWER_IS_BETTER` | $\frac{1}{N} \sum_{k=1}^N t_k$ | Vulnerable to single large outlier (GC pause, context switch). |
| **Min Latency** | $t_{min}$ | Nanoseconds / Microseconds | Pure Algorithm Execution | `LOWER_IS_BETTER` | $\min(t_k)$ | Represents optimal CPU cache locality and zero OS interruption. |
| **Max Latency** | $t_{max}$ | Nanoseconds / Microseconds | Pure Algorithm Execution | `LOWER_IS_BETTER` | $\max(t_k)$ | Captures worst-case tail latency and cache miss / thread descheduling events. |
| **Standard Deviation** | $\sigma$ | Nanoseconds / Microseconds | Sample Variance | `LOWER_IS_BETTER` | $\sqrt{\frac{1}{N-1}\sum_{k=1}^N (t_k - \mu)^2}$ | Quantifies measurement jitter; high $\sigma$ indicates host interference. |
| **Actual Batch Throughput** | $R_{batch}$ | Runs / Second | Batch Wall-Clock Horizon | `HIGHER_IS_BETTER` | $\frac{N_{valid}}{t_{batch\_wall\_sec}}$ | **Authentic metric**: Measures real jobs executed divided by true elapsed wall-clock time including inter-iteration overheads. |
| **Latency-Derived Throughput Estimate** | $R_{est}$ | Runs / Second | Derived Inverted Latency | `HIGHER_IS_BETTER` | $\frac{1}{P_{50}} \times 10^9$ | **Optimistic estimate**: Assumes zero scheduling jitter, zero cache misses, and perfect back-to-back invocation. |
| **Speedup Factor** | $S$ | Ratio ($x$) | Cross-Run Comparison | `HIGHER_IS_BETTER` | $\frac{P_{50, baseline}}{P_{50, candidate}}$ | $S > 1.0$ indicates acceleration. Independent of clock scaling when run on identical hardware. |

---

## 6. Throughput Discrepancy Analysis: Actual vs. Latency-Derived

A common flaw in naive benchmarking frameworks is taking the median latency $P_{50}$ of a micro-operation and defining throughput as $1 / P_{50}$. Orchestrivo measures both and records the structural difference:

1. **Why $R_{est}$ is Optimistic**:
   $R_{est} = \frac{1}{P_{50}}$ assumes that successive scheduling tasks occur in isolation with hot L1/L2 instruction caches and zero pipeline stalls.
2. **Why $R_{batch}$ Reflects Reality**:
   $R_{batch} = \frac{N_{valid}}{\Delta t_{batch}}$ captures the true end-to-end wall-clock time required to execute the entire batch of iterations. It naturally accounts for:
   - JIT compile de-optimizations and safepoints.
   - Host thread time-slicing and OS scheduler migrations.
   - CPU memory bus contention and data cache refills.
   - Inter-run garbage collection allocation pressures.

In Orchestrivo's micro-benchmarks, $R_{batch}$ typically sits at 70%–95% of $R_{est}$, providing an honest and rigorous characterization of host throughput.
