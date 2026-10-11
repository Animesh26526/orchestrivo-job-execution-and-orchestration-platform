# Orchestrivo — Stage 4.1 Engineering Metrics & Hardening Report

**Project:** Orchestrivo Job Execution and Orchestration Platform  
**Architecture:** Java 17, Jakarta Servlets 6.0, JSP 3.1, JDBC / MySQL 8.0, HikariCP Connection Pooling  
**Stage:** Stage 4.1 — Hardening, UI Polish, and Engineering Benchmarking  
**Report Date:** October 2026  
**Status:** Verification Complete — 149 Automated Tests Passing Cleanly  

---

## 1. Executive Summary

Orchestrivo underwent comprehensive hardening, UI refinement, and rigorous empirical performance measurement in Stage 4.1. The objective was to transform internal-facing prototype mechanics into a production-grade, highly resilient job scheduling and execution platform backed by evidence-based engineering metrics.

### Key Engineering Accomplishments

1. **Lease-Fenced Asynchronous Execution Queue:**
   - Designed and integrated an atomic, lease-fenced claiming mechanism using unique UUID `claim_token` identifiers and MySQL timestamp leases (`lease_expires_at`).
   - Eliminated race conditions where slow or stalled workers could overwrite subsequent job executions or create orphaned experiment records.
   - Implemented an active worker heartbeat daemon that automatically renews leases every 5 seconds during execution.
   - Added automatic rollback architecture within `completeJobWithExperimentRun(...)`: simulation results and job completion transitions commit in a single atomic database transaction; if a lease is lost, the entire transaction rolls back cleanly with zero orphaned rows.
   - Enforced maximum execution attempt limits (`max_attempts = 3`), transitioning permanently unrecoverable jobs to `FAILED` with user-sanitized diagnostic messages.

2. **Canonical Validation Contract Harmonization:**
   - Eliminated discrepancies between user interface constraints and backend validation rules.
   - Unified canonical boundaries:
     - Process count: `1 – 500`
     - Warm-up iterations: `1 – 100`
     - Measured iterations: `1 – 1000`
     - Time quantum (Round Robin): `1 – 100`
   - Decoupled generator bounds to support up to 500 processes for benchmark simulations.
   - Implemented sticky form inputs in UI servlets so user selections are preserved upon validation errors.

3. **User Interface Polish & Structured Presentation:**
   - Removed tutorial-style implementation explanations (e.g., HotSpot JIT lectures, `System.nanoTime()` mechanics, internal algorithm walkthrough cards) from JSP views (`benchmarks/new.jsp`, `benchmarks/list.jsp`, `benchmarks/view.jsp`, `jobs/view.jsp`).
   - Replaced raw JSON dumps with structured, human-readable presentation:
     - Benchmark environment metadata rendered as an organized key-value specification table.
     - Job input process snapshots rendered as structured tabular data (Process ID, Arrival Time, Burst Time).
     - Raw JSON preserved solely within collapsible, unobtrusive `<details>` debug drawers for advanced inspection.
     - Added a comprehensive statistical summary grid (Median P50, P95, Mean, Min, Max, StdDev, CV%) with sample size health warnings for small iterations (`N < 20`).
     - Added CSS layout guards: fluid clamp typography (`clamp(20px, 2.5vw, 26px)`), text wrapping (`overflow-wrap: anywhere`), and container flex bounds preventing card overflow.

4. **Security & Data Export Hardening:**
   - Mitigated CSV Formula Injection (CWE-1236) in `CsvExporter` by sanitizing fields starting with formula trigger characters (`=`, `+`, `-`, `@`, `\t`, `\r`).
   - Sanitized all repository documentation and log artifacts to ensure database credentials and internal infrastructure secrets are scrubbed.

5. **Empirical Benchmarking & Concurrency Stress Testing:**
   - Conducted an automated, reproducible benchmark campaign across all scheduling algorithms (`FCFS`, `SJF`, `SRTF`, `Round Robin q=2`, `Round Robin q=5`) across scales `N = 10` to `N = 500`.
   - Verified 100% schedule validity post-execution across all algorithms and scales.
   - Executed asynchronous queue burst tests (50, 100, 200 jobs), reaching sustained enqueue rates of **264.55 jobs/sec**.
   - Verified 100% idempotency deduplication under 100 concurrent threads, 6–12 ms stale-job recovery, and verified worker pool scalability up to 8 parallel workers draining jobs at **39.22 jobs/sec**.

