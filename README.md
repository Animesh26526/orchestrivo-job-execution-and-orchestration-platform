# Orchestrivo

Orchestrivo is a Java-based job execution and orchestration platform. It is designed to act as a simulator for operating-system CPU scheduling algorithms and will eventually evolve into a full web application for experiment management and benchmarking.

## Stage 1 Capabilities

Currently, Orchestrivo is in **Stage 1** of development. It provides the core domain models, four standard CPU scheduling algorithms, scheduling metrics calculation, and an independent schedule validation mechanism. 

The application is implemented as a layered modular monolith, ensuring separation of concerns between algorithms, metrics, validation, and data representation.

### Supported Scheduling Algorithms

1. **FCFS (First Come, First Served)**: Non-preemptive scheduling where processes are executed in the exact order they arrive.
2. **SJF (Shortest Job First)**: Non-preemptive scheduling where the process with the shortest burst time is selected next.
3. **SRTF (Shortest Remaining Time First)**: A preemptive version of SJF where a running process can be preempted if a newly arrived process has a shorter remaining burst time.
4. **Round Robin**: Preemptive, time-sliced scheduling where processes execute in a cyclic order for a specified time quantum.

## Architecture and Package Responsibilities

The project follows a clean layered architecture to keep concerns distinct:
- `com.orchestrivo.domain`: Contains immutable domain objects like `ProcessSpec`, `ExecutionSegment`, and `Workload`. Enforces domain invariants (e.g., rejecting the reserved `IDLE` process ID).
- `com.orchestrivo.scheduling`: Contains the `SchedulingStrategy` interface, configuration models, and the `MetricsCalculator` for computing wait time, turnaround time, throughput, etc.
- `com.orchestrivo.scheduling.strategies`: Contains the concrete implementations of the scheduling algorithms (FCFS, SJF, SRTF, Round Robin).
- `com.orchestrivo.validation`: Contains the `ScheduleValidator`, which independently verifies execution timelines, aggregate metrics, and result integrity without relying on the scheduling algorithm's logic.

## Technology Stack and Prerequisites

- **Java**: JDK 17 (Required)
- **Build Tool**: Maven (Provided via Maven Wrapper, globally installed Maven is not required)
- **Testing**: JUnit 5

## Repository Structure

```text
orchestrivo-job-execution-and-orchestration-platform/
├── .mvn/wrapper/        # Maven wrapper configuration
├── src/
│   ├── main/java/       # Source code for domain, scheduling, and validation
│   └── test/java/       # Comprehensive test suites (Unit and Integration)
├── .gitignore           # Git ignore rules
├── mvnw                 # Maven wrapper script for Unix
├── mvnw.cmd             # Maven wrapper script for Windows
├── pom.xml              # Maven configuration
└── README.md            # This documentation
```

## Building and Testing

You do not need a globally installed Maven executable. You can build and test the project using the included Maven Wrapper.

### On Unix-like Systems (Linux/macOS)

- **Run all tests:**
  ```bash
  ./mvnw clean test
  ```
- **Package the application:**
  ```bash
  ./mvnw clean package
  ```

### On Windows

- **Run all tests:**
  ```cmd
  .\mvnw.cmd clean test
  ```
- **Package the application:**
  ```cmd
  .\mvnw.cmd clean package
  ```

## Current Limitations

- The project is purely a simulation engine at this stage. It does not control actual OS processes or run arbitrary executable programs.
- The web interface (Servlets/JSP), authentication, authorization, and database persistence (JDBC/MySQL) are not yet implemented.
- Background worker execution or distributed capabilities do not exist.
- Advanced scheduling algorithms involving multiple cores, priority, or strict deadlines are not part of the initial phase.

## Planned Stage 2 Roadmap

In Stage 2, Orchestrivo is planned to evolve into a full web application featuring:
- A server-side rendered UI using Jakarta Servlets and JSP.
- Secure user authentication and session management.
- Persistent JDBC repositories (backed by MySQL) for workloads, execution metrics, and audit history.
- Workload management via UI, including CSV imports and randomized generation.
- Interactive algorithm comparison and visualizations (e.g., Gantt charts).
- Rigorous implementation benchmarking tools.