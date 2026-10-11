<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Benchmark #${benchmark.id} Report - Orchestrivo</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/app.css">
    <style>
        .latency-chart-container {
            width: 100%;
            height: 220px;
            background: #ffffff;
            border: 1px solid var(--color-slate-200);
            border-radius: var(--radius-sm);
            padding: 12px;
            margin-top: 14px;
            overflow-x: auto;
        }
    </style>
</head>
<body>

<jsp:include page="/WEB-INF/views/layout/navbar.jsp"/>

<div class="app-container">
    <div class="breadcrumb-bar">
        <a href="<%= request.getContextPath() %>/dashboard">Dashboard</a>
        <span class="breadcrumb-separator">/</span>
        <a href="<%= request.getContextPath() %>/benchmarks">Benchmarks</a>
        <span class="breadcrumb-separator">/</span>
        <span>Benchmark #${benchmark.id}</span>
    </div>

    <div class="page-header">
        <div class="page-title-group">
            <h1>Micro-Benchmark Report #${benchmark.id}: <c:out value="${benchmark.algorithmName}"/></h1>
            <p class="page-subtitle">${benchmark.processCount} synthetic processes &bull; ${benchmark.measuredIterations} timed iterations</p>
        </div>
        <div class="page-actions">
            <a href="<%= request.getContextPath() %>/benchmarks" class="btn btn-secondary">&larr; Back to Benchmarks</a>
            <a href="<%= request.getContextPath() %>/benchmarks/run" class="btn btn-primary">+ Run New Benchmark</a>
        </div>
    </div>

    <!-- Summary Card -->
    <div class="card">
        <div class="card-header">
            <h2 class="card-title">Benchmark Run Configuration</h2>
            <span class="badge ${benchmark.validationPassed ? 'badge-success' : 'badge-danger'}">
                ${benchmark.validationPassed ? 'PASSED (100% Invariants Verified)' : 'FAILED'}
            </span>
        </div>
        <div class="table-responsive" style="margin: 0; box-shadow: none; border: none;">
            <table class="data-table">
                <tbody>
                    <tr>
                        <td style="width: 240px; font-weight: 600;">Algorithm:</td>
                        <td><strong><c:out value="${benchmark.algorithmName}"/></strong></td>
                    </tr>
                    <tr>
                        <td style="font-weight: 600;">Process Count:</td>
                        <td>${benchmark.processCount} processes</td>
                    </tr>
                    <tr>
                        <td style="font-weight: 600;">Reproducibility Seed:</td>
                        <td><code>${benchmark.seed}</code></td>
                    </tr>
                    <tr>
                        <td style="font-weight: 600;">Protocol Iterations:</td>
                        <td>${benchmark.warmupIterations} warm-up cycles &bull; ${benchmark.measuredIterations} measured repetitions</td>
                    </tr>
                    <tr>
                        <td style="font-weight: 600;">Execution Timestamp:</td>
                        <td><c:out value="${benchmark.createdAt}"/></td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>

    <!-- Statistical Metric Cards -->
    <h2 style="font-size: 18px; font-weight: 600; margin-bottom: 12px; color: var(--color-slate-900);">Statistical Latency Summary (Wall-Clock Host Engine)</h2>
    <div class="metrics-grid">
        <div class="metric-card">
            <div class="metric-header">
                <span class="metric-label">Median Latency</span>
                <span class="badge badge-primary">P50</span>
            </div>
            <div class="metric-value">${benchmark.medianMicros} &micro;s</div>
            <div class="metric-subtext">(${benchmark.medianMillis} ms)</div>
        </div>
        <div class="metric-card">
            <div class="metric-header">
                <span class="metric-label">Min Latency</span>
                <span class="badge badge-success">Fastest</span>
            </div>
            <div class="metric-value">${benchmark.minMicros} &micro;s</div>
            <div class="metric-subtext">(${benchmark.minMillis} ms)</div>
        </div>
        <div class="metric-card">
            <div class="metric-header">
                <span class="metric-label">Max Latency</span>
                <span class="badge badge-warning">Slowest</span>
            </div>
            <div class="metric-value">${benchmark.maxMicros} &micro;s</div>
            <div class="metric-subtext">(${benchmark.maxMillis} ms)</div>
        </div>
        <div class="metric-card">
            <div class="metric-header">
                <span class="metric-label">Std Deviation</span>
                <span class="badge badge-neutral">Variance</span>
            </div>
            <div class="metric-value">${benchmark.stdDevMicros} &micro;s</div>
            <div class="metric-subtext">(${benchmark.stdDevMillis} ms)</div>
        </div>
    </div>

    <div class="alert alert-info">
        <div>
            <strong>Host Latency vs Scheduling Domain Separation:</strong>
            These latency measurements capture raw JVM execution time of the scheduling algorithm in memory. They are strictly separate from simulated scheduling domain metrics (e.g. turnaround time or waiting time). Furthermore, these micro-benchmarks are isolated execution measurements on the local host machine, not proof of clustered or distributed throughput.
        </div>
    </div>

    <!-- Latency Distribution SVG Chart -->
    <div class="card">
        <div class="card-header">
            <div>
                <h2 class="card-title">Per-Iteration Latency Distribution</h2>
                <p class="card-description">Sequential latency across measured repetitions (Microseconds &micro;s)</p>
            </div>
            <div style="font-size: 12px; color: var(--color-slate-500);">
                Min: <strong>${benchmark.minMicros} &micro;s</strong> &bull; Median: <strong>${benchmark.medianMicros} &micro;s</strong> &bull; Max: <strong>${benchmark.maxMicros} &micro;s</strong>
            </div>
        </div>
        <div class="latency-chart-container">
            <svg id="latencySvg" width="100%" height="190" viewBox="0 0 800 180" preserveAspectRatio="none">
                <!-- Grid background lines -->
                <line x1="40" y1="20" x2="780" y2="20" stroke="#f1f5f9" stroke-dasharray="3,3" />
                <line x1="40" y1="80" x2="780" y2="80" stroke="#f1f5f9" stroke-dasharray="3,3" />
                <line x1="40" y1="140" x2="780" y2="140" stroke="#e2e8f0" />

                <!-- Iteration bars/polyline -->
                <c:set var="mCount" value="${benchmark.measurements.size()}"/>
                <c:set var="maxMicrosVal" value="${benchmark.maxMicros > 0 ? benchmark.maxMicros : 1}"/>
                <c:forEach var="m" items="${benchmark.measurements}" varStatus="status">
                    <c:set var="micros" value="${m.durationNanos / 1000.0}"/>
                    <c:set var="barX" value="${40 + (status.index * (740.0 / (mCount > 1 ? mCount - 1 : 1)))}"/>
                    <c:set var="barH" value="${(micros / maxMicrosVal) * 120.0}"/>
                    <c:set var="barY" value="${140 - barH}"/>
                    <rect x="${barX - 1.5}" y="${barY}" width="3" height="${barH}" fill="#3b82f6" opacity="0.85">
                        <title>Iteration #${m.iterationNumber}: ${micros} &micro;s</title>
                    </rect>
                </c:forEach>

                <!-- Labels -->
                <text x="35" y="24" fill="#94a3b8" font-size="10" text-anchor="end">${benchmark.maxMicros}&micro;s</text>
                <text x="35" y="84" fill="#94a3b8" font-size="10" text-anchor="end">${benchmark.medianMicros}&micro;s</text>
                <text x="35" y="144" fill="#94a3b8" font-size="10" text-anchor="end">0&micro;s</text>
                <text x="40" y="165" fill="#64748b" font-size="10">Iter #1</text>
                <text x="780" y="165" fill="#64748b" font-size="10" text-anchor="end">Iter #${benchmark.measurements.size()}</text>
            </svg>
        </div>
    </div>

    <!-- Host Runtime Environment -->
    <div class="card">
        <div class="card-header">
            <div>
                <h2 class="card-title">Host Runtime Environment Metadata</h2>
                <p class="card-description">JVM, CPU, and host OS specifications captured during execution</p>
            </div>
        </div>
        <div class="code-box">
            <c:out value="${benchmark.environmentInfo}"/>
        </div>
    </div>

    <!-- Measured Iterations Table -->
    <div class="card">
        <div class="card-header">
            <div>
                <h2 class="card-title">Recorded Repetition Iterations</h2>
                <p class="card-description">${benchmark.measurements.size()} timed repetitions</p>
            </div>
        </div>
        <div class="table-responsive" style="margin: 0; box-shadow: none; border: none; max-height: 360px; overflow-y: auto;">
            <table class="data-table">
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
                        <td><strong>#${m.iterationNumber}</strong></td>
                        <td><code>${m.durationNanos} ns</code></td>
                        <td><strong>${String.format('%.2f', m.durationNanos / 1000.0)} &micro;s</strong></td>
                        <td>${String.format('%.4f', m.durationNanos / 1000000.0)} ms</td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </div>
</div>

<script src="<%= request.getContextPath() %>/assets/js/app.js"></script>
</body>
</html>
