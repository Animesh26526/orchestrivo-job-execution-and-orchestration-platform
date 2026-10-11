# Orchestrivo — Stage 4 Acceptance Report
**Project:** Orchestrivo Job Execution and Orchestration Platform  
**Stage:** Stage 4 — Complete UI Modernization + Reliability Engineering + Persistent Background Execution Engine  
**Date:** October 11, 2026  
**Environment:** Java 17, Jakarta Servlet 6.0, JSP 3.1, JDBC (HikariCP), MySQL 8.0, Apache Tomcat 10.1.60, Windows 11  
**Build & Test Status:** 110/110 Automated Tests Passing (100% Success, 0 Failures, 0 Errors, 0 Regressions)

---

## 1. Executive Summary

Stage 4 of **Orchestrivo** has been completed and verified. This milestone elevates the platform from an isolated prototype to an enterprise-grade analytics suite and persistent orchestration engine without introducing heavy frameworks (such as Spring Boot, Hibernate/JPA, or external queue brokers like Kafka or RabbitMQ). 

### Key Accomplishments Across the Four Primary Workstreams:
1. **Workstream A — Complete UI Modernization**:
   - Built a comprehensive, modular design system (`assets/css/app.css` and `assets/js/app.js`) defining strict color tokens, card layouts, data tables, typography, metric summary cards, and responsive navigation shells (`WEB-INF/views/layout/navbar.jsp`).
   - Upgraded all 13 existing JSP templates across Auth, Dashboard, Workloads, Experiments, and Benchmarks while preserving all JSTL assertions (`<c:out>`) and semantic forms.
   - Built an interactive, color-coded Gantt timeline with clear idle segment depiction, proportional sizing, and a complete tabular alternative for full accessibility (WCAG / screen reader compliance).
   - Embedded pure inline SVG charts for benchmark latency distributions with zero external runtime dependencies.
   - Explicitly clarified telemetry distinctions between theoretical CPU scheduling ticks and actual host Java execution latency.

2. **Workstream B — Reliability & Concurrency Engineering**:
   - Engineered atomic job lease claiming via conditional SQL updates (`UPDATE execution_jobs SET status='RUNNING', claimed_by=?, claimed_at=NOW() WHERE id=? AND status='QUEUED'`).
   - Verified concurrency safety with a multi-threaded test suite executing 10 competing worker threads across 5 queued jobs without duplicate claims or lost updates.
   - Integrated client-side double-submission guards and database-backed idempotency key deduplication.
   - Implemented automated stale lease recovery to safely rescue jobs stranded by crashed workers.
   - Strengthened tenant isolation across all job lifecycle actions (`view`, `status`, `retry`, `cancel`).

3. **Workstream C — Persistent Asynchronous Execution Engine**:
   - Authored database schema migration `V3__execution_jobs.sql`, creating the indexed `execution_jobs` table in MySQL.
   - Designed and deployed `ExecutionWorkerManager`, an autonomous background daemon service with a bounded thread pool and scheduled polling.
   - Integrated clean startup and graceful shutdown hooks via `AppConfigListener` to eliminate thread leaks and connection pool hangs on Tomcat reload.
   - Switched experiment execution to asynchronous background queuing by default, while retaining `?mode=sync` for synchronous fallback and testing.
   - Built high-performance JSON status polling (`/jobs/status`) with automatic UI state updates and navigation transitions.

4. **Workstream D — Observability & Queue Metrics**:
   - Exposed live aggregate queue telemetry (queued, running, completed, and failed counts, average queue wait duration, and engine execution latencies).
   - Displayed real metrics directly on the Dashboard and Queue Management views (`/jobs`).
   - Maintained full operational audit trails tracking all queue and worker operations with automatic credential/token scrubbing.

---

## 2. Baseline Audit & Pre-Implementation State

### Pre-Implementation Baseline (Stage 3):
- **Automated Tests:** 89 tests passing across domain, scheduling, validation, persistence, benchmarks, and web security.
- **Database Schema:** Tables `users`, `workloads`, `processes`, `experiment_runs`, `benchmark_runs`, `benchmark_measurements`, and `audit_logs` (Migrations `V1__init_schema.sql` and `V2__stage3_features.sql`).
- **Execution Mechanism:** Entirely synchronous inside `ExperimentServlet.doPost()`, blocking Tomcat request threads during CPU scheduling simulation calculations.
- **Frontend State:** Disconnected, unstyled JSP views lacking a cohesive design system, unified layout shell, or consistent navigation.

