# Orchestrivo

Orchestrivo is a Java-based CPU scheduling simulation, experiment management, and benchmarking platform. In Stage 3, it has evolved into a comprehensive experimental platform featuring workload lifecycle management, reproducible pseudo-random workload generation, RFC-compliant CSV workflows, multi-algorithm side-by-side comparison, immutable input snapshots, and wall-clock execution micro-benchmarking.

---

## Stage 3 Capabilities

Orchestrivo runs on a verified Java 17 + Jakarta Servlet 6.0 + JSP + JDBC + MySQL 8 foundation deployed as a WAR on Apache Tomcat 10.1+.

### 1. Workload Lifecycle Management
- **Manual Creation & Editing**: Create, inspect, edit metadata, and modify process specifications with transactional consistency.
- **Workload Duplication**: Clone existing workloads to isolate parameter variations without mutating baseline data.
- **Search, Filtering, & Allowlisted Sorting**: Server-side query filtering with bounded pagination (`10, 25, 50` items/page) and parameterized SQL queries preventing SQL injection.

### 2. CSV Data Workflows & Reproducible Generation
- **RFC-Compliant CSV Import**: Supports file upload (`multipart/form-data`) and direct text pasting. Row-level validation enforces positive bursts, non-negative arrivals, unique process IDs, and a 500-process safety ceiling.
- **Deterministic Workload Generation**: Generates synthetic workloads using a seeded pseudo-random generator (`java.util.Random(seed)`), enabling identical reconstruction across test environments.
- **Hardened CSV Export**: Neutralizes spreadsheet formula injection (CWE-1236) by automatically prefixing dangerous trigger characters (`=`, `+`, `-`, `@`, `\t`, `\r`) with a single quote (`'`).

### 3. Experiment Management & Side-by-Side Comparison
- **Immutable Input Snapshots**: Captures an exact JSON snapshot of the input processes at execution time (`experiment_runs.input_snapshot`). Retains historical integrity and mathematical reproducibility even if the source workload is modified or deleted.
- **Controlled Multi-Algorithm Comparison**: Compares up to 10 experiment runs side-by-side. Validates workload compatibility by verifying snapshot equality, presents metric winner highlights, and exports comparison reports to CSV.
- **Independent Schedule Validation**: Verifies that every generated execution timeline adheres strictly to CPU scheduling invariants (no overlaps, total CPU burst match, no pre-arrival execution).

### 4. Implementation Micro-Benchmarking
- **Engine Execution Latency Measurement**: Measures wall-clock algorithm execution using `System.nanoTime()`.
- **JIT Warm-Up Protocol**: Runs untimed initial iterations to prime the HotSpot JIT compiler before recording measured iterations.
- **Statistical Summaries**: Calculates median, minimum, maximum, and sample standard deviation in microseconds (µs) and milliseconds (ms).
- **Metric Domain Separation**: Strictly separates host engine execution latency from simulated domain metrics (e.g., turnaround time, waiting time).
- **Continuous Invariant Verification**: Validates the schedule output on every single benchmark repetition.
- **Host Runtime Metadata**: Automatically captures JVM version, vendor, OS name, architecture, and available processor cores.

### 5. Operational Audit Logging
- **Non-Blocking Audit Trail**: Asynchronously logs key operational events (`AUTH_LOGIN`, `AUTH_LOGOUT`, `AUTH_REGISTER`, `WORKLOAD_CREATE`, `WORKLOAD_UPDATE`, `WORKLOAD_DUPLICATE`, `WORKLOAD_DELETE`, `CSV_IMPORT`, `CSV_EXPORT`, `BENCHMARK_RUN`, `BENCHMARK_DELETE`).
- **Secret Redaction**: Automatically scrubs passwords, session identifiers, and CSRF tokens from audit payloads prior to persistence.

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

Execute the complete automated test suite (89 tests):
```powershell
$env:JDBC_URL = [System.Environment]::GetEnvironmentVariable('JDBC_URL', 'User')
$env:JDBC_USER = [System.Environment]::GetEnvironmentVariable('JDBC_USER', 'User')
$env:JDBC_PASSWORD = [System.Environment]::GetEnvironmentVariable('JDBC_PASSWORD', 'User')
.\mvnw.cmd test
```

### Test Coverage Highlights:
- **Unit Tests**: CSV parser RFC-compliance, formula injection mitigation, deterministic generator reproducibility, benchmark statistics (median, std dev), comparison logic.
- **Persistence Tests**: MySQL JDBC repositories for Users, Workloads, Experiment Runs, Benchmark Runs & Measurements, Audit Logs.
- **Security Tests**: Session ID rotation on auth, CSRF rejection on all state-changing endpoints, resource ownership isolation, JSP XSS output escaping (`<c:out>`).

---

## Current Scope & Explicit Non-Goals

- **CPU Scheduling Simulation Only**: The application models theoretical operating system scheduling algorithms (FCFS, SJF, SRTF, RR). It does not execute arbitrary shell commands or OS processes.
- **Single-Node Architecture**: Orchestrivo is designed as a modular monolith; distributed clustering and worker pools are out of scope.
- **No Spring/JPA**: Hand-crafted JDBC and Jakarta Servlets are intentionally preserved to maintain architectural purity and zero-framework transparency.