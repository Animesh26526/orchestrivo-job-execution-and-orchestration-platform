# Orchestrivo

Orchestrivo is a Java-based CPU scheduling simulation, experiment management, and benchmarking platform. In Stage 4, Orchestrivo has evolved with a **complete UI modernization** across every application view, **hardened concurrency & reliability engineering**, a **persistent MySQL-backed background execution engine** with worker pool lifecycle management, and **full queue observability**.

---

## Stage 4 Capabilities

Orchestrivo runs on a verified Java 17 + Jakarta Servlet 6.0 + JSP 3.1 + JDBC + MySQL 8 foundation deployed as a WAR on Apache Tomcat 10.1+.

### 1. Complete UI Modernization (Workstream A)
- **Shared Design System**: Polished visual language (`assets/css/app.css`, `assets/js/app.js`) featuring clean typography, consistent color tokens, metric cards, status badges, and responsive navigation shells.
- **Interactive & Accessible Gantt Timeline**: Rich visual execution timeline with color-coded process blocks, idle CPU representations, time axis ticks, and a screen-reader friendly tabular alternative for full accessibility.
- **Benchmark Visualizations**: Inline SVG charts rendering per-iteration execution latency distributions without external frontend framework bloat.
- **Telemetry Distinction**: Clear visual separation between theoretical CPU scheduling metrics (clock ticks) and actual host engine execution durations (wall-clock milliseconds/nanoseconds).

### 2. Persistent Background Execution Engine (Workstream C)
- **MySQL-Backed Job Queue**: Schema migration `V3__execution_jobs.sql` introduces persistent job records tracking statuses (`QUEUED`, `RUNNING`, `COMPLETED`, `FAILED`, `CANCELLED`).
- **Autonomous Worker Engine**: `ExecutionWorkerManager` orchestrates a bounded daemon thread pool that polls the job queue, executes simulations asynchronously, validates CPU invariants, and records completed runs.
- **Lifecycle Integration**: Managed via `AppConfigListener` for clean startup and graceful shutdown during Tomcat context destruction.
- **Asynchronous & Synchronous Execution**: Experiments run asynchronously by default with automatic background dispatch and polling (`/jobs/status`), with synchronous mode supported via `?mode=sync`.

### 3. Reliability & Concurrency Engineering (Workstream B)
- **Atomic Job Claiming**: Multi-worker concurrency safety utilizing atomic SQL transitions (`UPDATE execution_jobs SET status='RUNNING', claimed_by=?, claimed_at=NOW() WHERE id=? AND status='QUEUED'`).
- **Idempotency Deduplication**: Client-provided idempotency keys prevent duplicate job enqueueing under retries or network replays.
- **Safe Retries & Attempt Caps**: Failed jobs support retries up to `max_attempts` with error message tracking and manual retry actions.
- **Stale Lease Recovery**: Automatic detection and recovery of stalled jobs from crashed or unresponsive workers.
- **Double-Submit Prevention**: Client-side form protection and transaction boundaries preventing accidental duplicate submissions.
- **Resource Ownership Enforcement**: Strict tenant isolation across all job operations (`cancel`, `retry`, `view`, `status`).

### 4. Queue Observability & Dashboard Telemetry (Workstream D)
- **Live Metrics**: Dashboard and Queue management pages display live queue counters (Queued, Running, Completed, Failed), average queue wait times, and engine compute latencies.
- **Audit Integration**: Full operational trail tracking job dispatch, cancellation, retries, and completions.

---

## Architecture & Code Organization

```
src/
├── main/
│   ├── java/com/orchestrivo/
│   │   ├── domain/        # Pure CPU scheduling domain models & algorithms (FCFS, SJF, SRTF, RR)
│   │   ├── model/         # Persistence entities, DTOs, and pagination models
│   │   ├── persistence/   # JDBC repository interfaces & implementations (HikariCP)
│   │   ├── scheduling/    # Scheduling strategies and strategy factory
│   │   ├── service/       # Application business logic, validation, benchmarks, and audit
│   │   ├── util/          # CSV parser, CSV exporter (CWE-1236 safe), workload generator
│   │   ├── validation/    # Independent schedule validator
│   │   └── web/           # Jakarta Servlets, AuthFilter (CSRF/RBAC), ServiceRegistry
│   ├── resources/
│   │   └── db/            # Database schema migrations (V1__init_schema.sql, V2__stage3_features.sql)
│   └── webapp/
│       ├── WEB-INF/views/ # Server-rendered JSP templates (JSTL, c:out escaping)
│       └── assets/        # Static stylesheets and client assets
└── test/                  # Comprehensive unit, persistence, and security integration tests
```