### Identified Bottlenecks & Gaps in Baseline:
1. **Thread Starvation Risk:** Heavy workloads run synchronously could block HTTP container worker threads, leading to request queuing under load.
2. **Crash Fragility:** If the server crashed during a long-running simulation, no persistent state recorded what was in flight.
3. **Lack of Idempotency:** Rapid double-clicks by users created duplicate `experiment_runs` records.
4. **Disjointed UI/UX:** No visual hierarchy, absence of responsive tables, no visual execution Gantt timeline, and lack of visual feedback during simulation runs.

---

## 3. Visual System & Architecture Decisions

### 3.1 Design System Tokens & Conventions (`assets/css/app.css`)
- **Color Palette:**
  - Primary Accent: Deep Engineering Blue (`#2563eb` / `#1d4ed8`)
  - Background Surfaces: Crisp Light Theme (`#f8fafc` canvas, `#ffffff` card surfaces, `#e2e8f0` borders)
  - Typography: Neutral system sans stack (`-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif`) with strict modular scale.
  - Status Semantics: Green (`#16a34a` for `COMPLETED`/`VALID`), Amber (`#d97706` for `QUEUED`/`WARNING`), Blue (`#2563eb` for `RUNNING`), Red (`#dc2626` for `FAILED`/`CANCELLED`).
- **Reusable Component Classes:**
  - Layout: `.app-navbar`, `.nav-inner`, `.nav-brand`, `.app-container`, `.breadcrumb-bar`, `.page-header`.
  - Cards & Grids: `.card`, `.card-header`, `.metrics-grid`, `.metric-card`, `.form-grid-2`.
  - Forms: `.form-group`, `.form-label`, `.form-input`, `.form-select`, `.form-hint`.
  - Tables: `.table-responsive`, `.data-table`, `.badge`, `.actions-cell`.
  - Empty States: `.empty-state`, `.empty-state-icon`, `.empty-state-title`.
  - Alerts: `.alert`, `.alert-info`, `.alert-success`, `.alert-warning`, `.alert-danger`.

### 3.2 Client Script Architecture (`assets/js/app.js`)
- **Double-Submit Protection:** Attaches submit handlers to forms, disabling submission buttons and rendering spinner states to eliminate race conditions on slow connections.
- **Dynamic Process Table Rows:** Allows adding and removing process specification rows in manual workload creation/editing without page reloads.
- **Dynamic Algorithm Quantum Toggle:** Automatically displays the Time Quantum field when `ROUND_ROBIN` is selected, hiding it for non-quantum algorithms.
- **Asynchronous Status Poller (`startJobStatusPolling`):** Polls `/jobs/status?id={jobId}` every 1000ms with progressive status badges. When `COMPLETED`, redirects directly to `/experiments/view?id={runId}`.

### 3.3 Visual Timeline & Gantt Architecture
- **Proportional Segmentation:** Calculates percentage widths dynamically based on `segmentDuration / totalDuration * 100`.
- **Deterministic Color Assignment:** Computes deterministic color swatches derived from `processId.hashCode()` so processes maintain uniform identity across Gantt timeline bars.
- **Explicit Idle CPU Blocks:** Idle intervals (where no process has arrived) are rendered in textured slate grey (`.gantt-block.idle`) with tooltips.
- **Dual Accessibility Representation:** A corresponding tabular view (`Execution Timeline Segments (Tabular Alternative)`) is rendered beneath the Gantt chart for screen readers and high-precision inspection.

---

## 4. Route & Page Inventory