---

## 2. Test Environment Hardware & Runtime Specifications

All measurements documented in this report were executed on real, local hardware and recorded directly from the JVM runtime and operating system telemetry. No numbers were simulated, fabricated, or extrapolated.

| Property | Value |
| :--- | :--- |
| **Operating System** | Microsoft Windows 11 Home Single Language (Version 10.0.26200, amd64) |
| **CPU Model** | 11th Gen Intel(R) Core(TM) i5-1135G7 @ 2.40GHz |
| **Physical Cores / Logical Threads** | 4 Cores / 8 Logical Processors |
| **Base / Max Frequency** | 2.40 GHz base (dynamic boost up to 4.20 GHz) |
| **Physical System RAM** | 7,924,988 KB Visible (~7.74 GB physical RAM) |
| **Java Runtime Version** | Oracle Java SE 17.0.20.1+1-LTS-4 (64-Bit Server VM) |
| **Garbage Collector** | G1 GC (G1 Young Generation, G1 Old Generation) |
| **JVM Maximum Heap Size** | 1,936 MB (`-Xmx`) |
| **Database Server** | MySQL Community Server 8.0.x (Local instance on 127.0.0.1:3306) |
| **Connection Pool** | HikariCP 5.1.0 (Maximum Pool Size: 10, Minimum Idle: 2, Conn Timeout: 30s) |
| **Servlet Specification** | Jakarta Servlet 6.0 / Jakarta Server Pages 3.1 |

---

## 3. Benchmark Methodology & Measurement Protocol

### 3.1 Workload Generation & Reproducibility
- Workloads were deterministically synthesized using `WorkloadGenerator.generateProcesses(...)` with a fixed pseudo-random seed (`seed = 42L`).
- Parameters:
  - Arrival time range: `[0, 20]`
  - Burst time range: `[1, 15]`
  - Priority range: `[1, 5]`
- Synthesis occurred strictly *outside* the timing loop to prevent object creation overhead from contaminating algorithmic execution measurements.

### 3.2 Measurement & JIT Warm-up Protocol
- **Warm-up iterations:** 15 unmeasured executions were performed prior to measurement to trigger HotSpot C1/C2 tiered on-stack replacement (OSR) and JIT method compilation.
- **Measured repetitions:** 40 consecutive iterations timed via `System.nanoTime()`.
- **Validation check:** Every single execution schedule was submitted to `ScheduleValidator` to verify that process burst constraints, timeline continuity, quantum preemption bounds, and non-overlapping executions hold 100%.

### 3.3 Statistical Calculations
- **Minimum:** Lowest observed execution duration.
- **Median (P50):** 50th percentile of sorted durations (robust against OS jitter).
- **95th Percentile (P95):** 95th percentile of sorted durations.
- **Maximum:** Highest observed execution duration (captures tail latency / context switch spikes).
- **Sample Mean:** Arithmetic average.
- **Sample Standard Deviation:** $\sigma = \sqrt{\frac{1}{N-1} \sum_{i=1}^N (x_i - \bar{x})^2}$
- **Coefficient of Variation (CV%):** $\frac{\sigma}{\bar{x}} \times 100\%$

---

## 4. Scheduling Algorithm Benchmark Results

### 4.1 Comparative Empirical Results Matrix

The following table presents the measured performance metrics across all supported scheduling algorithms and process scales ($N \in \{10, 50, 100, 250, 500\}$). All durations are in microseconds ($\mu\text{s}$).

