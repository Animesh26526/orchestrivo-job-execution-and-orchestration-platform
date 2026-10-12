# Orchestrivo — Product Requirements Document (PRD)

**Document status:** Implementation specification  
**Product name:** Orchestrivo 
**Product type:** Java-based CPU scheduling simulation, experiment management, and benchmarking web application  
**Primary purpose:** Demonstrate a complete, secure, database-backed Java web application using Core Java, OOP, SQL, JDBC, Jakarta Servlets, JSP, Maven, and MySQL.

---

## 1. Product Overview

Orchestrivo is a server-rendered Java web application that lets users create CPU-process workloads, simulate classic operating-system scheduling algorithms, inspect execution timelines, calculate scheduling metrics, save experiments, compare algorithm outcomes, and run reproducible implementation benchmarks.

The project uses an existing CPU Scheduling Algorithm Simulator repository as a **reference for algorithm behavior and visual concepts**. The original repository is a Next.js/React/TypeScript frontend with implementations of FCFS, SJF, SRTF, and Round Robin. Orchestrivo must port and test the scheduling logic in Java and independently implement the application, persistence, security, experiment-management, and benchmark layers.

Orchestrivo is a **simulator**, not an operating-system CPU scheduler. It does not control actual OS process scheduling and does not execute arbitrary user-supplied programs.

### 1.1 Product goals

1. Implement correct, testable Java versions of FCFS, non-preemptive SJF, SRTF, and Round Robin.
2. Provide a complete web application using Core Java, OOP, Servlets, JSP, JDBC, and MySQL.
3. Support user accounts, session management, authorization, workload ownership, and secure data access.
4. Let users create, import, validate, save, run, revisit, and compare workloads and experiments.
5. Calculate scheduling metrics accurately and render a readable Gantt chart.
6. Independently validate generated schedules instead of trusting the scheduling implementation.
7. Provide reproducible experiments and benchmark reports without inventing or hardcoding performance claims.
8. Include tests, Maven build configuration, environment-based configuration, setup documentation, and deployment instructions.

### 1.2 Non-goals

The initial version will not:

- Replace or control the operating system's actual CPU scheduler.
- Execute arbitrary scripts, binaries, shell commands, or uploaded programs.
- Implement distributed job execution or a multi-node worker cluster.
- Add AI-based scheduling.
- Claim that simulated time is real execution latency.
- Claim production readiness, scalability, or security without appropriate verification.
- Require a JavaScript frontend framework. A small amount of client-side JavaScript may be used for interface interactions and chart rendering, but the application must remain a Java Servlet/JSP application.
- Add priority scheduling, multi-core scheduling, or deadline-aware scheduling to the initial required algorithm set. These may be future extensions after the baseline is correct.

---

## 2. Target Users and Use Cases

### 2.1 Primary users

- **Student / individual user:** Creates workloads, learns algorithm behavior, runs comparisons, and reviews results.
- **Administrator:** Manages user accounts and roles when necessary, and can inspect system-level operational records.
- **Project reviewer:** Evaluates the application's Java architecture, SQL/JDBC implementation, security, tests, and benchmark methodology.

### 2.2 Core user stories

1. As a user, I can register and sign in securely.
2. As a user, I can create a workload by manually entering processes.
3. As a user, I can import a workload from CSV and see clear validation errors.
4. As a user, I can generate a workload using configurable ranges and a random seed.
5. As a user, I can select one scheduling algorithm and its parameters.
6. As a user, I can run the same workload through multiple algorithms.
7. As a user, I can view a Gantt chart and per-process metrics.
8. As a user, I can compare aggregate metrics for different algorithms on the same workload.
9. As a user, I can save workloads and experiment runs and revisit them later.
10. As a user, I can search, filter, sort, and paginate my workload and experiment history.
11. As a user, I can export experiment results to CSV.
12. As a user, I can run implementation benchmarks on controlled workloads and inspect their measurement methodology.
13. As a user, I cannot access another user's private workloads or results by changing a URL or identifier.
14. As an administrator, I can access authorized administrative functionality without gaining unrestricted access through client-side controls alone.

---

## 3. Functional Scope

### 3.1 Authentication and sessions

The application must support:

- User registration.
- Login and logout.
- Secure password hashing using a suitable password-hashing algorithm such as BCrypt or Argon2 via a maintained Java library.
- Server-side session management.
- Session ID renewal after successful authentication.
- Session invalidation on logout.
- Role-based access control enforced on the server.
- Clear validation and authentication error messages that do not reveal whether a password or account-specific secret is correct.
- Safe handling of expired sessions.