| Route | HTTP Method | JSP View | Controller / Servlet | Purpose & Key Features |
| :--- | :--- | :--- | :--- | :--- |
| `/login` | GET, POST | `auth/login.jsp` | `LoginServlet` | Authentication, OWASP session rotation, CSRF check. |
| `/register` | GET, POST | `auth/register.jsp` | `RegisterServlet` | User registration, password hashing via BCrypt, auto-login. |
| `/logout` | POST | N/A (Redirect) | `LogoutServlet` | Secure session invalidation and cookie clearing. |
| `/dashboard` | GET | `dashboard/index.jsp` | `DashboardServlet` | System overview, live queue metrics, recent jobs, audit log. |
| `/workloads` | GET | `workloads/list.jsp` | `WorkloadServlet` | Workload listing, pagination, sorting, search, empty state. |
| `/workloads/new` | GET, POST | `workloads/new.jsp` | `WorkloadServlet` | Manual workload specification, dynamic row adder. |
| `/workloads/view` | GET | `workloads/view.jsp` | `WorkloadServlet` | Workload details, process specifications, experiment dispatcher. |
| `/workloads/edit` | GET, POST | `workloads/edit.jsp` | `WorkloadServlet` | Workload metadata and process editor. |
| `/workloads/import` | GET, POST | `workloads/import.jsp` | `WorkloadServlet` | RFC-4180 CSV upload and textarea paste, row-level error log. |
| `/workloads/generate` | GET, POST | `workloads/generate.jsp` | `WorkloadServlet` | Seeded pseudo-random deterministic workload generator. |
| `/workloads/export` | GET | N/A (CSV Stream) | `WorkloadServlet` | CWE-1236 hardened CSV download. |
| `/workloads/duplicate` | POST | N/A (Redirect) | `WorkloadServlet` | Deep-cloning of workloads and processes. |
| `/workloads/delete` | POST | N/A (Redirect) | `WorkloadServlet` | Ownership-enforced workload deletion. |
| `/experiments` | GET | `experiments/list.jsp` | `ExperimentServlet` | Historical experiment runs, status badges, compare selector. |
| `/experiments/run` | POST | N/A (Redirect) | `ExperimentServlet` | Asynchronous job queuing (`?mode=sync` fallback supported). |
| `/experiments/view` | GET | `experiments/view.jsp` | `ExperimentServlet` | Full execution details, Gantt timeline, metrics, snapshot. |
| `/experiments/compare` | GET | `experiments/compare.jsp` | `ExperimentServlet` | Multi-algorithm side-by-side comparison with winner highlights. |
| `/experiments/export` | GET | N/A (CSV Stream) | `ExperimentServlet` | Export run execution metrics to CSV. |
| `/experiments/delete` | POST | N/A (Redirect) | `ExperimentServlet` | Ownership-enforced experiment run deletion. |
| `/jobs` | GET | `jobs/list.jsp` | `JobServlet` | Background execution queue dashboard, status filter, paged list. |
| `/jobs/view` | GET | `jobs/view.jsp` | `JobServlet` | Detailed job inspect view, live poller, attempt logs, error traces. |
| `/jobs/status` | GET | N/A (JSON) | `JobServlet` | JSON telemetry endpoint for asynchronous AJAX polling. |
| `/jobs/retry` | POST | N/A (Redirect) | `JobServlet` | Manual retry action for failed execution jobs. |
| `/jobs/cancel` | POST | N/A (Redirect) | `JobServlet` | User cancellation for queued background jobs. |
| `/benchmarks` | GET | `benchmarks/list.jsp` | `BenchmarkServlet` | Historical micro-benchmark suite results. |
| `/benchmarks/new` | GET, POST | `benchmarks/new.jsp` | `BenchmarkServlet` | Benchmark parameter configuration (iterations, warm-up). |
| `/benchmarks/view` | GET | `benchmarks/view.jsp` | `BenchmarkServlet` | Statistical summaries, inline SVG latency distribution chart. |
| `/benchmarks/export` | GET | N/A (CSV Stream) | `BenchmarkServlet` | Benchmark iteration measurement CSV export. |
| `/benchmarks/delete` | POST | N/A (Redirect) | `BenchmarkServlet` | Ownership-enforced benchmark deletion. |

---

## 5. Workload Management Enhancements

1. **Structured Process Editor:**
   - Redesigned `/workloads/new` and `/workloads/edit` to render tabular forms with individual rows for `Process ID`, `Arrival Time`, and `Burst Time`.
   - Client-side validation prevents negative arrivals or zero/negative bursts prior to dispatch.
2. **CSV Import UX:**
   - Explicit instructions provided for header structure (`processId,arrivalTime,burstTime`).
   - Copyable sample CSV templates built directly into the UI.
   - Row-level parsing feedback rendering detailed failure messages (e.g., `Row 4: Arrival time cannot be negative: -2`).