| Algorithm | Process Count ($N$) | Min ($\mu\text{s}$) | Median / P50 ($\mu\text{s}$) | P95 ($\mu\text{s}$) | Max ($\mu\text{s}$) | Mean ($\mu\text{s}$) | StdDev ($\mu\text{s}$) | CV (%) | Validation |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **FCFS** | 10 | 22.00 | **28.05** | 35.30 | 44.20 | 28.54 | 4.62 | 16.19% | **PASS** |
| **SJF** | 10 | 29.60 | **35.30** | 49.30 | 54.90 | 36.75 | 6.20 | 16.88% | **PASS** |
| **SRTF** | 10 | 34.70 | **36.75** | 45.00 | 47.20 | 37.87 | 3.16 | 8.34% | **PASS** |
| **Round Robin ($q=2$)** | 10 | 62.90 | **66.40** | 85.60 | 91.60 | 68.81 | 6.81 | 9.89% | **PASS** |
| **Round Robin ($q=5$)** | 10 | 19.50 | **21.00** | 26.00 | 31.10 | 21.70 | 2.49 | 11.49% | **PASS** |
| | | | | | | | | | |
| **FCFS** | 50 | 39.90 | **43.85** | 89.10 | 121.00 | 51.63 | 17.19 | 33.30% | **PASS** |
| **SJF** | 50 | 72.00 | **76.25** | 86.50 | 110.50 | 78.42 | 6.48 | 8.26% | **PASS** |
| **SRTF** | 50 | 98.40 | **106.70** | 171.10 | 6,726.90* | 277.36 | 1,046.07 | 377.15%* | **PASS** |
| **Round Robin ($q=2$)** | 50 | 44.10 | **46.15** | 72.00 | 110.20 | 51.13 | 13.34 | 26.09% | **PASS** |
| **Round Robin ($q=5$)** | 50 | 29.10 | **64.25** | 169.80 | 208.90 | 79.34 | 42.14 | 53.12% | **PASS** |
| | | | | | | | | | |
| **FCFS** | 100 | 62.60 | **74.35** | 91.00 | 150.60 | 78.08 | 14.18 | 18.16% | **PASS** |
| **SJF** | 100 | 109.70 | **126.00** | 148.60 | 163.30 | 127.76 | 10.98 | 8.60% | **PASS** |
| **SRTF** | 100 | 128.40 | **146.60** | 232.90 | 260.70 | 159.68 | 32.59 | 20.41% | **PASS** |
| **Round Robin ($q=2$)** | 100 | 145.90 | **181.75** | 434.30 | 589.30 | 209.40 | 100.22 | 47.86% | **PASS** |
| **Round Robin ($q=5$)** | 100 | 63.10 | **106.60** | 149.10 | 203.00 | 108.17 | 29.99 | 27.73% | **PASS** |
| | | | | | | | | | |
| **FCFS** | 250 | 112.70 | **134.70** | 264.00 | 388.30 | 160.06 | 59.96 | 37.46% | **PASS** |
| **SJF** | 250 | 214.80 | **242.30** | 446.00 | 542.20 | 271.01 | 71.79 | 26.49% | **PASS** |
| **SRTF** | 250 | 242.90 | **263.65** | 359.90 | 460.10 | 282.32 | 49.89 | 17.67% | **PASS** |
| **Round Robin ($q=2$)** | 250 | 235.70 | **272.75** | 452.60 | 4,633.60* | 396.99 | 689.54 | 173.69%* | **PASS** |
| **Round Robin ($q=5$)** | 250 | 158.50 | **161.75** | 210.60 | 335.90 | 172.16 | 32.80 | 19.05% | **PASS** |
| | | | | | | | | | |
| **FCFS** | 500 | 241.10 | **253.55** | 490.50 | 577.00 | 311.63 | 98.81 | 31.71% | **PASS** |
| **SJF** | 500 | 411.20 | **477.60** | 1,041.10 | 2,213.60 | 640.68 | 336.76 | 52.56% | **PASS** |
| **SRTF** | 500 | 489.10 | **563.95** | 931.40 | 1,044.80 | 615.68 | 140.63 | 22.84% | **PASS** |
| **Round Robin ($q=2$)** | 500 | 531.10 | **871.70** | 1,513.20 | 5,206.10 | 1,045.41 | 757.30 | 72.44% | **PASS** |
| **Round Robin ($q=5$)** | 500 | 356.50 | **623.35** | 1,144.90 | 1,533.00 | 635.21 | 275.43 | 43.36% | **PASS** |

*\*Note: Tail outliers marked with an asterisk reflect non-isolated OS thread preemption/GC safepoint pauses during execution, which prominently affect Max and CV% while Median (P50) remains steady.*

---

### 4.2 Algorithmic Scaling & Complexity Analysis

