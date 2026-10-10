# Orchestrivo

Orchestrivo is a Java-based job execution and orchestration platform. In Stage 2, it has evolved from a simple scheduling simulator into a full Java web application (WAR) with persistent storage, web interfaces, authentication, and experiment management.

## Stage 2 Capabilities

Orchestrivo has been transformed into a fully deployable web application running on Tomcat. The scheduling algorithms and domain logic from Stage 1 have been kept strictly separate from the web and persistence layers.

### Key Features
1. **Jakarta EE Web Interface**: Server-side rendered UI using Jakarta Servlets and JSP pages.
2. **Authentication & Authorization**: Secure user registration and login utilizing bcrypt password hashing and session-based authentication via filters.
3. **JDBC Persistence**: Custom JDBC repositories using HikariCP for connection pooling. Supports complete lifecycles for workloads, execution metrics, and audit history.
4. **Workload Management**: Users can create, view, and manage process workloads directly from the web interface.
5. **Experiment Running**: Users can trigger simulations of the four scheduling algorithms (FCFS, SJF, SRTF, Round Robin) against workloads and view validation outcomes and metrics.
6. **Graceful Database Handling**: If the database is missing, integration tests are automatically skipped (`Assumptions.assumeTrue`) to ensure uninterrupted build pipelines.

## Architecture
- `com.orchestrivo.model`: Contains persistence and Data Transfer Objects (`User`, `WorkloadRecord`, `ExperimentRunRecord`).
- `com.orchestrivo.domain`: Contains core domain objects like `ProcessSpec` and `ExecutionSegment` (kept pure, no persistence annotations).
- `com.orchestrivo.scheduling`: Contains `SchedulingStrategy` implementations (FCFS, SJF, SRTF, Round Robin) and metric calculations.
- `com.orchestrivo.persistence`: Contains interface definitions for repositories and their concrete JDBC implementations (`UserRepositoryJdbcImpl`, `WorkloadRepositoryJdbcImpl`, etc.).
- `com.orchestrivo.service`: Application services (`AuthenticationService`, `WorkloadService`, `ExperimentService`) handling business logic and transactions.
- `com.orchestrivo.web`: Servlets, Filters, Listeners, and the `ServiceRegistry` which binds layers together manually.

## Technology Stack and Prerequisites

- **Java**: JDK 17 (Required)
- **Web Server**: Apache Tomcat (Requires support for Jakarta EE 10 / Servlet 6.0)
- **Database**: MySQL 8.0+
- **Build Tool**: Maven (Provided via Maven Wrapper)
- **Testing**: JUnit 5

## Configuration and Deployment

### Database Setup
1. Create a MySQL database (e.g., `orchestrivo`).
2. Apply the schema migration located at `src/main/resources/db/V1__init_schema.sql`.
3. Set the following environment variables (or provide defaults in `DataSourceManager`):
   - `JDBC_URL`: e.g. `jdbc:mysql://localhost:3306/orchestrivo`
   - `JDBC_USER`: e.g. `root`
   - `JDBC_PASSWORD`: e.g. `password`

### Building the Web Application
You can build the project into a deployable WAR file using the included Maven Wrapper.

**On Unix-like Systems (Linux/macOS):**
```bash
./mvnw clean package
```

**On Windows:**
```cmd
.\mvnw.cmd clean package
```

The resulting `orchestrivo.war` file will be generated in the `target/` directory.

### Deployment
1. Drop the `target/orchestrivo.war` file into your Tomcat `webapps/` directory.
2. Ensure Tomcat has access to the database credentials.
3. Access the application at `http://localhost:8080/orchestrivo`.

## Current Limitations

- **Simulation Only**: The project simulates scheduling. It does not execute actual OS processes.
- **Single-Node**: This application is not yet distributed or cluster-aware.
- **Frontend**: The UI uses simple HTML/CSS and vanilla JS. It does not utilize an advanced framework like React or Vue.

## Testing

Run unit and integration tests using:
```bash
./mvnw clean test  # Unix
.\mvnw.cmd clean test # Windows
```
*Note: Database integration tests will automatically skip if a local MySQL instance is not running and configured correctly.*