3. **Deterministic Generator Usability:**
   - `/workloads/generate` includes explanatory notices on random seed reproducibility.
   - Input controls bounded with validation parameters (process count 1–200, seed, min/max arrivals, min/max bursts).
4. **Formula Injection Hardening (CWE-1236):**
   - Verified across all CSV exports (`/workloads/export`, `/experiments/export`, `/benchmarks/export`). Cells prefixed with single quotes (`'`) if starting with `=`, `+`, `-`, `@`, `\t`, or `\r`.

---

## 6. Experiment Execution & Visualization

### 6.1 Interactive Gantt Timeline
- Execution schedules produced by scheduling strategies (`FcfsStrategy`, `SjfStrategy`, `SrtfStrategy`, `RoundRobinStrategy`) are rendered dynamically.
- Consecutive execution intervals of identical processes are preserved to accurately reflect algorithm preemption.
- Dedicated idle segments visually communicate CPU bubbles where ready queues were empty.
- Fully proportional width allocation with clear time markers at `t=0`, `t=midpoint`, and `t=completion`.

### 6.2 Domain vs Host Engine Latency Clarification
- Visual alert badges and documentation clearly distinguish between:
  - **Simulated Metrics:** Theoretical CPU clock units (Turnaround Time, Waiting Time, Response Time, Context Switch Count).
  - **Host Runtime Metrics:** Wall-clock Java execution latency measured in milliseconds/nanoseconds by `System.nanoTime()`.

### 6.3 Immutable Input Snapshot Inspection
- Every experiment run view provides a dedicated table displaying the immutable JSON input snapshot captured at submission time.
- Guarantees complete auditability and mathematical reproducibility even if the originating workload is mutated or deleted.

---

## 7. Persistent Queue & Background Engine Architecture

### 7.1 Database Table Schema (`execution_jobs`)
```sql
CREATE TABLE IF NOT EXISTS execution_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_user_id BIGINT NOT NULL,
    workload_id BIGINT NOT NULL,
    algorithm_name VARCHAR(64) NOT NULL,
    algorithm_parameters TEXT,
    input_snapshot MEDIUMTEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'QUEUED',
    attempt_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 3,
    idempotency_key VARCHAR(128),
    error_message TEXT,
    claimed_by VARCHAR(128),
    claimed_at DATETIME,
    started_at DATETIME,
    completed_at DATETIME,
    queue_wait_ms BIGINT,
    execution_duration_ms BIGINT,
    experiment_run_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_exec_jobs_user FOREIGN KEY (owner_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_exec_jobs_workload FOREIGN KEY (workload_id) REFERENCES workloads(id) ON DELETE CASCADE,
    CONSTRAINT fk_exec_jobs_experiment FOREIGN KEY (experiment_run_id) REFERENCES experiment_runs(id) ON DELETE SET NULL,
    INDEX idx_exec_jobs_status (status),
    INDEX idx_exec_jobs_owner_status (owner_user_id, status),
    INDEX idx_exec_jobs_claimed (claimed_at),
    UNIQUE INDEX uq_exec_jobs_idempotency (owner_user_id, idempotency_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 7.2 Background Worker Engine (`ExecutionWorkerManager`)
- **Bounded Worker Pool:** Configured with `Executors.newFixedThreadPool(workerThreads, threadFactory)` using daemon threads named `orchestrivo-worker-#`.
- **Scheduled Poller:** Dedicated single-thread scheduled executor runs every 1000ms checking for queued tasks.
- **Worker Lease Identification:** Each engine instance generates a unique host identity (e.g. `worker-host-a1b2c3d4`).
- **Autonomous Recovery:** Worker periodically executes `recoverStaleJobs(120)` to reclaim jobs stranded in `RUNNING` status whose claiming lease has expired.
- **Tomcat Lifecycle Management:** Registered in `AppConfigListener`:
  - `contextInitialized`: Initializes DB pool, repositories, and starts the `ExecutionWorkerManager`.
  - `contextDestroyed`: Gracefully stops the worker manager, terminating polling, waiting up to 5 seconds for in-flight tasks to finish, and shutting down HikariCP cleanly.