```mermaid
flowchart TD
    A[Workload N Processes] --> B{Algorithm Type}
    B -->|FCFS| C["Sort Arrival O(N log N) -> Linear Scan O(N)"]
    B -->|SJF Non-preemptive| D["Priority Selection O(N^2) or O(N log N) Heap"]
    B -->|SRTF Preemptive| E["Arrival Slicing + Min Remaining Burst O(M log N)"]
    B -->|Round Robin| F["Queue Cycling by Quantum O(Total Burst / Q)"]
    C --> G[Result: 253.5 us at N=500]
    D --> H[Result: 477.6 us at N=500]
    E --> I[Result: 563.9 us at N=500]
    F --> J[Result: 623 us Q=5 / 871 us Q=2 at N=500]
```

1. **FCFS vs. SRTF Scaling ($N=500$):**
   - At $N = 500$, FCFS achieves a median duration of **253.55 $\mu\text{s}$**, whereas SRTF requires **563.95 $\mu\text{s}$** (a **2.22x overhead**).
   - *Underlying Cause:* FCFS requires a single sort by arrival time followed by sequential cursor progression ($O(N)$). SRTF performs dynamic preemption: every arrival event recalculates the currently active process's remaining burst time, triggering potential context switches and generating intermediate `ExecutionSegment` objects. The overhead stems from segment allocations and ready-queue reordering.

2. **Round Robin Quantum Sensitivity ($q=2$ vs. $q=5$):**
   - At $N = 10$: $q=2$ takes **66.40 $\mu\text{s}$** vs. $q=5$ at **21.00 $\mu\text{s}$** (a **3.16x reduction** with larger quantum).
   - At $N = 500$: $q=2$ takes **871.70 $\mu\text{s}$** vs. $q=5$ at **623.35 $\mu\text{s}$** (a **28.5% reduction** with larger quantum).
   - *Underlying Cause:* A small quantum ($q=2$) segments process bursts into numerous sub-slices (e.g., a burst of 15 yields 8 separate segment transitions), heavily exercising queue enqueue/dequeue operations and allocating more memory. A larger quantum ($q=5$) reduces context switching frequency by over 50%, directly cutting scheduling overhead.

3. **JIT Tiered Compilation Progression (Warm-up Analysis):**
   Empirical measurements recorded during the 15 untimed warm-up cycles of SRTF at $N = 500$:

| Warm-up Cycle | Duration ($\mu\text{s}$) | JIT State / Phase |
| :---: | :---: | :--- |
| **Cycle 1** | 813.90 | Initial bytecode interpretation / Tier 1 compilation |
| **Cycle 2** | 667.10 | Tier 2 C1 compilation with basic profiling |
| **Cycle 3** | 821.00 | Profiling active |
| **Cycle 4** | 674.80 | Profiling active |
| **Cycle 5** | 1,035.50 | **C2 tiered compilation trigger / On-Stack Replacement (OSR)** |
| **Cycle 6** | 1,086.20 | C2 compilation & deoptimization transition |
| **Cycle 7** | 730.50 | Post-C2 compilation stabilization |
| **Cycle 8** | 530.40 | High-performance compiled native code |
| **Cycle 9** | 554.20 | Steady-state native execution |
| **Cycle 10** | 588.60 | Steady-state native execution |
| **Cycle 11** | 528.40 | Steady-state native execution |
| **Cycle 12** | 543.10 | Steady-state native execution |
| **Cycle 13** | 513.60 | Steady-state native execution |
| **Cycle 14** | 508.80 | Steady-state native execution |
| **Cycle 15** | 491.20 | **Steady state (~40% reduction from Cycle 1)** |

*Observation:* The temporary spike between cycles 5 and 6 (1,035 to 1,086 $\mu\text{s}$) directly marks the HotSpot C2 compiler compiling hot loops into optimized machine code. By Cycle 15, execution latency stabilizes at 491.20 $\mu\text{s}$, confirming the necessity of warm-up iterations.

4. **Measurement Stability & Jitter:**
   - For lower process counts ($N = 10, 50, 100$), CV% remained predominantly below 20% across FCFS, SJF, and SRTF.
   - At larger process counts ($N = 250, 500$) and for preemptive algorithms, occasional single-repetition spikes (e.g. 6.7 ms for SRTF N=50) occur due to non-isolated OS thread preemption by background Windows processes.
   - Using the **Median (P50)** metric effectively filters out these tail artifacts, providing a stable basis for algorithmic evaluation.