Do not store plaintext passwords or reversible password representations. Do not hardcode credentials or secrets in source control.

### 3.2 Roles and permissions

Minimum roles:

- **USER:** Manage their own workloads, simulations, experiment runs, reports, and account session.
- **ADMIN:** Access explicitly defined administrative features such as user/role management and operational audit review.

Every protected request must verify authentication, role, and resource ownership server-side. Hiding a button in JSP is not authorization.

Users must not be able to read, update, delete, export, or run another user's private resources by changing a path parameter, query parameter, or form field.

### 3.3 Workload management

A workload is a named collection of simulated CPU processes and workload-generation metadata.

Users must be able to:

- Create a workload manually.
- Edit a workload before running it.
- View workload details.
- Duplicate a workload to create a new experiment input.
- Delete a workload they own, subject to a clearly defined retention policy for associated experiment history.
- Import a CSV workload.
- Generate a workload using configured ranges and a reproducible random seed.
- View workload creation and update timestamps.

Each process must include:

- `processId`: unique within a workload.
- `arrivalTime`: non-negative integer simulation time.
- `burstTime`: positive integer CPU time required.
- `priority`: optional field reserved for future algorithms; not used by the initial four algorithms.
- `deadline`: optional future extension; not required for baseline scheduling.

The initial implementation must not require priority or deadline values for the four baseline algorithms.

#### 3.3.1 Input validation

Reject invalid inputs including:

- Missing or duplicate process IDs within a workload.
- Negative arrival times.
- Zero or negative burst times.
- Values outside supported integer/range limits.
- Empty workloads.
- Malformed CSV files.
- Unsupported or ambiguous CSV headers.
- Excessive workload sizes that could make the application unresponsive.

Validation must happen on the server, even if the browser also validates input.

#### 3.3.2 CSV format

Support a documented minimum format:

```csv
process_id,arrival_time,burst_time
P1,0,5
P2,1,3
P3,2,1
P4,4,2
```

The parser must handle whitespace consistently, reject duplicate process IDs, and report row-level errors with row numbers. CSV import must not treat cell contents as executable code. Exported CSV should be safely formatted to reduce spreadsheet formula-injection risk.

#### 3.3.3 Seeded workload generation

Allow the user to specify:

- Number of processes.
- Arrival-time range.
- Burst-time range.
- Random seed.

Store the seed and generation configuration so that the workload can be reproduced. Store the generated process rows as the authoritative workload used for the experiment; do not rely solely on re-generating data later.

### 3.4 Scheduling algorithms

The initial release must implement the following algorithms in Java behind a common interface.

#### FCFS — First Come, First Served

- Non-preemptive.
- Processes execute in arrival order.
- Define deterministic tie-breaking, such as original input order or process ID.
- Handle CPU idle periods when no process has arrived.

#### SJF — Shortest Job First

- Non-preemptive.
- At each decision point, select the arrived and unfinished process with the shortest burst time.
- Define deterministic tie-breaking.
- If no process is available, advance simulation time to the next arrival.

#### SRTF — Shortest Remaining Time First

- Preemptive.
- At each scheduling decision, select the available process with the shortest remaining burst time.
- Correctly handle arrivals that interrupt a running process.
- Define deterministic tie-breaking and document the preemption policy.
- Merge adjacent timeline segments for the same process when no meaningful scheduling boundary needs to be retained; document how context switches are counted.

#### Round Robin

- Preemptive, time-sliced scheduling.
- Requires a positive time quantum.
- Processes enter and leave the ready queue according to a documented, deterministic policy.
- Correctly handle arrivals during a time slice, completion before the quantum ends, and idle periods.
- Reject missing, zero, or negative quantum values.

#### Shared algorithm contract

Each algorithm must accept an immutable or defensively copied workload and validated parameters, then return a schedule result containing:

- Execution timeline segments.
- Per-process first-start and completion times.
- Derived per-process metrics.
- Aggregate scheduling metrics.
- Algorithm name and parameters used.

Algorithms must not access the database, Servlet request objects, JSP views, or HTTP session state. Keep the scheduling core independent of the web and persistence layers.

### 3.5 Schedule representation and Gantt chart

Each execution segment should include:

- Process ID, or an explicit `IDLE` marker.
- Start time.
- End time.
- Segment duration.

Required behaviors:

- Render the schedule in chronological order.
- Show time boundaries and process labels.
- Distinguish idle intervals visually.
- Provide a readable table alternative for accessibility and precise inspection.
- Correctly handle long timelines without making the page unusable.
- Escape displayed user-controlled values.

The chart is a visualization of **simulated time**, not actual wall-clock runtime.