```
+-----------------------------------------------------------------------------------+
|                            HTTP Request (Tomcat)                                  |
|   User submits Experiment -> ExperimentServlet -> ExecutionJobService.enqueue()   |
+-----------------------------------------------------------------------------------+
                                         |
                                         v  INSERT (status='QUEUED')
+-----------------------------------------------------------------------------------+
|                             MySQL: execution_jobs                                 |
+-----------------------------------------------------------------------------------+
                                         ^
                                         |  claimNextQueuedJob()
+-----------------------------------------------------------------------------------+
|               ExecutionWorkerManager (Scheduled Poll: 1000ms)                     |
|                                                                                   |
|   +-------------------+   +-------------------+   +---------------------------+   |
|   |  Worker Thread 1  |   |  Worker Thread 2  |   |  Stale Recovery (120s)    |   |
|   +-------------------+   +-------------------+   +---------------------------+   |
|             |                       |                                             |
|             v                       v                                             |
|     Deserialize Snapshot -> Run Strategy -> ScheduleValidator -> Save Experiment   |
+-----------------------------------------------------------------------------------+
                                         |
                                         v  UPDATE (status='COMPLETED', duration_ms)
+-----------------------------------------------------------------------------------+
|                        MySQL: experiment_runs & execution_jobs                    |
+-----------------------------------------------------------------------------------+
```

---

## 8. Concurrency, Race Condition, & Failure Analysis

### 8.1 Atomic Job Claiming
Race condition analysis: Multiple worker threads attempting to claim the same queued job simultaneously could cause duplicate executions.
- **Mitigation:** In `ExecutionJobRepositoryJdbcImpl.claimNextQueuedJob(workerId)`:
  1. Identifies candidates: `SELECT id FROM execution_jobs WHERE status = 'QUEUED' ORDER BY id ASC LIMIT 5`.
  2. Executes atomic conditional update:
     ```sql
     UPDATE execution_jobs 
     SET status = 'RUNNING', claimed_by = ?, claimed_at = NOW(), 
         started_at = IFNULL(started_at, NOW()), 
         queue_wait_ms = IFNULL(queue_wait_ms, TIMESTAMPDIFF(MICROSECOND, created_at, NOW()) / 1000), 
         attempt_count = attempt_count + 1 
     WHERE id = ? AND status = 'QUEUED'
     ```
  3. Only the worker thread that receives `rowsUpdated == 1` acquires the lease and processes the job. All other competing threads receive 0 and move to the next candidate.
- **Verification:** Verified via automated integration test `testConcurrentAtomicClaiming` where 10 concurrent threads competed for 5 queued jobs simultaneously; exactly 5 unique claims were made with zero duplicates.

### 8.2 Client Double-Submission & Idempotency
- **Client Side:** Buttons disabled upon submission with visual spinner indicators.
- **Server Side:** Database enforces unique constraint `UNIQUE INDEX uq_exec_jobs_idempotency (owner_user_id, idempotency_key)`. Replayed requests return the existing job record immediately without duplicate queue insertions.

### 8.3 Safe Retries & Stale Job Recovery
- **Automatic Retries:** Transient worker exceptions decrement attempts remaining. If `attempt_count < max_attempts`, job transitions back to `QUEUED`.
- **Permanent Failure:** Semantic validation errors (e.g. empty process list) fail permanently immediately with error diagnostics (`status = 'FAILED'`).
- **Stale Lease Recovery:** Jobs claimed by dead worker nodes (> 120 seconds old) are automatically re-queued.

---

## 9. Benchmark & Observability Findings

### 9.1 Live Queue Observability
The platform exposes real queue metrics calculated via aggregate SQL queries:
- **Total Queued Jobs** (`COUNT(*) WHERE status = 'QUEUED'`)
- **Total Running Jobs** (`COUNT(*) WHERE status = 'RUNNING'`)
- **Total Completed Jobs** (`COUNT(*) WHERE status = 'COMPLETED'`)
- **Total Failed Jobs** (`COUNT(*) WHERE status = 'FAILED'`)
- **Average Queue Wait Duration** (`AVG(queue_wait_ms)`)
- **Average Execution Duration** (`AVG(execution_duration_ms)`)

All numbers on the Dashboard (`/dashboard`) and Queue Management (`/jobs`) are populated strictly from real database queries. No mock or hardcoded statistics are present.

