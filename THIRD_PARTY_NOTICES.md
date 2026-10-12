# Third-Party Notices and Attributions

This project incorporates and builds upon ideas and open-source software packages. Below are the notices and license attributions for third-party software referenced or utilized by Orchestrivo.

---

## 1. Scheduling Algorithm Reference Simulator

- **Project:** SchedulingAlgorithmSimulator
- **Author:** Kautilya DK
- **Source Repository:** https://github.com/kautilyadk/SchedulingAlgorithmSimulator
- **License:** MIT License

### Original License Text:

```text
MIT License

Copyright (c) 2024 Kautilya DK

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

**Context & Role in Orchestrivo:**
The discrete CPU scheduling mathematical models (FCFS, Non-Preemptive SJF, Preemptive SRTF, and Round Robin quantum slicing) in Orchestrivo reference the algorithm logic and tie-breaking conventions formulated in Kautilya DK's simulator. Orchestrivo implements these algorithms natively in Java 17 within its deterministic scheduling domain layer (`com.orchestrivo.domain`).

---

## 2. Java Runtime and Build Dependencies

The following open-source dependencies are referenced via Maven dependencies:

| Dependency | License | Project URL |
|------------|---------|-------------|
| **HikariCP** (com.zaxxer:HikariCP) | Apache License 2.0 | https://github.com/brettwooldridge/HikariCP |
| **MySQL Connector/J** (com.mysql:mysql-connector-j) | GPLv2 with FOSS Exception | https://github.com/mysql/mysql-connector-j |
| **Flyway Community** (org.flywaydb:flyway-core, flyway-mysql) | Apache License 2.0 | https://flywaydb.org |
| **Google Gson** (com.google.code.gson:gson) | Apache License 2.0 | https://github.com/google/gson |
| **BCrypt Password Hashing** (at.favre.lib:bcrypt) | Apache License 2.0 | https://github.com/patrickfav/bcrypt |
| **Jakarta Servlet API** (jakarta.servlet:jakarta.servlet-api) | Eclipse Public License 2.0 | https://projects.eclipse.org/projects/ee4j.servlet |
| **Jakarta JSP API** (jakarta.servlet.jsp:jakarta.servlet.jsp-api) | Eclipse Public License 2.0 | https://projects.eclipse.org/projects/ee4j.jsp |
| **Jakarta JSTL** (jakarta.servlet.jsp.jstl) | Eclipse Public License 2.0 | https://projects.eclipse.org/projects/ee4j.jstl |
| **JUnit 5 Jupiter** (org.junit.jupiter:junit-jupiter) | Eclipse Public License 2.0 | https://junit.org/junit5/ |
| **SLF4J API & Simple** (org.slf4j:slf4j-api, slf4j-simple) | MIT License | https://www.slf4j.org/ |
