<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
    <title>Run Engine Micro-Benchmark - Orchestrivo</title>
    <style>
        body { font-family: sans-serif; margin: 20px; line-height: 1.5; color: #333; }
        .nav { margin-bottom: 15px; }
        .nav a { margin-right: 15px; text-decoration: none; color: #0066cc; }
        .form-container { background: #fdfdfd; border: 1px solid #ddd; border-radius: 4px; padding: 20px; max-width: 650px; }
        .form-group { margin-bottom: 15px; }
        label { display: block; font-weight: bold; margin-bottom: 5px; font-size: 0.9em; }
        input[type="text"], input[type="number"], select { width: 100%; box-sizing: border-box; padding: 8px 10px; border: 1px solid #ccc; border-radius: 4px; font-size: 0.95em; }
        .grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; }
        .btn { display: inline-block; padding: 8px 16px; background: #0066cc; color: #fff; text-decoration: none; border-radius: 4px; font-size: 0.95em; border: none; cursor: pointer; }
        .btn:hover { background: #0052a3; }
        .help-text { font-size: 0.8em; color: #666; margin-top: 4px; }
        .alert-error { background: #fce8e6; border: 1px solid #fad2cf; color: #c5221f; padding: 10px 14px; border-radius: 4px; margin-bottom: 15px; max-width: 650px; }
        .note { font-size: 0.85em; color: #555; background: #f0f4f8; border-left: 4px solid #1a73e8; padding: 10px 14px; margin-bottom: 15px; max-width: 650px; }
    </style>
</head>
<body>

<div class="nav">
    <a href="<%= request.getContextPath() %>/benchmarks">&larr; Back to Benchmarks</a>
    <a href="<%= request.getContextPath() %>/dashboard">Dashboard</a>
</div>

<h2>Configure Algorithm Micro-Benchmark</h2>

<div class="note">
    <strong>Micro-Benchmark Execution Protocol:</strong>
    Warm-up cycles trigger JVM JIT compilation without recording timestamps. Timed iterations subsequently measure elapsed wall-clock nanoseconds using <code>System.nanoTime()</code>. On every measured repetition, output schedules undergo independent validation.
</div>

<c:if test="${not empty error}">
    <div class="alert-error">
        <strong>Error:</strong> <c:out value="${error}"/>
    </div>
</c:if>

<div class="form-container">
    <form action="<%= request.getContextPath() %>/benchmarks/run" method="post">
        <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}'/>"/>

        <div class="form-group">
            <label for="algorithmName">Scheduling Algorithm:</label>
            <select id="algorithmName" name="algorithmName" required onchange="toggleQuantum()">
                <option value="FCFS" ${config.algorithmName == 'FCFS' ? 'selected' : ''}>First-Come, First-Served (FCFS)</option>
                <option value="SJF" ${config.algorithmName == 'SJF' ? 'selected' : ''}>Shortest Job First (SJF - Non-preemptive)</option>
                <option value="SRTF" ${config.algorithmName == 'SRTF' ? 'selected' : ''}>Shortest Remaining Time First (SRTF - Preemptive)</option>
                <option value="ROUND_ROBIN" ${config.algorithmName == 'ROUND_ROBIN' ? 'selected' : ''}>Round Robin (RR)</option>
            </select>
        </div>

        <div class="form-group" id="quantumGroup" style="display: none;">
            <label for="quantum">Time Quantum (for Round Robin):</label>
            <input type="number" id="quantum" name="quantum" value="${config.quantum != 0 ? config.quantum : 3}" min="1" max="100"/>
        </div>

        <div class="grid-2">
            <div class="form-group">
                <label for="processCount">Process Count (1 - 500):</label>
                <input type="number" id="processCount" name="processCount" value="${config.processCount != 0 ? config.processCount : 50}" min="1" max="500" required/>
            </div>
            <div class="form-group">
                <label for="seed">Reproducibility Seed:</label>
                <input type="number" id="seed" name="seed" value="${config.seed != 0 ? config.seed : 42}" required/>
                <div class="help-text">Constant seed ensures identical synthetic process sets.</div>
            </div>
        </div>

        <div class="grid-2">
            <div class="form-group">
                <label for="minArrival">Min Arrival Time (>= 0):</label>
                <input type="number" id="minArrival" name="minArrival" value="${config.minArrival != 0 ? config.minArrival : 0}" min="0" required/>
            </div>
            <div class="form-group">
                <label for="maxArrival">Max Arrival Time:</label>
                <input type="number" id="maxArrival" name="maxArrival" value="${config.maxArrival != 0 ? config.maxArrival : 50}" min="0" required/>
            </div>
        </div>

        <div class="grid-2">
            <div class="form-group">
                <label for="minBurst">Min Burst Time (>= 1):</label>
                <input type="number" id="minBurst" name="minBurst" value="${config.minBurst != 0 ? config.minBurst : 1}" min="1" required/>
            </div>
            <div class="form-group">
                <label for="maxBurst">Max Burst Time:</label>
                <input type="number" id="maxBurst" name="maxBurst" value="${config.maxBurst != 0 ? config.maxBurst : 20}" min="1" required/>
            </div>
        </div>

        <div class="grid-2">
            <div class="form-group">
                <label for="warmupIterations">Warm-Up Cycles (1 - 100):</label>
                <input type="number" id="warmupIterations" name="warmupIterations" value="${config.warmupIterations != 0 ? config.warmupIterations : 10}" min="1" max="100" required/>
                <div class="help-text">Untimed iterations to prime JIT.</div>
            </div>
            <div class="form-group">
                <label for="measuredIterations">Measured Iterations (1 - 500):</label>
                <input type="number" id="measuredIterations" name="measuredIterations" value="${config.measuredIterations != 0 ? config.measuredIterations : 50}" min="1" max="500" required/>
                <div class="help-text">Timed iterations using nanoTime().</div>
            </div>
        </div>

        <div style="margin-top: 20px;">
            <button type="submit" class="btn">Execute Benchmark</button>
            <a href="<%= request.getContextPath() %>/benchmarks" style="margin-left: 15px; color: #666; text-decoration: none;">Cancel</a>
        </div>
    </form>
</div>

<script>
function toggleQuantum() {
    var algo = document.getElementById("algorithmName").value;
    var qGroup = document.getElementById("quantumGroup");
    if (algo === "ROUND_ROBIN") {
        qGroup.style.display = "block";
    } else {
        qGroup.style.display = "none";
    }
}
toggleQuantum();
</script>

</body>
</html>