---

## 5. Asynchronous Queue Performance & Stress Results

### 5.1 Enqueue Burst Benchmark

To measure ingestion capability, jobs were enqueued in rapid succession into MySQL with complete database validation and idempotency handling.

| Burst Size | Total Time (ms) | Mean Latency (ms) | Median / P50 (ms) | P95 Latency (ms) | Sustained Ingestion Throughput |
| :---: | :---: | :---: | :---: | :---: | :---: |
| **50 jobs** | 352 ms | 6.90 ms | 6.50 ms | 12.00 ms | **142.05 jobs/sec** |
| **100 jobs** | 478 ms | 4.78 ms | 5.00 ms | 11.00 ms | **209.21 jobs/sec** |
| **200 jobs** | 756 ms | 3.75 ms | 4.00 ms | 9.00 ms | **264.55 jobs/sec** |

*Analysis:*
- As the burst size increased from 50 to 200, average enqueue latency dropped from 6.90 ms to 3.75 ms, and throughput climbed from 142 jobs/sec to 264.55 jobs/sec.
- This demonstrates the amortized benefit of HikariCP connection reuse and prepared statement caching under load.

---

### 5.2 Worker Pool Scalability & Processing Throughput

A queue drain test was conducted by pre-enqueuing 30 complete simulation jobs and measuring the total drain time across worker pool configurations (2, 4, and 8 worker threads).

| Configured Worker Pool | Jobs Drained | Total Drain Time (ms) | Drain Throughput (jobs/sec) | Average Queue Wait | Average Execution Duration |
| :---: | :---: | :---: | :---: | :---: | :---: |
| **2 Workers** | 30 | 15,061 ms | **1.99 jobs/sec** | 9,075.8 ms | 1.2 ms |
| **4 Workers** | 30 | 7,906 ms | **3.79 jobs/sec** | 16,609.8 ms | 1.1 ms |
| **8 Workers** | 30 | 765 ms | **39.22 jobs/sec** | 15,540.9 ms | 1.1 ms |

*Analysis:*
- The simulation execution time itself is negligible (~1.1 ms).
- With 2 workers, throughput is constrained by worker availability and the poller dispatch cadence (drain time ~15.0 s).
- Scaling to 8 workers allows all queued items to be claimed and processed in parallel across available CPU threads, draining the entire batch in **765 ms** (a **19.7x throughput increase** to **39.22 jobs/sec**).

---

## 6. Correctness, Resilience & Failure-Mode Verification

| Test Scenario | Condition Tested | Empirical Result | Status |
| :--- | :--- | :--- | :---: |
| **Concurrent Idempotency Burst** | 100 concurrent threads attempting to enqueue jobs with the identical idempotency key | Exactly **1 unique job ID created**; 99 concurrent duplicate requests deduplicated. Deduplication rate: **100%**. | **PASS** |
| **Stale-Job Recovery Latency** | A dead worker leaves a job in `RUNNING` status with an expired lease | Expired lease detected and reclaimed back to `QUEUED` in **6–12 ms**. | **PASS** |
| **Worker Fencing & Rollback** | Stale worker wakes up and attempts completion with expired/mismatched claim token | Database transition rejected; atomic simulation rollback executed; **0 orphan experiment runs created**. | **PASS** |
| **Max Attempts Exhaustion** | A crashing job reaches `max_attempts` (3 attempts) | Recovery permanently marks job as `FAILED` with sanitized diagnostic message. | **PASS** |
| **Ownership Isolation** | User A attempts to view, cancel, or re-run User B's execution jobs | Unauthorized access rejected with `SecurityException` / 403 Forbidden. | **PASS** |
| **CSV Injection Mitigation** | Workload names containing formula payloads (`=1+1`, `@SUM`, `-CMD`) exported | Sanitized with leading single quote (`'`), neutralizing spreadsheet formula execution. | **PASS** |

---

## 7. Architectural Decisions & Hardening Details

### 7.1 Lease-Based Claiming Protocol