### 9.2 Micro-Benchmarking Engine
- Micro-benchmarks record wall-clock execution of scheduling algorithms across customizable iterations (e.g. 50 iterations with 10 warm-up runs).
- Benchmark view displays statistical summaries: Minimum, Median, Maximum, and Standard Deviation in microseconds (µs) and milliseconds (ms).
- Per-iteration latency distribution is rendered via pure inline SVG bar charts.

---

## 10. Security & Tenant Isolation Verification

1. **Strict Ownership Enforcement:**
   - Evaluated across `WorkloadRepository`, `ExperimentRunRepository`, and `ExecutionJobRepository`.
   - All query mutations enforce `WHERE id = ? AND owner_user_id = ?`. Foreign users receive HTTP 403 Forbidden or empty results.
2. **CSRF Protection:**
   - Cryptographically random session tokens (`UUID.randomUUID().toString()`) validated on all POST, PUT, DELETE routes.
   - Tested and verified via `JobSecurityIntegrationTest.testCsrfRejectionOnJobRetry` and `testCsrfRejectionOnJobCancel`.
3. **Session ID Rotation (OWASP):**
   - `req.changeSessionId()` executed upon authentication in `LoginServlet` and `RegisterServlet` to prevent session fixation.
4. **Parameterized SQL Queries:**
   - All database access uses JDBC `PreparedStatement` with bind variables, completely neutralizing SQL injection risks.
5. **XSS Prevention:**
   - All dynamic JSP outputs utilize JSTL `<c:out value="${...}"/>` with automatic HTML entity escaping.

---

## 11. Automated Test Suite Results

The automated test suite expanded from **89 tests** in Stage 3 to **110 tests** in Stage 4. All tests pass with zero failures and zero errors.

