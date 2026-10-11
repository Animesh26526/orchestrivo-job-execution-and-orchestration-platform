<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
    <title>Benchmark #${benchmark.id} Details - Orchestrivo</title>
    <style>
        body { font-family: sans-serif; margin: 20px; line-height: 1.5; color: #333; }
        .nav { margin-bottom: 15px; }
        .nav a { margin-right: 15px; text-decoration: none; color: #0066cc; }
        .card { background: #fdfdfd; border: 1px solid #ddd; border-radius: 4px; padding: 15px; margin: 15px 0; max-width: 900px; }
        .metric-grid { display: flex; flex-wrap: wrap; gap: 15px; margin: 15px 0; }
        .metric-box { background: #f0f4f8; border: 1px solid #d0dbe5; border-radius: 4px; padding: 12px 18px; min-width: 160px; }
        .metric-label { font-size: 0.85em; color: #666; text-transform: uppercase; letter-spacing: 0.5px; }
        .metric-value { font-size: 1.4em; font-weight: bold; color: #1a365d; margin-top: 4px; }
        table { border-collapse: collapse; width: 100%; max-width: 900px; margin-top: 10px; margin-bottom: 20px; }
        th, td { border: 1px solid #ddd; padding: 8px 12px; text-align: left; }
        th { background: #f4f6f8; font-weight: 600; }
        tr:nth-child(even) { background: #fafbfc; }
        .badge { display: inline-block; padding: 3px 8px; border-radius: 3px; font-size: 0.85em; font-weight: bold; }
        .badge-success { background: #e6f4ea; color: #137333; }
        .badge-danger { background: #fce8e6; color: #c5221f; }
        .env-box { background: #2b2b2b; color: #f8f8f2; font-family: monospace; padding: 12px 16px; border-radius: 4px; overflow-x: auto; max-width: 900px; font-size: 0.9em; white-space: pre-wrap; }
        .note { font-size: 0.9em; color: #555; background: #fff8e1; border-left: 4px solid #ffb300; padding: 10px 14px; margin: 15px 0; max-width: 900px; }
        .btn { display: inline-block; padding: 6px 14px; background: #0066cc; color: #fff; text-decoration: none; border-radius: 4px; font-size: 0.9em; border: none; cursor: pointer; }
        .btn:hover { background: #0052a3; }
    </style>
</head>
<body>

<div class="nav">
    <a href="<%= request.getContextPath() %>/benchmarks">&larr; Back to Benchmarks</a>
    <a href="<%= request.getContextPath() %>/benchmarks/run">Run New Benchmark</a>
    <a href="<%= request.getContextPath() %>/dashboard">Dashboard</a>
</div>

<h2>Micro-Benchmark Run #${benchmark.id}</h2>

<div class="card">
    <table style="width: auto; margin: 0; border: none;">
        <tr><td style="border:none; padding: 4px 10px; font-weight:bold;">Algorithm:</td><td style="border:none; padding: 4px 10px;"><strong><c:out value="${benchmark.algorithmName}"/></strong></td></tr>
        <tr><td style="border:none; padding: 4px 10px; font-weight:bold;">Process Count:</td><td style="border:none; padding: 4px 10px;">${benchmark.processCount}</td></tr>
        <tr><td style="border:none; padding: 4px 10px; font-weight:bold;">Reproducibility Seed:</td><td style="border:none; padding: 4px 10px;">${benchmark.seed}</td></tr>
        <tr><td style="border:none; padding: 4px 10px; font-weight:bold;">Warm-Up / Measured Iterations:</td><td style="border:none; padding: 4px 10px;">${benchmark.warmupIterations} warm-up / ${benchmark.measuredIterations} measured</td></tr>
        <tr>
            <td style="border:none; padding: 4px 10px; font-weight:bold;">Validation Across Runs:</td>
            <td style="border:none; padding: 4px 10px;">
                <span class="badge ${benchmark.validationPassed ? 'badge-success' : 'badge-danger'}">
                    ${benchmark.validationPassed ? 'PASSED (All schedules validated)' : 'FAILED'}
                </span>
            </td>
        </tr>
        <tr><td style="border:none; padding: 4px 10px; font-weight:bold;">Execution Timestamp:</td><td style="border:none; padding: 4px 10px;"><c:out value="${benchmark.createdAt}"/></td></tr>
    </table>
</div>

<h3>Statistical Execution Metrics (Wall-Clock Latency)</h3>
<div class="metric-grid">
    <div class="metric-box">
        <div class="metric-label">Median Latency</div>
        <div class="metric-value">${benchmark.medianMicros} µs</div>
        <small style="color: #666;">(${benchmark.medianMillis} ms)</small>
    </div>
    <div class="metric-box">
        <div class="metric-label">Min Latency</div>
        <div class="metric-value">${benchmark.minMicros} µs</div>
        <small style="color: #666;">(${benchmark.minMillis} ms)</small>
    </div>
    <div class="metric-box">
        <div class="metric-label">Max Latency</div>
        <div class="metric-value">${benchmark.maxMicros} µs</div>
        <small style="color: #666;">(${benchmark.maxMillis} ms)</small>
    </div>
    <div class="metric-box">
        <div class="metric-label">Std Deviation</div>
        <div class="metric-value">${benchmark.stdDevMicros} µs</div>
        <small style="color: #666;">(${benchmark.stdDevMillis} ms)</small>
    </div>
</div>

<div class="note">
    <strong>Timing vs Domain Metric Separation:</strong>
    These metrics reflect engine code execution performance on the host JVM. They are completely separate from simulated metrics (e.g. turnaround time or waiting time), which model CPU scheduler domain behavior.
</div>

<h3>Host Runtime Environment</h3>
<div class="env-box"><c:out value="${benchmark.environmentInfo}"/></div>

<h3>Measured Iterations (${benchmark.measurements.size()} iterations recorded)</h3>
<table>
    <thead>
        <tr>
            <th>Iteration #</th>
            <th>Duration (Nanoseconds)</th>
            <th>Duration (Microseconds)</th>
            <th>Duration (Milliseconds)</th>
        </tr>
    </thead>
    <tbody>
    <c:forEach var="m" items="${benchmark.measurements}">
        <tr>
            <td>${m.iterationNumber}</td>
            <td>${m.durationNanos} ns</td>
            <td>${m.durationMicros} µs</td>
            <td>${m.durationMillis} ms</td>
        </tr>
    </c:forEach>
    </tbody>
</table>

</body>
</html>