### 3.6 Scheduling metrics

For each process \(i\), calculate:

- Arrival time \(AT_i\).
- Burst time \(BT_i\).
- First start time \(ST_i\).
- Completion time \(CT_i\).
- Turnaround time: \(TAT_i = CT_i - AT_i\).
- Waiting time: \(WT_i = TAT_i - BT_i\).
- Response time: \(RT_i = ST_i - AT_i\).

Aggregate metrics:

- Average waiting time.
- Average turnaround time.
- Average response time.
- Makespan / total simulated schedule duration, with the exact definition documented.
- Simulated throughput, defined consistently as completed processes divided by the chosen simulated observation interval.
- Number of context switches under a documented counting convention.
- Total CPU idle time and idle percentage.
- Maximum ready-queue length, if tracked consistently.

Optional future metrics:

- Deadline miss rate, only when deadlines are part of the workload.
- Fairness measures, with the chosen formula and interpretation documented.

Metrics must be calculated from the schedule and validated independently. Do not hardcode displayed metrics or silently change definitions between algorithms.

### 3.7 Independent schedule validator

Implement a validator separate from the scheduling algorithm implementations and their metric-calculation helpers.

The validator must check at least:

1. Every execution segment has a valid start and end time.
2. Segment durations are positive.
3. Segments are chronologically ordered and do not overlap.
4. No process executes before its arrival time.
5. Each process receives exactly its required total burst time.
6. A process never executes after its completion.
7. No process receives CPU time before it is available.
8. Every process in the workload completes exactly once in a successful simulation.
9. CPU idle periods and gaps are represented consistently.
10. Metric identities are satisfied, including `turnaround = completion - arrival`, `waiting = turnaround - burst`, and `response = first start - arrival`.
11. Algorithm-specific constraints are respected, including non-preemption for FCFS/SJF and quantum constraints for Round Robin.

A failed validation must prevent the run from being marked successful. Show a safe, useful error and retain appropriate diagnostic information without exposing secrets.

### 3.8 Experiment management

An experiment run records one execution of one algorithm on a particular workload and its parameters.

Store at least:

- Workload reference and immutable input snapshot or equivalent reproducible representation.
- Algorithm name.
- Algorithm parameters, including time quantum where applicable.
- Run status: `PENDING`, `RUNNING`, `SUCCEEDED`, or `FAILED`, where appropriate to the implementation.
- Creation/start/completion timestamps.
- Application/version identifier or commit/version label when available.
- Random seed and generation configuration where applicable.
- Aggregate metrics.
- Validation result.
- Error category and safe diagnostic information for failed runs.

The implementation may execute simulations synchronously for the initial version. Do not introduce background worker infrastructure solely to use these status values. If execution is synchronous, represent statuses in a way that accurately reflects the actual lifecycle.

Users must be able to:

- View run details.
- Review per-process results.
- Compare multiple runs on the same workload.
- Filter history by date, algorithm, status, and workload.
- Re-run a workload with another algorithm.
- Delete runs according to a documented retention policy.
- Export results.

A comparison must use the same workload snapshot for all algorithms being compared. Clearly flag comparisons where input workloads or configurations differ.

### 3.9 Algorithm comparison

Provide a comparison view that includes:

- Algorithm and parameters.
- Average waiting time.
- Average turnaround time.
- Average response time.
- Simulated throughput.
- Context switches.
- CPU idle time.
- Fairness or deadline metrics only when available and correctly defined.
- Schedule validity status.

The UI may highlight the minimum or maximum value for a metric, but it must not declare a universal “best algorithm.” Results depend on the workload and optimization objective.

Provide a concise explanation of trade-offs where practical. For example, Round Robin may improve responsiveness in some workloads but increase context switches; SJF may reduce average waiting time under suitable assumptions but can disadvantage long processes.

### 3.10 Implementation benchmarking

Scheduling outcome metrics and implementation performance metrics must remain distinct.

#### Scheduling outcomes

Calculated from simulated timestamps:

- Waiting time.
- Turnaround time.
- Response time.
- Simulated throughput.
- Context switches.
- CPU idle time.

#### Implementation performance

Measured by executing the Java implementation:

- Algorithm execution duration in wall-clock time.
- Scheduling overhead per process, where meaningful.
- Memory consumption, if measured using a documented method.
- Variability across repeated runs.
- Correctness/validation pass rate for benchmark cases.

Benchmark methodology must:

1. Use repeatable workloads and record random seeds.
2. Run warm-up iterations where appropriate for JVM behavior.
3. Repeat measured runs.
4. Report median and variation, not just the single fastest run.
5. Use the same machine, Java version, JVM settings, and workload when comparing implementations.
6. Separate simulation timestamps from wall-clock measurement.
7. Verify correctness before including a measurement.
8. Record workload size and algorithm parameters.
9. Avoid unsupported claims about speedup, scalability, or superiority.
10. State limitations and the measurement environment in the report.

Do not compare different algorithms' wall-clock times as if they alone measure scheduling quality. Algorithm execution speed and the quality of the simulated schedule answer different questions.

### 3.11 CSV export and reports

Users must be able to export:

- Per-process results.
- Aggregate run metrics.
- Workload inputs.
- Algorithm name and parameters.
- Relevant run metadata.

Exports must include a documented header row, use consistent units, and safely handle values that could be interpreted as spreadsheet formulas. Export operations must enforce authentication and resource ownership.

PDF export is not required for the initial release.

### 3.12 Search, filtering, and pagination

Provide server-side search/filtering for saved workloads and experiment history. Support pagination or another documented result limit to prevent large result sets from being loaded into memory all at once.

Use prepared statements and safe parameter handling for all database queries. Never concatenate untrusted input into SQL.

### 3.13 Audit history

Record security-relevant and meaningful user actions, such as:

- Successful login and logout where appropriate.
- Workload creation, update, and deletion.
- Experiment execution and result export.
- Administrative role changes.

Audit records should include actor ID when known, event type, resource reference, timestamp, and minimal non-sensitive metadata. Never log plaintext passwords, session tokens, CSRF tokens, or database credentials. Treat audit history as append-only at the application level; do not claim it is tamper-proof unless additional controls establish that property.

---

## 4. Non-Functional Requirements

### 4.1 Correctness

- The four required algorithms must pass deterministic unit tests and edge-case tests.
- The independent validator must reject invalid schedules.
- All metrics must be derived using documented formulas.
- Equivalent inputs and parameters must produce deterministic schedules under the documented tie-breaking policy.
- Results must not be hardcoded or fabricated.

### 4.2 Security

- Passwords must be hashed with an appropriate password-hashing function.
- Authentication and authorization must be enforced server-side.
- Every resource access must enforce ownership or authorized administrative access.
- Use prepared statements for SQL operations.
- Use CSRF protection for state-changing browser requests.
- Escape output in JSP and avoid rendering untrusted content as raw HTML.
- Validate input on the server.
- Renew session identifiers after authentication and invalidate sessions on logout.
- Configure cookie security attributes appropriately for the deployment environment.
- Keep secrets outside source control.
- Use generic error pages for unexpected exceptions and log safe diagnostic details.
- Do not expose stack traces, SQL statements, credentials, or internal paths to users.
- Protect CSV import/export and uploads with file-size, row-count, and input limits.

### 4.3 Reliability and data integrity

- Use foreign keys and appropriate database constraints.
- Use transactions for logically atomic multi-table writes.
- Roll back failed writes.
- Use try-with-resources for JDBC resources.
- Avoid storing partially completed experiment results as successful runs.
- Preserve a reproducible representation of the input used by a saved experiment.

### 4.4 Maintainability

- Keep a clear separation between models, algorithm implementations, services, repositories, Servlets, filters, JSP views, and utilities.
- Avoid business logic in JSP.
- Avoid database logic in Servlets.
- Use meaningful names and focused classes.
- Keep the algorithm engine independently testable.
- Document non-obvious algorithm behavior and tie-breaking rules.

### 4.5 Usability and accessibility

- Provide clear form labels and validation messages.
- Make tables usable on common laptop and mobile screen widths.
- Provide a tabular alternative to the Gantt chart.
- Use semantic HTML and keyboard-accessible controls.
- Do not rely on color alone to convey status.
- Show loading, empty, success, and error states clearly.

### 4.6 Performance and limits

- Define a configurable maximum workload size and CSV upload size.
- Avoid unbounded query results and unbounded input processing.
- Benchmark the algorithm implementation using a controlled procedure.
- Do not promise a numeric latency or throughput target until a baseline has been measured and documented.

---

## 5. Proposed Architecture

Use a layered Java web architecture:

```text
Browser
  |
  v
JSP Views + small client-side interactions
  |
  v
Jakarta Servlets / Servlet Filters
  |-- Authentication/session handling
  |-- CSRF validation
  |-- Request validation
  |-- Authorization and resource ownership
  |
  v
Service Layer
  |-- WorkloadService
  |-- SchedulingService
  |-- ExperimentService
  |-- BenchmarkService
  |-- AuthenticationService
  |-- ReportService
  |
  +-----------------------------+
  |                             |
  v                             v
Scheduling Core             Repository / DAO Layer
  |                             |
  |-- FCFS                       |-- UserRepository
  |-- SJF                        |-- WorkloadRepository
  |-- SRTF                       |-- ProcessRepository
  |-- RoundRobin                 |-- ExperimentRepository
  |-- ScheduleValidator          |-- AuditRepository
  |-- MetricsCalculator          |
  |                             v
  |                         JDBC + MySQL
  |
  v
Schedule result + validation
```

### 5.1 Architecture rules

- Scheduling algorithms must not depend on Servlet APIs or JDBC.
- Services own application-level business rules and coordinate repositories.
- Repositories own SQL and JDBC resource handling.
- Servlets handle HTTP input/output and delegate to services.
- JSP renders prepared view data and does not execute SQL.
- Authorization and resource ownership checks occur on every relevant request.
- Keep configuration environment-driven.
- Use a connection pool only if compatible with the selected runtime and implemented correctly; never open a new unmanaged connection for every database operation without safe lifecycle management.

---

## 6. Suggested Project Structure

The exact package names can be adjusted, but preserve the layer separation.

```text
Orchestrivo/
├── pom.xml
├── README.md
├── prd.md
├── .gitignore
├── .env.example
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/Orchestrivo/
│   │   │       ├── model/
│   │   │       ├── dto/
│   │   │       ├── algorithms/
│   │   │       │   ├── SchedulingAlgorithm.java
│   │   │       │   ├── FcfsScheduler.java
│   │   │       │   ├── SjfScheduler.java
│   │   │       │   ├── SrtfScheduler.java
│   │   │       │   └── RoundRobinScheduler.java
│   │   │       ├── validation/
│   │   │       │   └── ScheduleValidator.java
│   │   │       ├── metrics/
│   │   │       │   └── MetricsCalculator.java
│   │   │       ├── service/
│   │   │       ├── repository/
│   │   │       ├── servlet/
│   │   │       ├── filter/
│   │   │       ├── security/
│   │   │       ├── config/
│   │   │       └── util/
│   │   ├── resources/
│   │   │   ├── db/
│   │   │   │   ├── schema.sql
│   │   │   │   └── seed.sql
│   │   │   └── application.properties.example
│   │   └── webapp/
│   │       ├── assets/
│   │       │   ├── css/
│   │       │   ├── js/
│   │       │   └── images/
│   │       ├── WEB-INF/
│   │       │   ├── web.xml
│   │       │   └── views/
│   │       │       ├── auth/
│   │       │       ├── dashboard/
│   │       │       ├── workloads/
│   │       │       ├── experiments/
│   │       │       ├── reports/
│   │       │       └── errors/
│   │       └── index.jsp
│   └── test/
│       └── java/com/Orchestrivo/
│           ├── algorithms/
│           ├── validation/
│           ├── metrics/
│           ├── service/
│           └── repository/
├── scripts/
│   └── benchmark/
└── docs/
    ├── architecture.md
    ├── benchmark-methodology.md
    └── database-schema.md
```

This is a suggested structure, not a requirement to create every file immediately. Keep the implementation as simple as practical.

---

## 7. Data Model

### 7.1 `users`

Suggested fields:

- `id` — primary key.
- `email` — unique, normalized.
- `password_hash`.
- `role` — constrained to supported roles.
- `created_at`.
- `updated_at`.
- `is_active`.

### 7.2 `workloads`

Suggested fields:

- `id` — primary key.
- `owner_user_id` — foreign key to users.
- `name`.
- `description`, optional.
- `source_type` — manual, CSV, or generated.
- `generation_seed`, nullable.
- `generation_config`, nullable; use a documented structured representation.
- `created_at`.
- `updated_at`.

### 7.3 `processes`

Suggested fields:

- `id` — database primary key.
- `workload_id` — foreign key.
- `process_key` — process identifier unique within a workload.
- `arrival_time`.
- `burst_time`.
- Optional future fields for priority and deadline.

### 7.4 `experiment_runs`

Suggested fields:

- `id` — primary key.
- `owner_user_id`.
- `workload_id`.
- Algorithm name.
- Algorithm parameters, including Round Robin quantum where relevant.
- Run status.
- Creation, start, and completion timestamps.
- Input snapshot or immutable workload version reference.
- Application/version label where available.
- Validation status.
- Aggregate metrics or a normalized reference to them.
- Safe failure category/message if failed.

### 7.5 `process_results`

Suggested fields:

- `id` — primary key.
- `experiment_run_id` — foreign key.
- `process_key`.
- First start time.
- Completion time.
- Waiting time.
- Turnaround time.
- Response time.