### Test Execution Summary (`mvn test`)
```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.orchestrivo.domain.ExecutionSegmentTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.orchestrivo.domain.ProcessSpecTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.orchestrivo.persistence.AuditLogRepositoryJdbcImplTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.orchestrivo.persistence.BenchmarkRepositoryJdbcImplTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.orchestrivo.persistence.ExecutionJobRepositoryJdbcImplTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.orchestrivo.persistence.ExperimentRunRepositoryJdbcImplTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.orchestrivo.persistence.UserRepositoryJdbcImplTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.orchestrivo.persistence.WorkloadRepositoryJdbcImplTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.orchestrivo.scheduling.SchedulingIntegrationTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.orchestrivo.scheduling.strategies.FcfsStrategyTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.orchestrivo.scheduling.strategies.RoundRobinStrategyTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.orchestrivo.scheduling.strategies.SjfStrategyTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.orchestrivo.scheduling.strategies.SrtfStrategyTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.orchestrivo.service.AuthenticationServiceTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.orchestrivo.service.BenchmarkServiceTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.orchestrivo.service.ExecutionWorkerManagerTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.orchestrivo.service.ExperimentComparisonTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.orchestrivo.service.ExperimentSnapshotTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.orchestrivo.service.WorkloadLifecycleTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.orchestrivo.service.WorkloadServiceTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.orchestrivo.util.CsvExporterTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.orchestrivo.util.CsvWorkloadParserTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.orchestrivo.util.WorkloadGeneratorTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.orchestrivo.validation.ScheduleValidatorTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.orchestrivo.web.JobSecurityIntegrationTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.orchestrivo.web.SecurityIntegrationTest
[INFO] Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 110, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### Breakdown of New Stage 4 Tests:
- `ExecutionJobRepositoryJdbcImplTest` (11 tests):
  - `testSaveAndFindById`: Validates CRUD persistence and default values.
  - `testOwnershipEnforcement`: Proves strict tenant access boundaries.
  - `testAtomicClaimingSingleThread`: Confirms worker status transitions (`QUEUED` -> `RUNNING`).
  - `testConcurrentAtomicClaiming`: 10 threads competing for 5 jobs; validates zero duplicate claims.
  - `testMarkCompletedAndDuration`: Validates `COMPLETED` state update, duration recording, and link to experiment run.
  - `testCancellation`: Validates cancellation rules (only queued jobs can be cancelled by owner).
  - `testRetryFlow`: Validates retry transition (`FAILED` -> `QUEUED`) with attempt bounds.
  - `testIdempotencyDeduplication`: Validates idempotent retrieval by key.
  - `testQueueMetrics`: Confirms aggregate counters for user queues.
  - `testStaleJobRecovery`: Validates automated recovery of orphaned leases back to `QUEUED`.
  - `testFindByOwnerPagedWithNullSortAndStatus`: Validates null-safe sorting and filtering defaults.
- `ExecutionWorkerManagerTest` (4 tests):
  - `testExecuteJobSuccessfulFcfs`: Tests full end-to-end background execution for FCFS algorithm.
  - `testExecuteJobRoundRobinWithQuantum`: Validates Round Robin execution with custom quantum parameter.
  - `testExecuteJobWithInvalidSnapshotFailsPermanently`: Confirms permanent failure handling for unparseable input snapshots.
  - `testWorkerManagerLifecycle`: Validates start, immediate polling, and clean shutdown lifecycle.
- `JobSecurityIntegrationTest` (6 tests):
  - `testUnauthenticatedAccessToJobsRedirectsToLogin`: Confirms unauthenticated protection of `/jobs`.
  - `testCsrfRejectionOnJobRetry`: Proves CSRF validation on retry POST requests.
  - `testCsrfRejectionOnJobCancel`: Proves CSRF validation on cancel POST requests.
  - `testJobOwnershipEnforcementOnCancel`: Validates 403 Forbidden on attempting to cancel another user's job.
  - `testJobOwnershipEnforcementOnRetry`: Validates 403 Forbidden on attempting to retry another user's job.
  - `testJobStatusAccessControl`: Validates tenant privacy on JSON status polling.

---

## 12. Database Schema & Migration Verification

### Migration Execution
Migration `V3__execution_jobs.sql` was executed successfully against local MySQL 8 database `orchestrivo`:
```
mysql -u orchestrivo_user -pAnimesh@26526 orchestrivo < src/main/resources/db/V3__execution_jobs.sql
```

### Schema State Verification
Table structure inspected via MySQL CLI:
- Columns: `id`, `owner_user_id`, `workload_id`, `algorithm_name`, `algorithm_parameters`, `input_snapshot`, `status`, `attempt_count`, `max_attempts`, `idempotency_key`, `error_message`, `claimed_by`, `claimed_at`, `started_at`, `completed_at`, `queue_wait_ms`, `execution_duration_ms`, `experiment_run_id`, `created_at`, `updated_at`.
- Foreign Keys:
  - `fk_exec_jobs_user` -> `users(id)` ON DELETE CASCADE
  - `fk_exec_jobs_workload` -> `workloads(id)` ON DELETE CASCADE
  - `fk_exec_jobs_experiment` -> `experiment_runs(id)` ON DELETE SET NULL
- Performance Indexes:
  - `idx_exec_jobs_status` on `(status)`
  - `idx_exec_jobs_owner_status` on `(owner_user_id, status)`
  - `idx_exec_jobs_claimed` on `(claimed_at)`
  - `uq_exec_jobs_idempotency` UNIQUE on `(owner_user_id, idempotency_key)`

---

## 13. Live Deployment & Smoke Verification

### 13.1 Package Build & Tomcat Deployment
1. Built WAR archive:
   ```cmd
   mvn clean package
   ```
   Generated artifact: `target/orchestrivo.war` (Size: ~8.3 MB).
2. Deployed to Apache Tomcat 10.1.60:
   ```powershell
   Copy-Item target\orchestrivo.war "C:\Program Files\Apache Software Foundation\Tomcat 10.1\webapps\orchestrivo.war" -Force
   ```
3. Tomcat auto-redeployed the web application context without errors.

### 13.2 Live End-to-End Workflow Verification
Executed live automated end-to-end smoke tests against running Tomcat service (`http://localhost:8080/orchestrivo`):
1. **User Registration & Login:** Created user `worker_e2e_1791731797207@example.com`, verified password hashing and OWASP session ID rotation.
2. **Workload Generation:** Seeded workload generated (`processCount=4`, `seed=777`), creating Workload #141.
3. **Asynchronous Experiment Enqueue:** Dispatched Round Robin simulation (`quantum=2`) via POST `/experiments/run`, enqueuing Background Job #113.
4. **Autonomous Worker Execution:** Background `ExecutionWorkerManager` detected the queued job, claimed the lease, performed the simulation, validated invariants with `ScheduleValidator`, persisted ExperimentRun #52, and marked Job #113 `COMPLETED` in 1 millisecond.
5. **Live Polling:** Client polling `/jobs/status?id=113` returned:
   ```json
   {"maxAttempts":3,"experimentRunId":52,"executionDurationMs":1,"attemptCount":1,"id":113,"queueWaitMs":0,"status":"COMPLETED"}
   ```
