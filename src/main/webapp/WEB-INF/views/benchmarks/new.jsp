<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Configure Micro-Benchmark - Orchestrivo</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/app.css">
</head>
<body>

<jsp:include page="/WEB-INF/views/layout/navbar.jsp"/>

<div class="app-container">
    <div class="breadcrumb-bar">
        <a href="<%= request.getContextPath() %>/dashboard">Dashboard</a>
        <span class="breadcrumb-separator">/</span>
        <a href="<%= request.getContextPath() %>/benchmarks">Benchmarks</a>
        <span class="breadcrumb-separator">/</span>
        <span>New Benchmark</span>
    </div>

    <div class="page-header">
        <div class="page-title-group">
            <h1>Configure Algorithm Micro-Benchmark</h1>
            <p class="page-subtitle">Set up controlled JIT warm-up repetitions and wall-clock nanosecond measurements</p>
        </div>
        <div class="page-actions">
            <a href="<%= request.getContextPath() %>/benchmarks" class="btn btn-secondary">&larr; Back to Benchmarks</a>
        </div>
    </div>

    <c:if test="${not empty error}">
        <div class="alert alert-danger">
            <strong>Error:</strong> <c:out value="${error}"/>
        </div>
    </c:if>

    <div class="card" style="max-width: 720px;">
        <form action="<%= request.getContextPath() %>/benchmarks/run" method="post">
            <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}'/>"/>

            <div class="form-group">
                <label class="form-label" for="algorithmName">Scheduling Algorithm:</label>
                <select id="algorithmName" name="algorithmName" class="form-select" required>
                    <option value="FCFS" ${config.algorithmName == 'FCFS' ? 'selected' : ''}>First-Come, First-Served (FCFS)</option>
                    <option value="SJF" ${config.algorithmName == 'SJF' ? 'selected' : ''}>Shortest Job First (SJF - Non-preemptive)</option>
                    <option value="SRTF" ${config.algorithmName == 'SRTF' ? 'selected' : ''}>Shortest Remaining Time First (SRTF - Preemptive)</option>
                    <option value="ROUND_ROBIN" ${config.algorithmName == 'ROUND_ROBIN' ? 'selected' : ''}>Round Robin (RR)</option>
                </select>
            </div>

            <div class="form-group" id="quantumGroup" style="display: none;">
                <label class="form-label" for="quantum">Time Quantum (for Round Robin):</label>
                <input type="number" id="quantum" name="quantum" class="form-input" value="${config.quantum != 0 ? config.quantum : 3}" min="1" max="100"/>
                <div class="form-hint">Time slice per process round</div>
            </div>

            <div class="form-grid-2">
                <div class="form-group">
                    <label class="form-label" for="runType">Run Designation / Type:</label>
                    <select id="runType" name="runType" class="form-select">
                        <option value="STANDARD" ${config.runType == 'STANDARD' ? 'selected' : ''}>Standard Benchmark</option>
                        <option value="BASELINE" ${config.runType == 'BASELINE' ? 'selected' : ''}>Baseline (Reference 'Before' Run)</option>
                        <option value="CANDIDATE" ${config.runType == 'CANDIDATE' ? 'selected' : ''}>Candidate (Optimized 'After' Run)</option>
                    </select>
                    <div class="form-hint">Allows pairwise before/after comparison</div>
                </div>
                <div class="form-group">
                    <label class="form-label" for="benchmarkGroup">Benchmark Group / Tag (Optional):</label>
                    <input type="text" id="benchmarkGroup" name="benchmarkGroup" class="form-input" value="<c:out value='${config.benchmarkGroup}'/>" placeholder="e.g. stage4-hardening">
                    <div class="form-hint">Identifier linking baseline and candidate runs</div>
                </div>
            </div>

            <div class="form-grid-2">
                <div class="form-group">
                    <label class="form-label" for="processCount">Process Count (1 - 500):</label>
                    <input type="number" id="processCount" name="processCount" class="form-input" value="${config.processCount != 0 ? config.processCount : 50}" min="1" max="500" required/>
                </div>
                <div class="form-group">
                    <label class="form-label" for="seed">Reproducibility Seed:</label>
                    <input type="number" id="seed" name="seed" class="form-input" value="${config.seed != 0 ? config.seed : 42}" required/>
                    <div class="form-hint">Fixed seed generates identical synthetic processes</div>
                </div>
            </div>

            <div class="form-grid-2">
                <div class="form-group">
                    <label class="form-label" for="minArrival">Min Arrival Time (&ge; 0):</label>
                    <input type="number" id="minArrival" name="minArrival" class="form-input" value="${config.minArrival != 0 ? config.minArrival : 0}" min="0" required/>
                </div>
                <div class="form-group">
                    <label class="form-label" for="maxArrival">Max Arrival Time:</label>
                    <input type="number" id="maxArrival" name="maxArrival" class="form-input" value="${config.maxArrival != 0 ? config.maxArrival : 50}" min="0" required/>
                </div>
            </div>

            <div class="form-grid-2">
                <div class="form-group">
                    <label class="form-label" for="minBurst">Min Burst Time (&ge; 1):</label>
                    <input type="number" id="minBurst" name="minBurst" class="form-input" value="${config.minBurst != 0 ? config.minBurst : 1}" min="1" required/>
                </div>
                <div class="form-group">
                    <label class="form-label" for="maxBurst">Max Burst Time:</label>
                    <input type="number" id="maxBurst" name="maxBurst" class="form-input" value="${config.maxBurst != 0 ? config.maxBurst : 20}" min="1" required/>
                </div>
            </div>

            <div class="form-grid-2">
                <div class="form-group">
                    <label class="form-label" for="warmupIterations">Warm-Up Cycles (1 - 100):</label>
                    <input type="number" id="warmupIterations" name="warmupIterations" class="form-input" value="${config.warmupIterations != 0 ? config.warmupIterations : 10}" min="1" max="100" required/>
                    <div class="form-hint">Untimed iterations to reach steady-state execution</div>
                </div>
                <div class="form-group">
                    <label class="form-label" for="measuredIterations">Measured Iterations (1 - 1000):</label>
                    <input type="number" id="measuredIterations" name="measuredIterations" class="form-input" value="${config.measuredIterations != 0 ? config.measuredIterations : 50}" min="1" max="1000" required/>
                    <div class="form-hint">Repetitions recorded for statistical latency distribution</div>
                </div>
            </div>

            <div style="margin-top: 24px; display: flex; gap: 10px;">
                <button type="submit" class="btn btn-primary">Execute Micro-Benchmark &rarr;</button>
                <a href="<%= request.getContextPath() %>/benchmarks" class="btn btn-secondary">Cancel</a>
            </div>
        </form>
    </div>
</div>

<script src="<%= request.getContextPath() %>/assets/js/app.js"></script>
</body>
</html>