### 7.6 `execution_segments`

Suggested fields:

- `id` — primary key.
- `experiment_run_id` — foreign key.
- Segment sequence.
- Process key or explicit idle marker.
- Start time.
- End time.

### 7.7 `audit_logs`

Suggested fields:

- `id` — primary key.
- Actor user ID, nullable for certain anonymous events.
- Event type.
- Resource type and resource ID.
- Timestamp.
- Minimal structured metadata without secrets.

### 7.8 Database design requirements

- Use appropriate types and constraints.
- Index columns used for ownership filters, foreign-key joins, and common history searches.
- Add unique constraints for normalized email and process IDs within a workload.
- Use foreign keys and documented deletion behavior.
- Avoid storing redundant metrics unless the design has a clear reason and consistency is maintained.
- Use transactions for multi-table experiment persistence.
- Never build SQL queries by concatenating untrusted user input.

---

## 8. URL and Servlet Design

The exact URLs can be changed, but the implementation should provide equivalent functionality.

| Method | Path | Purpose |
|---|---|---|
| GET | `/` | Landing page or redirect |
| GET | `/register` | Registration form |
| POST | `/register` | Create account |
| GET | `/login` | Login form |
| POST | `/login` | Authenticate user |
| POST | `/logout` | Invalidate session |
| GET | `/dashboard` | User dashboard |
| GET | `/workloads` | List owned workloads |
| GET | `/workloads/new` | New workload form |
| POST | `/workloads` | Create workload |
| GET | `/workloads/{id}` | View owned workload |
| POST | `/workloads/{id}/update` | Update workload |
| POST | `/workloads/{id}/delete` | Delete workload |
| POST | `/workloads/import` | Import CSV workload |
| POST | `/workloads/generate` | Generate seeded workload |
| POST | `/workloads/{id}/simulate` | Run selected algorithm |
| GET | `/experiments` | List owned experiment runs |
| GET | `/experiments/{id}` | View run and metrics |
| POST | `/experiments/compare` | Compare authorized runs |
| GET | `/experiments/{id}/export.csv` | Export authorized run |
| GET | `/benchmarks` | Benchmark page |
| POST | `/benchmarks/run` | Start a controlled benchmark |
| GET | `/benchmarks/{id}` | View benchmark results |
| GET/POST | `/admin/users` | Admin-only user management |

Path-variable routing may be implemented through a simple front controller, individual Servlets, or another clear Servlet-based design. All state-changing operations must use POST (or another appropriate non-GET method) and enforce CSRF protection.

Do not expose an endpoint simply because it appears in this table; implement only when the corresponding functionality exists and is protected.

---

## 9. UI / Page Requirements

### 9.1 Public pages

- Landing page describing Orchestrivo.
- Registration page.
- Login page.
- Access-denied and generic error pages.

### 9.2 Authenticated pages

- Dashboard with workload count, recent experiments, and shortcuts.
- Workload list with search, filters, pagination, and create/import actions.
- Workload create/edit form.
- Workload details page with process table.
- Simulation configuration page.
- Simulation results page with Gantt chart, process results, aggregate metrics, validation status, and algorithm parameters.
- Experiment history page.
- Experiment detail page.
- Algorithm comparison page.
- Benchmark setup and results pages.
- CSV export action.
- Profile/session page if required by the final implementation.

### 9.3 Admin pages

- User list and status.
- Role management with server-side authorization.
- Audit event search and details.

### 9.4 UX requirements

- Clear loading, empty, success, and error states.
- Inline field validation with server-side revalidation.
- Confirmation before destructive actions.
- Pagination for potentially large tables.
- Accessible labels and keyboard navigation.
- Responsive, professional, light-first visual style.
- Gantt chart must have a table alternative.
- Do not use fake sample metrics on real user result pages. If demo data is used, label it clearly as demo data.

---

## 10. Testing Strategy

### 10.1 Unit tests for algorithms

For each algorithm, test:

- A single process.
- Multiple processes with the same arrival time.
- Different arrival times.
- A CPU idle period before the first process arrives.
- Gaps between process arrivals.
- Equal burst times.
- A process arriving while another is running.
- Preemption behavior for SRTF.
- Round Robin quantum greater than, equal to, and less than a process's remaining burst.
- Invalid parameters.
- Deterministic tie-breaking.
- Large but supported process sets.

Use hand-calculated reference cases and retain expected timelines and metrics in tests.

### 10.2 Independent validator tests

Test both valid and deliberately invalid schedules, including:

- Overlapping segments.
- Negative or reversed time intervals.
- Missing process execution.
- Extra execution beyond burst time.
- Execution before arrival.
- Invalid idle intervals.
- Incorrect derived metrics.
- Duplicate completion or malformed timeline.

### 10.3 Service tests

Test:

- Workload validation.
- Workload ownership.
- Experiment creation.
- Algorithm selection and parameter validation.
- Failure propagation.
- Result persistence behavior.
- Comparison rejection for incompatible or unauthorized runs.

### 10.4 Repository / JDBC tests

Use a real test database or a documented isolated integration-test setup. Test:

- CRUD.
- Prepared statement parameter handling.
- Joins and filters.
- Foreign-key constraints.
- Transaction commit.
- Rollback after a forced failure.
- Resource closure.
- Persistence across application restarts.

### 10.5 Security tests

Test:

- Unauthenticated access to protected pages.
- Cross-user workload and experiment access.
- Unauthorized admin operations.
- CSRF rejection.
- Invalid input and oversized uploads.
- Escaping of stored user-controlled values.
- Session invalidation on logout.
- No secrets in responses or logs.

### 10.6 Acceptance criteria

The project is not complete merely because it compiles. Completion requires:

- Maven build succeeds.
- Tests pass.
- All four algorithms pass deterministic reference cases.
- Independent validation works.
- A user can register, sign in, create a workload, run a simulation, view results, save history, and revisit results.
- SQL/JDBC persistence works after restart.
- Cross-user access is denied.
- CSV import and export handle valid and invalid input safely.
- Benchmark results are repeatable enough to report their methodology and variability.
- README setup instructions have been followed successfully from a clean checkout.

---

## 11. Benchmark Methodology

The benchmark subsystem must distinguish **algorithm behavior** from **Java implementation performance**.

### 11.1 Scheduling behavior experiments

Run multiple scheduling algorithms on the same immutable workload. Compare:

- Average waiting time.
- Average turnaround time.
- Average response time.
- Context switches.
- CPU idle time.
- Simulated throughput.
- Optional fairness or deadline metrics only when properly supported.

Do not state that one algorithm is universally best. Describe the workloads and objectives under which a policy performed better.

### 11.2 Implementation performance experiments

Measure wall-clock execution time for the Java scheduling implementation with controlled workload sizes.

Requirements:

- Record Java/JVM version and environment details.
- Use deterministic workload generation.
- Warm up the JVM before timed measurements.
- Repeat each benchmark and report median plus variability.
- Keep workload and environment identical for comparisons.
- Separate data-generation time, database I/O, chart rendering, and algorithm execution when reporting algorithm timing.
- Include correctness validation and do not report invalid runs.
- Record process count, input parameters, run count, and measurement method.
- Avoid unsupported speedup claims.
- If measuring memory, document the measurement approach and limitations.

For more rigorous performance analysis, a dedicated benchmarking tool such as JMH may be considered later. It is not mandatory for the first release; if added, it must be used correctly and documented.

---

## 12. Error Handling and Observability

- Validate requests before invoking business logic.
- Use a consistent error response/page structure.
- Distinguish validation errors, authorization failures, missing resources, conflicts, and unexpected server errors.
- Avoid revealing whether a private resource exists to unauthorized users where practical.
- Log exception details server-side with request/correlation IDs where useful.
- Never show stack traces or database details to users.
- Never log passwords, session identifiers, CSRF tokens, or secrets.
- Keep logs useful but minimal; do not build a separate observability platform for this project.

---

## 13. Configuration and Deployment

### 13.1 Configuration

Use environment variables or an external configuration file for:

- Database URL.
- Database username.
- Database password.
- Application environment.
- Session/cookie security options.
- Upload limits.
- Benchmark workload limits.

Provide an `.env.example` or equivalent example containing placeholders only. Never commit real secrets.

### 13.2 Local development

Document:

1. Required Java and Maven versions.
2. Required MySQL version.
3. Database creation and schema initialization.
4. Required environment variables.
5. Maven build and test commands.
6. WAR deployment to a compatible Tomcat version.
7. Application startup and smoke-test steps.
8. Common configuration errors and troubleshooting.

Ensure the selected Tomcat version matches the Jakarta Servlet/JSP API version used by the project.

### 13.3 Deployment

Provide deployment instructions for a compatible Servlet container. If deployed to a public service, configure HTTPS, secure cookies, environment secrets, database access restrictions, and appropriate upload limits. Do not promise a particular hosting provider or a successful public deployment until verified.

---

## 14. License and Third-Party Attribution

The uploaded `SchedulingAlgorithmSimulator-master.zip` contains a third-party TypeScript/React implementation and a license file. Before reusing or translating any source code:

1. Inspect and retain the original license terms and required copyright notice.
2. Attribute the original repository and identify which portions were adapted, if any.
3. Do not present copied or translated source code as wholly original.
4. Distinguish independently written Java application code from adapted algorithm logic.
5. Verify that any additional libraries used are compatible with the project's license and intended distribution.

The original repository should be treated as a reference for algorithm behavior and UI concepts. Any port must be tested independently; the existence of the original code does not prove the translated Java implementation is correct.

---

## 15. Implementation Phases

### Phase 1 — Scheduling core

- Establish Maven project.
- Define process, workload, timeline, and result models.
- Implement the common scheduling interface.
- Port FCFS, SJF, SRTF, and Round Robin.
- Define tie-breaking and edge-case behavior.
- Implement metrics and the independent schedule validator.
- Write deterministic unit tests.

**Exit criteria:** The four algorithms pass reference and edge-case tests without requiring a web server or database.

### Phase 2 — SQL and JDBC

- Design and create the relational schema.
- Implement repositories/DAOs.
- Implement CRUD and ownership-aware queries.
- Add transaction boundaries.
- Test rollback, constraints, and resource management.

**Exit criteria:** Workloads and experiment results persist correctly in MySQL.

### Phase 3 — Servlet/JSP application

- Implement registration, login, logout, and session handling.
- Build workload management forms and pages.
- Connect Servlets to services and repositories.
- Render Gantt charts, metrics, and process tables.
- Add validation and error pages.

**Exit criteria:** A user can complete the core workload-to-results journey in the browser.

### Phase 4 — Security and experiment management

- Add authorization and ownership checks.
- Add CSRF protection and output escaping.
- Add workload CSV import and seeded generation.
- Add experiment history, comparison, search, and export.
- Add audit events.

**Exit criteria:** Core workflows are protected and persisted; cross-user access tests pass.

### Phase 5 — Benchmarking and delivery

- Add reproducible implementation benchmarks.
- Record environment and workload metadata.
- Add benchmark reports and methodology documentation.
- Complete integration/security tests.
- Finalize README, schema scripts, architecture notes, and deployment instructions.

**Exit criteria:** A clean checkout can be built, configured, tested, and run using documented instructions.

---

## 16. Definition of Done

Orchestrivo is ready to be presented as a completed project only when:

- [ ] All four scheduling algorithms are implemented in Java and independently tested.
- [ ] Metrics and Gantt timelines are correct for reference and edge-case workloads.
- [ ] An independent validator checks schedules.
- [ ] Java OOP and layered architecture are used meaningfully.
- [ ] MySQL schema, constraints, joins, indexes, and transactions are implemented.
- [ ] JDBC uses prepared statements and safe resource management.
- [ ] Servlets handle requests, validation, sessions, and authorization.
- [ ] JSP pages render forms, history, results, comparisons, and reports.
- [ ] Authentication, role checks, CSRF protection, and output escaping are tested.
- [ ] Workloads and experiment results persist across application restarts.
- [ ] Search, filters, CSV import, and CSV export are tested.
- [ ] Benchmark results are reproducible enough to document, with no fabricated metrics.
- [ ] Maven build and tests pass.
- [ ] README, architecture, database, benchmark, and deployment documentation are complete.
- [ ] Third-party license and attribution obligations are met.
- [ ] The developer can explain the scheduling logic, Servlet request flow, JSP rendering, JDBC queries, transaction behavior, security controls, and benchmark methodology.

---

## 17. Resume Positioning Guidelines

Potential project title:

**Orchestrivo — Java CPU Scheduling Simulation & Benchmarking Platform**

A resume entry may eventually emphasize:

- A Java web application implementing four CPU scheduling algorithms with a tested scheduling core.
- A Servlet/JSP, JDBC, and MySQL architecture with authenticated workload and experiment management.
- Independent schedule validation, reproducible comparisons, and measured benchmark results.

Only include features and numerical results that have actually been implemented, tested, and measured. Do not claim distributed execution, production-scale performance, or a specific speedup unless the implementation and evidence support those claims.

---

## 18. Final Product Principle

Orchestrivo should be a complete Java enterprise-style web application whose core is a CPU scheduling simulator. The scheduling algorithms provide the domain logic; the application demonstrates Core Java, OOP, SQL, JDBC, Servlets, JSP, security, testing, and disciplined measurement.

Prioritize correctness, understandable architecture, and verifiable features over unnecessary complexity. The initial four algorithms and complete required Java stack take precedence over advanced algorithms, distributed systems, or additional dashboard features.