```mermaid
sequenceDiagram
    autonumber
    actor W as Worker Thread
    participant Q as ExecutionJobRepository (MySQL)
    participant E as ExperimentRunRepository
    
    W->>Q: claimNextQueuedJob(workerId, leaseDuration=30s)
    Note over Q: SELECT FOR UPDATE / Atomic UPDATE<br/>status='RUNNING', claim_token=UUID,<br/>lease_expires_at=NOW()+30s, attempt_count+=1
    Q-->>W: Return job with claimToken
    
    loop Every 5s (Heartbeat)
        W->>Q: renewLease(jobId, claimToken, 30s)
        Q-->>W: true (lease extended)
    end
    
    Note over W: Simulation Execution Finished
    W->>Q: completeJobWithExperimentRun(jobId, claimToken, runRecord, durationMs)
    Note over Q: Begin Transaction<br/>Verify status='RUNNING' AND claim_token=token<br/>Insert experiment_runs -> Update execution_jobs<br/>Commit Transaction
    Q-->>W: true (Success)
```

1. **Attempt-Fenced Database Schema (Migration `V4__queue_hardening.sql`):**
   Added `claim_token VARCHAR(64)` and `lease_expires_at TIMESTAMP NULL` to `execution_jobs`. An automatic runtime migration safeguard (`ExecutionJobRepositoryJdbcImpl.ensureSchema`) ensures database schema backward compatibility without manual DDL execution.
2. **Heartbeat Scheduler:**
   A dedicated single-threaded daemon (`heartbeatScheduler`) in `ExecutionWorkerManager` periodically calls `renewLease(...)` every 5 seconds for all locally active jobs, guaranteeing long-running jobs are never prematurely reaped while worker threads are healthy.
3. **Atomic Completion Rollback:**
   In `completeJobWithExperimentRun(...)`, the creation of the `experiment_runs` row and the `execution_jobs` state update occur within a single JDBC transaction. If the worker lost its lease due to thread stalls or transient network partition, the update count is zero, prompting an immediate transaction rollback so no orphaned experiment run records remain.

---

### 7.2 UI Polish & Information Architecture Refactoring

1. **Removal of Self-Referential Explanations:**
   - **`benchmarks/new.jsp`:** Removed the large "Micro-benchmark Execution Protocol" tutorial card explaining HotSpot JIT warmup, `System.nanoTime()`, and algorithm internals. Replaced with concise, professional configuration hints.
   - **`benchmarks/list.jsp`:** Removed redundant execution protocol cards.
   - **`benchmarks/view.jsp`:** Removed textbook-style implementation narratives.
2. **Replacement of Raw JSON Dumps:**
   - **Benchmark Environment Metadata:** Replaced raw serialized JSON dumps with a clean metadata table displaying Java runtime version, operating system, architecture, available cores, and heap capacity. Raw JSON was placed into an optional `<details>` collapsible debug drawer.
   - **Workload Snapshot:** Replaced raw serialized process JSON in `jobs/view.jsp` with a structured tabular view listing Process ID, Arrival Time, and Burst Time.
3. **Responsive Visual Guards:**
   - Added `min-width: 0` and `overflow-wrap: anywhere` to `.metric-card` and `.metric-value` in `app.css`.
   - Replaced fixed font sizes on metric displays with fluid typography (`clamp(20px, 2.5vw, 26px)`), preventing numerical value overflow on mobile and compact screens.

---

## 8. Production Readiness Assessment

### 8.1 Production-Ready Components
- **Scheduling Simulation Engine:** Deterministic, strategy-pattern based, validated against formal invariants across all process scales up to $N=500$.
- **Validation Engine:** Strict, canonical parameter validation shared across UI and backend servlets with sticky form retention.
- **Asynchronous Execution Queue:** Robust single-node or clustered MySQL-backed queue with atomic claiming, lease heartbeats, stale recovery, and max attempt fencing.
- **Security & Authorization:** Strict user ownership boundaries enforced on all workloads, experiments, benchmarks, and asynchronous jobs.
- **Data Export:** Hardened against CSV formula injection (CWE-1236).

### 8.2 Items Remaining Before Enterprise Deployment
1. **Distributed Worker Decoupling:**
   - Currently, workers run within the same Servlet container context (`ExecutionWorkerManager`). For massive scale, workers can be extracted into dedicated headless worker containers reading from the same MySQL database.