---

## Technology Stack and Prerequisites

- **Java**: JDK 17 (Required)
- **Web Container**: Apache Tomcat 10.1+ (Jakarta EE 10 / Servlet 6.0)
- **Database**: MySQL 8.0+
- **Connection Pool**: HikariCP 5.1.0
- **JSON Processing**: Google Gson 2.10.1
- **Password Hashing**: BCrypt 0.10.2
- **Build Tool**: Maven 3.8+ (via Maven Wrapper `mvnw`)
- **Testing**: JUnit 5 & Mockito

---

## Configuration and Deployment

### 1. Database Setup
1. Create a MySQL database (e.g., `orchestrivo`).
2. Run database migrations:
   ```bash
   mysql -u root -p orchestrivo < src/main/resources/db/V1__init_schema.sql
   mysql -u root -p orchestrivo < src/main/resources/db/V2__stage3_features.sql
   mysql -u root -p orchestrivo < src/main/resources/db/V3__execution_jobs.sql
   ```
3. Grant permissions to dedicated application user `orchestrivo_user`:
   ```sql
   CREATE USER IF NOT EXISTS 'orchestrivo_user'@'localhost' IDENTIFIED BY 'Orchestrivo_Secure_2026!';
   GRANT ALL PRIVILEGES ON orchestrivo.* TO 'orchestrivo_user'@'localhost';
   FLUSH PRIVILEGES;
   ```
4. Set environment variables (or configure in container):
   - `JDBC_URL`: `jdbc:mysql://localhost:3306/orchestrivo?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`
   - `JDBC_USER`: `orchestrivo_user`
   - `JDBC_PASSWORD`: `Orchestrivo_Secure_2026!`

### 2. Building the Web Application
```cmd
.\mvnw.cmd clean package
```
The resulting `target/orchestrivo.war` artifact will be generated.

### 3. Deploying to Apache Tomcat
1. Copy `target/orchestrivo.war` into Tomcat's `webapps/` folder.
2. Ensure Tomcat environment has the `JDBC_*` environment variables or JVM system properties configured.
3. Access the application at `http://localhost:8080/orchestrivo`.

---

## Verification & Testing

Execute the complete automated test suite (110 tests):
```powershell
$env:JDBC_URL = [System.Environment]::GetEnvironmentVariable('JDBC_URL', 'User')
$env:JDBC_USER = [System.Environment]::GetEnvironmentVariable('JDBC_USER', 'User')
$env:JDBC_PASSWORD = [System.Environment]::GetEnvironmentVariable('JDBC_PASSWORD', 'User')
.\mvnw.cmd test
```

### Test Coverage Highlights:
- **Unit Tests**: CSV parser RFC-compliance, formula injection mitigation, deterministic generator reproducibility, benchmark statistics (median, std dev), comparison logic.
- **Persistence Tests**: MySQL JDBC repositories for Users, Workloads, Experiment Runs, Benchmark Runs & Measurements, Audit Logs, and Execution Jobs (atomic claiming with 10 concurrent threads, state transitions, idempotency deduplication, stale lease recovery).
- **Service & Engine Tests**: ExecutionWorkerManager lifecycle, asynchronous execution of FCFS and Round Robin, permanent invalid snapshot failure handling, schedule invariant verification.
- **Security Tests**: Session ID rotation on auth, CSRF rejection on all state-changing endpoints (including job cancellation and retries), resource ownership isolation, JSP XSS output escaping (`<c:out>`).

---

## Current Scope & Explicit Non-Goals

- **CPU Scheduling Simulation Only**: The application models theoretical operating system scheduling algorithms (FCFS, SJF, SRTF, RR). It does not execute arbitrary shell commands or OS processes.
- **Single-Node Architecture**: Orchestrivo is designed as a modular monolith; distributed clustering and worker pools are out of scope.
- **No Spring/JPA**: Hand-crafted JDBC and Jakarta Servlets are intentionally preserved to maintain architectural purity and zero-framework transparency.