6. **Visualization Verification:** Loaded `/experiments/view?id=52` directly:
   - Proportional Gantt Timeline rendered with distinct colored blocks for processes `P1`, `P2`, `P3`, `P4` and CPU `IDLE` blocks.
   - Screen-reader accessible tabular segment table rendered.
   - Per-process performance metrics table displayed.
   - Immutable JSON snapshot table displayed.

---

## 14. Regressions, Discrepancies, & Risk Register

### 14.1 Regressions Evaluated & Resolved
- **Issue Discovered During Integration Testing:** Stale test records in `execution_jobs` table caused initial test interference with `claimNextQueuedJob`.
  - **Resolution:** Added `@BeforeEach` and `@AfterEach` table cleanups to `ExecutionJobRepositoryJdbcImplTest` and `ExecutionWorkerManagerTest`, ensuring complete test data isolation.
- **Issue Discovered During Smoke Testing:** Java `Set.of(...).contains(sortBy)` threw `NullPointerException` when `sortBy` was omitted (null).
  - **Resolution:** Updated `ExecutionJobRepositoryJdbcImpl.findByOwnerPaged` to use a null-safe check `(sortBy != null && allowedSortCols.contains(sortBy.toLowerCase())) ? sortBy.toLowerCase() : "created_at"`. Added regression test `testFindByOwnerPagedWithNullSortAndStatus`.

### 14.2 Risk Register
1. **Database Contention on Extreme Job Volume:**
   - *Risk:* A high-frequency queue poller executing `SELECT ... WHERE status = 'QUEUED'` every 1000ms creates minor overhead if millions of rows accumulate.
   - *Mitigation:* Indexed `(status)` and bounded candidate search (`LIMIT 5`). Periodic archiving of completed jobs recommended if table exceeds 500,000 rows.
2. **Worker Node Crash During Execution:**
   - *Risk:* In the event of a sudden VM crash or power loss, in-flight jobs remain marked `RUNNING`.
   - *Mitigation:* `ExecutionWorkerManager` includes automated stale lease recovery which resets uncompleted jobs older than 120 seconds back to `QUEUED`.
3. **No Distributed Queue:**
   - *Risk:* This single-process engine runs workers within the Tomcat container JVM.
   - *Mitigation:* Complies with the non-negotiable architectural constraint prohibiting external distributed brokers (Kafka, Redis, RabbitMQ). Scales across CPU cores via configurable worker pool sizing.

---

## 15. Acceptance Sign-Off & Next Steps

### Stage 4 Acceptance Criteria Checklist:
- [x] Shared design system CSS and JS deployed and active across all views.
- [x] Application shell with unified navigation and user info deployed.
- [x] Dashboard updated with real database-backed queue and audit metrics.
- [x] Interactive, accessible Gantt timeline with clear idle segment representation.
- [x] Distinctions between simulated scheduling ticks and Java host latency clarified.
- [x] MySQL-backed persistent job queue (`execution_jobs` table) deployed with foreign keys and indexes.
- [x] Background worker engine (`ExecutionWorkerManager`) operating autonomously with bounded thread pool.
- [x] Atomic job claiming concurrency safety verified under multi-threaded load.
- [x] Job retry protocols, attempt limits, and stale recovery active.
- [x] Full tenant isolation and CSRF protection enforced across all endpoints.
- [x] All 110 automated tests passing with zero failures and zero regressions.
- [x] Live Tomcat 10.1 smoke test verified with full workload generation, job queueing, worker execution, and status polling.

### Recommended Next Steps for Stage 5:
1. **Dynamic Real-Time WebSocket Notifications:** Integrate Jakarta WebSocket API to replace HTTP interval polling for job completion alerts.
2. **Multi-Algorithm Batch Suite Dispatch:** Enable enqueuing all four algorithms (FCFS, SJF, SRTF, RR) in parallel with a single click from the Workload View.
3. **Automated Queue Pruning / Archival Job:** Introduce a daily cron-style maintenance task to archive completed jobs older than 30 days.

---
*Report Certified by: Antigravity AI Pair Programmer & Orchestrivo Engineering Team*
