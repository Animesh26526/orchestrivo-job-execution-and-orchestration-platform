# Orchestrivo

[![Java 17](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://openjdk.org/projects/jdk/17/)
[![Jakarta EE 10](https://img.shields.io/badge/Jakarta_EE-10-orange.svg)](https://jakarta.ee/)
[![Apache Tomcat](https://img.shields.io/badge/Tomcat-10.1%2B-yellow.svg)](https://tomcat.apache.org/)
[![MySQL 8](https://img.shields.io/badge/MySQL-8.0%2B-blue.svg)](https://www.mysql.com/)
[![Tests](https://img.shields.io/badge/Tests-177%20Passing%20(100%25)-brightgreen.svg)]()
[![Zero Mock Fabrication](https://img.shields.io/badge/Metrics-Evidence--Backed%20Only-success.svg)]()

**Orchestrivo** is a high-integrity CPU scheduling algorithm simulator, asynchronous execution job queue, and scientific micro-benchmarking platform implemented in Java 17, Jakarta Servlets, JDBC, and MySQL.

Architected as a transparent, zero-framework modular monolith (no Spring, no Hibernate/JPA), Orchestrivo prioritizes mechanical sympathy, explicit state transitions, strict mathematical correctness, and reproducible performance measurement.

---

## Table of Contents

- [Core Engineering Pillars](#core-engineering-pillars)
- [Scientific Benchmark Methodology](#scientific-benchmark-methodology)
- [Architecture & Design Decisions](#architecture--design-decisions)
- [Dashboard & Telemetry Overview](#dashboard--telemetry-overview)
- [Demo Data Pipeline & Quick Credentials](#demo-data-pipeline--quick-credentials)
- [Directory Structure](#directory-structure)
- [Prerequisites & Quickstart](#prerequisites--quickstart)
- [Automated Verification & Test Suite](#automated-verification--test-suite)
- [Security & Production Hardening](#security--production-hardening)

---

## Core Engineering Pillars

### 1. Pure CPU Scheduling Engine
- **Strategy & Factory Pattern**: Implementations for **First-Come, First-Served (FCFS)**, non-preemptive **Shortest Job First (SJF)**, preemptive **Shortest Remaining Time First (SRTF)**, and **Round Robin (RR)** with configurable time quantum slices.
- **Strict Invariant Verification**: Independent `ScheduleValidator` mathematically audits every generated schedule to guarantee zero overlapping processes on single CPU cores, zero execution before process arrival time, exact burst time fulfillment, and zero premature terminations.
- **Dual Telemetry Separation**: Explicitly separates theoretical CPU scheduling metrics (CPU clock ticks, theoretical throughput in jobs/tick, wait time, turnaround time) from actual host JVM wall-clock execution costs (nanoseconds/microseconds).

### 2. Durable Asynchronous Execution Engine
- **Persistent MySQL-Backed Queue**: State machine tracking `QUEUED`, `RUNNING`, `COMPLETED`, `FAILED`, and `CANCELLED` states across execution jobs with snapshot versioning.
- **Atomic Worker Claims**: Multi-worker concurrency safety via atomic database updates (`UPDATE execution_jobs SET status='RUNNING', claimed_by=?, claimed_at=NOW() WHERE id=? AND status='QUEUED'`).
- **Bounded Worker Pool**: `ExecutionWorkerManager` orchestrates configurable background daemon threads with graceful start/stop integration tied to the servlet container lifecycle (`AppConfigListener`).
- **Lease Recovery & Attempt Caps**: Autonomous detection and recovery of abandoned jobs from crashed workers, with exponential backoff and dead-letter classification upon reaching `max_attempts`.
- **Idempotency & Deduplication**: Client-provided idempotency keys prevent duplicate executions under network retries and replay conditions.

### 3. Accessible Visualization & UI Design System
- **Accessible Gantt Chart**: Color-coded visual timeline with idle CPU state representation and an interactive accessible tabular fallback with screen-reader friendly ARIA attributes.
- **Hierarchical Navigation**: Fully accessible dropdown navbar with keyboard navigation (Esc, Arrow keys), touch support, click-outside dismissal, and mobile hamburger drawer.
- **Modern Dark-Slate Aesthetic**: Clean custom CSS design tokens (`app.css`, `app.js`) without heavy frontend dependencies or bloated libraries.

---

## Scientific Benchmark Methodology

Orchestrivo enforces rigorous benchmark separation to prevent statistical fallacies and JVM microbenchmarking pitfalls:

### 1. Host Benchmark Throughput vs. Latency Estimate
- **Actual Measured Batch Throughput ($\text{runs/sec}$)**:
  $$\text{Throughput}_{\text{actual}} = \frac{N_{\text{valid}}}{T_{\text{elapsed\_batch}}}$$
  Measures the true wall-clock duration of the entire measured iteration loop using monotonic `System.nanoTime()`, strictly excluding warmup cycles.
- **Latency-Derived Throughput Estimate ($\text{runs/sec}$)**:
  $$\text{Throughput}_{\text{estimate}} = \frac{1}{\text{Median Latency}}$$
  Clearly labeled throughout the UI, CSV exports, and code as a synthetic reciprocal estimate rather than an independently measured rate.

### 2. Controlled Comparison Invariants
To ensure scientific validity, candidate comparisons require invariant matching:
- Same algorithm and time quantum (or explicitly flagged variant).
- Same process count, arrival distributions, and burst time characteristics.
- Same deterministic pseudo-random seed (`java.util.Random(seed)`).
- Same warmup iterations and measured repetitions.

### 3. Durable Pinned Canonical Comparison
- The featured baseline/candidate comparison is durably stored in the `system_settings` table (`featured_baseline_run_id`, `featured_candidate_run_id`), surviving platform restarts and preserving canonical reference benchmarks across sessions.

### 4. Dedicated Methodology Guide & RFC-4180 CSV Export
- View the 12-step scientific protocol in the dedicated guide at `/benchmarks/guide`.
- Export RFC-4180 compliant CSV reports comparing Median, Mean, Min, Max, Std Dev, Actual Batch Throughput, Latency-Derived Estimate, and Speedup Factor directly via `/benchmarks/compare?export=csv`.

---

## Architecture & Design Decisions

```
+--------------------------------------------------------------------------+
|                            Browser / Client                              |
+--------------------------------------------------------------------------+
                                     |
                                     v HTTP / HTTPS
+--------------------------------------------------------------------------+
|                  Jakarta Servlet Container (Tomcat 10.1)                 |
|  +--------------------+   +---------------------+   +-----------------+  |
|  |     AuthFilter     |-->|   DashboardServlet  |-->| JSP Views (JSTL)|  |
|  | (CSRF, RBAC, Auth) |   |   BenchmarkServlet  |   | (Auto-escaped)  |  |
|  +--------------------+   |   JobServlet        |   +-----------------+  |
|                           |   WorkloadServlet   |                        |
+---------------------------+---------------------+------------------------+
                                     |
                                     v
+--------------------------------------------------------------------------+
|                             Service Layer                                |
|  +--------------------+   +---------------------+   +-----------------+  |
|  |  BenchmarkService  |   | ExecutionJobService |   | WorkloadService |  |
|  |  (12-Step Protocol)|   | (Idempotency, Lease)|   | (Generator/CSV) |  |
|  +--------------------+   +---------------------+   +-----------------+  |
|                                    |                                     |
|             +----------------------+----------------------+              |
|             v                                             v              |
|  +------------------------+                    +----------------------+  |
|  | ExecutionWorkerManager |                    |  ScheduleValidator   |  |
|  | (Bounded Daemon Pool)  |                    |  (Mathematical Proof)|  |
|  +------------------------+                    +----------------------+  |
+--------------------------------------------------------------------------+
                                     |
                                     v JDBC (HikariCP Connection Pool)
+--------------------------------------------------------------------------+
|                               MySQL 8.0+                                 |
|  Tables: users | workloads | experiment_runs | benchmark_runs            |
|          benchmark_measurements | execution_jobs | system_settings       |
+--------------------------------------------------------------------------+
```

---

## Dashboard & Telemetry Overview

The redesigned production dashboard (`/dashboard`) is divided into 4 clear functional sections:

- **Section A: Platform Overview & Queue Health**: Total workloads, job counts broken down by status (`QUEUED`, `RUNNING`, `COMPLETED`, `FAILED`, `CANCELLED`), completed experiments, and recent audit activity.
- **Section B: Durable Featured Benchmark Comparison**: Pinned canonical comparison pair (e.g. Round Robin baseline vs. candidate) displaying speedup factor, actual batch throughput, latency-derived throughput estimate, and statistical distribution deltas.
- **Section C: Live Execution Activity**: Most recent background jobs with real-time status badges, execution metrics, and direct links to job inspect pages.
- **Section D: Scheduling & Micro-Benchmark Insights**: Cross-workload average wait times, turnaround times, and CPU utilization across all algorithms, alongside recent micro-benchmark history.

---

## Demo Data Pipeline & Quick Credentials

Orchestrivo includes a real demo data seeder (`DemoDataSeeder`) that populates the database with non-mock, fully validated records passing through the genuine application pipeline:

- **Dedicated Demo Account**:
  - **Email**: `demo@orchestrivo.io`
  - **Password**: `Demo@12345`
- **Seeded Data Artifacts**:
  - **5 Realistic Workloads**: Convoy Effect, Interactive Desktop, Uniform Batch, 50-Process Scale, Mixed Burst.
  - **~40 Execution Jobs**: Distributed across `COMPLETED`, `FAILED` (intentional validation failure), `CANCELLED`, and `QUEUED` states.
  - **5 Micro-Benchmarks**: FCFS, SJF, SRTF, and Round Robin benchmarks with warmups and measured runs.
  - **Pinned Canonical Comparison**: Pre-configured baseline vs candidate comparison.
- **One-Click Seeder Console**: Accessible at `/demo` with options to Seed (Idempotent) or Reset & Re-seed Cleanly.

---

## Directory Structure

```
orchestrivo-job-execution-and-orchestration-platform/
├── src/
│   ├── main/
│   │   ├── java/com/orchestrivo/
│   │   │   ├── domain/        # ProcessSpec, Workload, SchedulingResult, ExecutionSegment, AlgorithmType
│   │   │   ├── model/         # BenchmarkRunRecord, ExecutionJobRecord, PagedResult, QueueMetrics
│   │   │   ├── persistence/   # HikariCP JDBC repositories & SystemSettingsRepository
│   │   │   ├── scheduling/    # SchedulingStrategy implementations (Fcfs, Sjf, Srtf, RoundRobin)
│   │   │   ├── service/       # BenchmarkService, ExecutionJobService, DemoDataSeeder, WorkloadService
│   │   │   ├── util/          # CsvExporter (CWE-1236 safe), CsvWorkloadParser, WorkloadGenerator
│   │   │   ├── validation/    # ScheduleValidator (Invariant assertions)
│   │   │   └── web/           # Jakarta Servlets (Dashboard, Benchmark, Job, Workload, Auth, Demo)
│   │   ├── resources/
│   │   │   └── db/            # Database schema migrations (V1 through V7)
│   │   └── webapp/
│   │       ├── WEB-INF/views/ # Server-rendered JSP views (dashboard, benchmarks, jobs, workloads)
│   │       └── assets/        # CSS design system and JavaScript controller
│   └── test/java/com/orchestrivo/
│       ├── persistence/       # JDBC persistence and concurrency tests
│       ├── scheduling/        # Algorithm correctness and tie-breaking tests
│       ├── service/           # Concurrency bursts, worker pool drain, and idempotency tests
│       ├── util/              # CSV RFC compliance and formula injection mitigation tests
│       ├── validation/        # Schedule validator invariant tests
│       └── web/               # CSRF, auth session rotation, and tenant isolation tests
├── pom.xml                    # Maven configuration (Java 17, Tomcat 10.1 WAR)
└── README.md                  # Project engineering documentation
```

---

## Prerequisites & Quickstart

### Prerequisites
- **Java**: OpenJDK 17 or higher
- **Web Container**: Apache Tomcat 10.1+ (Jakarta EE 10 / Servlet 6.0)
- **Database**: MySQL 8.0 or higher
- **Build Tool**: Maven Wrapper (`mvnw` / `mvnw.cmd` included)

### 1. Database Setup
Create the MySQL database and user:
```sql
CREATE DATABASE orchestrivo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'orchestrivo_user'@'localhost' IDENTIFIED BY 'Orchestrivo_Secure_2026!';
GRANT ALL PRIVILEGES ON orchestrivo.* TO 'orchestrivo_user'@'localhost';
FLUSH PRIVILEGES;
```

Run Flyway migrations located in `src/main/resources/db/`:
```bash
mysql -u orchestrivo_user -p orchestrivo < src/main/resources/db/V1__init_schema.sql
mysql -u orchestrivo_user -p orchestrivo < src/main/resources/db/V2__stage3_features.sql
mysql -u orchestrivo_user -p orchestrivo < src/main/resources/db/V3__execution_jobs.sql
mysql -u orchestrivo_user -p orchestrivo < src/main/resources/db/V7__final_readiness_pass.sql
```

Configure environment variables:
- `JDBC_URL`: `jdbc:mysql://localhost:3306/orchestrivo?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`
- `JDBC_USER`: `orchestrivo_user`
- `JDBC_PASSWORD`: `Orchestrivo_Secure_2026!`

### 2. Build the Application
Compile, test, and assemble the WAR:
```cmd
.\mvnw.cmd clean package
```
The deployable archive will be generated at `target/orchestrivo.war`.

### 3. Deploy to Tomcat
1. Copy `target/orchestrivo.war` to Tomcat's `webapps/` directory (`$CATALINA_HOME/webapps/`).
2. Ensure database environment variables or JVM system properties are set in Tomcat's `setenv.bat` or `setenv.sh`.
3. Start Tomcat:
   ```cmd
   catalina.bat start
   ```
4. Access the application in your browser:
   ```
   http://localhost:8080/orchestrivo
   ```
5. Log in with the pre-seeded demo user (`demo@orchestrivo.io` / `Demo@12345`), or visit `/demo` to initialize seed data.

---

## Automated Verification & Test Suite

Orchestrivo features a comprehensive automated test suite with **177 passing tests (0 failures, 0 errors, 0 skipped)**:

```cmd
.\mvnw.cmd test
```

### Test Coverage Highlights:
- **Scheduling Correctness (`com.orchestrivo.scheduling.*`)**: FCFS, SJF, SRTF, and Round Robin mathematical invariants, deterministic tie-breaking, process starvation resistance, and quantum boundary enforcement.
- **Schedule Validator (`com.orchestrivo.validation.*`)**: Verification of zero-overlap CPU execution, arrival time causality, burst fulfillment, and invalid schedule rejection.
- **Concurrent Queue Engineering (`com.orchestrivo.service.ExecutionJobConcurrencyTest`)**:
  - Enqueue bursts (50, 100, 200 jobs) under high concurrency.
  - Multi-worker scalability tests (2, 4, 8 workers draining 30 jobs).
  - Atomic job claiming with 12 concurrent worker threads racing for jobs without duplicate processing.
  - Stale lease recovery and dead-letter classification.
  - Idempotency key deduplication under network replay simulations.
- **Micro-Benchmarking Methodology (`com.orchestrivo.service.*`)**:
  - Verification of actual batch wall-clock throughput calculation excluding warmup cycles.
  - Separation of latency-derived throughput estimates from measured throughput.
  - Durable `system_settings` pinning and unpinning lifecycle.
- **Security & Authorization (`com.orchestrivo.web.*`)**:
  - CSRF token validation on all state-changing endpoints.
  - Strict resource ownership isolation preventing cross-tenant access to jobs, workloads, and benchmarks.
  - Session fixation protection via session identifier rotation upon authentication.
  - CSV formula injection mitigation (`=`, `+`, `-`, `@` sanitized per CWE-1236).

---

## Security & Production Hardening

- **CWE-1236 CSV Injection Sanitization**: All exported CSV files prefix potentially malicious trigger characters (`=`, `+`, `-`, `@`, `\t`, `\r`) with single quotes.
- **CSRF Token Verification**: Every state-changing HTTP request (`POST`, `PUT`, `DELETE`) is guarded by a session-scoped cryptographically random UUID token verified by `AuthFilter`.
- **SQL Injection Prevention**: 100% of database interactions utilize JDBC `PreparedStatement` parameterized queries.
- **Cross-Site Scripting (XSS) Prevention**: All JSP templates utilize `<c:out value="..."/>` to ensure strict HTML entity escaping.
- **Password Security**: Strong salt-derived hashing via BCrypt (`org.mindrot:jbcrypt`).
- **Resource Ownership Isolation**: Multi-tenant authorization checks on all query and mutation operations verify ownership against the authenticated session user.

---

## License

This project is open-source software licensed under the [MIT License](LICENSE).