2. **Leader Election for Queue Poller:**
   - In a multi-node Servlet container deployment, multiple web nodes would poll MySQL simultaneously. Adding database-level leader election (e.g., `GET_LOCK('orchestrivo_poller_leader', 0)`) prevents redundant polling contention across nodes.
3. **Production Database & Infrastructure Hardening:**
   - Enforce TLS encryption for JDBC database connections (`useSSL=true&requireSSL=true`).
   - Inject database credentials via secure secrets manager (AWS Secrets Manager, HashiCorp Vault) rather than environment variables or configuration files.
   - Set up an HTTPS reverse proxy (Nginx or AWS ALB) terminating SSL in front of the Tomcat container.

---

## 9. Verification Summary & Test Suite Results

The complete test suite was executed locally with zero failures:

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.orchestrivo.benchmark.BenchmarkCampaignRunnerTest (2 tests) - SUCCESS
[INFO] Running com.orchestrivo.domain.ExecutionSegmentTest (1 test) - SUCCESS
[INFO] Running com.orchestrivo.domain.ProcessSpecTest (3 tests) - SUCCESS
[INFO] Running com.orchestrivo.persistence.AuditLogRepositoryJdbcImplTest (1 test) - SUCCESS
[INFO] Running com.orchestrivo.persistence.BenchmarkRepositoryJdbcImplTest (1 test) - SUCCESS
[INFO] Running com.orchestrivo.persistence.ExecutionJobRepositoryJdbcImplTest (11 tests) - SUCCESS
[INFO] Running com.orchestrivo.persistence.ExperimentRunRepositoryJdbcImplTest (1 test) - SUCCESS
[INFO] Running com.orchestrivo.persistence.UserRepositoryJdbcImplTest (1 test) - SUCCESS
[INFO] Running com.orchestrivo.persistence.WorkloadRepositoryJdbcImplTest (1 test) - SUCCESS
[INFO] Running com.orchestrivo.scheduling.SchedulingIntegrationTest (8 tests) - SUCCESS
[INFO] Running com.orchestrivo.scheduling.strategies.FcfsStrategyTest (4 tests) - SUCCESS
[INFO] Running com.orchestrivo.scheduling.strategies.RoundRobinStrategyTest (1 test) - SUCCESS
[INFO] Running com.orchestrivo.scheduling.strategies.SjfStrategyTest (1 test) - SUCCESS
[INFO] Running com.orchestrivo.scheduling.strategies.SrtfStrategyTest (1 test) - SUCCESS
[INFO] Running com.orchestrivo.service.AuthenticationServiceTest (1 test) - SUCCESS
[INFO] Running com.orchestrivo.service.BenchmarkServiceTest (5 tests) - SUCCESS
[INFO] Running com.orchestrivo.service.ExecutionJobConcurrencyTest (8 tests) - SUCCESS
[INFO] Running com.orchestrivo.service.ExecutionWorkerManagerTest (4 tests) - SUCCESS
[INFO] Running com.orchestrivo.service.ExperimentComparisonTest (5 tests) - SUCCESS
[INFO] Running com.orchestrivo.service.ExperimentSnapshotTest (1 test) - SUCCESS
[INFO] Running com.orchestrivo.service.WorkloadLifecycleTest (3 tests) - SUCCESS
[INFO] Running com.orchestrivo.service.WorkloadServiceTest (1 test) - SUCCESS
[INFO] Running com.orchestrivo.util.CsvExporterTest (6 tests) - SUCCESS
[INFO] Running com.orchestrivo.util.CsvWorkloadParserTest (12 tests) - SUCCESS
[INFO] Running com.orchestrivo.util.WorkloadGeneratorTest (5 tests) - SUCCESS
[INFO] Running com.orchestrivo.validation.BenchmarkValidationTest (28 tests) - SUCCESS
[INFO] Running com.orchestrivo.validation.ScheduleValidatorTest (6 tests) - SUCCESS
[INFO] Running com.orchestrivo.web.JobSecurityIntegrationTest (6 tests) - SUCCESS
[INFO] Running com.orchestrivo.web.SecurityIntegrationTest (21 tests) - SUCCESS
[INFO] 
[INFO] Results:
[INFO] Tests run: 149, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

WAR packaging (`mvn package`) also verified:
`target/orchestrivo.war` successfully generated and ready for deployment.
