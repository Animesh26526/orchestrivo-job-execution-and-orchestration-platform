<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Benchmark Before/After Comparison - Orchestrivo</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/app.css">
    <style>
        .delta-good { color: #15803d; font-weight: 700; }
        .delta-bad { color: #b91c1c; font-weight: 700; }
        .delta-parity { color: var(--color-slate-600); font-weight: 600; }
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
        <span>Before / After Comparison</span>
    </div>

    <div class="page-header">
        <div class="page-title-group">
            <h1>Benchmark Before/After Comparison</h1>
            <p class="page-subtitle">Pairwise performance delta analysis between Baseline and Candidate runs</p>
        </div>
        <div class="page-actions">
            <a href="<%= request.getContextPath() %>/benchmarks" class="btn btn-secondary">&larr; Back to Benchmarks</a>
            <c:if test="${not empty comparison}">
                <a href="<%= request.getContextPath() %>/benchmarks/compare?baselineId=${comparison.baseline.id}&candidateId=${comparison.candidate.id}&export=csv" class="btn btn-primary">Export Comparison CSV</a>
            </c:if>
        </div>
    </div>

    <c:choose>
        <c:when test="${empty comparison}">
            <!-- Selection Card if no comparison runs specified -->
            <div class="card" style="max-width: 650px;">
                <div class="card-header">
                    <h2 class="card-title">Select Runs for Pairwise Comparison</h2>
                </div>
                <form action="<%= request.getContextPath() %>/benchmarks/compare" method="get">
                    <div class="form-group">
                        <label class="form-label" for="baselineId">Baseline Run (Before):</label>
                        <select id="baselineId" name="baselineId" class="form-select" required>
                            <c:forEach var="b" items="${benchmarks}">
                                <option value="${b.id}">Run #${b.id} &mdash; ${b.algorithmName} (${b.processCount} procs, ${String.format('%.1f', b.medianMicros)} &micro;s) [${b.runType}]</option>
                            </c:forEach>
                        </select>
                    </div>

                    <div class="form-group">
                        <label class="form-label" for="candidateId">Candidate Run (After):</label>
                        <select id="candidateId" name="candidateId" class="form-select" required>
                            <c:forEach var="c" items="${benchmarks}">
                                <option value="${c.id}">Run #${c.id} &mdash; ${c.algorithmName} (${c.processCount} procs, ${String.format('%.1f', c.medianMicros)} &micro;s) [${c.runType}]</option>
                            </c:forEach>
                        </select>
                    </div>

                    <div style="margin-top: 20px;">
                        <button type="submit" class="btn btn-primary">Compare Benchmarks &rarr;</button>
                    </div>
                </form>
            </div>
        </c:when>

        <c:otherwise>
            <!-- Workload Matching Banner -->
            <c:choose>
                <c:when test="${comparison.workloadMatched}">
                    <div class="alert alert-success" style="display: flex; align-items: flex-start; gap: 12px;">
                        <span class="badge badge-success" style="font-size: 12px; padding: 6px 10px; white-space: nowrap;">Controlled Match</span>
                        <div>
                            <strong>Rigorous Workload Equivalence:</strong>
                            <p style="margin-top: 4px; margin-bottom: 0;"><c:out value="${comparison.matchNotes}"/></p>
                        </div>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="alert alert-warning" style="display: flex; align-items: flex-start; gap: 12px;">
                        <span class="badge badge-warning" style="font-size: 12px; padding: 6px 10px; white-space: nowrap;">Workload Divergence</span>
                        <div>
                            <strong>Uncontrolled Workload Warning:</strong>
                            <p style="margin-top: 4px; margin-bottom: 0;"><c:out value="${comparison.matchNotes}"/></p>
                        </div>
                    </div>
                </c:otherwise>
            </c:choose>

            <!-- KPI Cards -->
            <div class="metrics-grid">
                <div class="metric-card">
                    <div class="metric-header">
                        <span class="metric-label">Speedup Factor</span>
                        <span class="badge ${comparison.improvement ? 'badge-success' : (comparison.regression ? 'badge-danger' : 'badge-neutral')}">
                            ${comparison.improvement ? 'FASTER' : (comparison.regression ? 'SLOWER' : 'PARITY')}
                        </span>
                    </div>
                    <div class="metric-value ${comparison.improvement ? 'delta-good' : (comparison.regression ? 'delta-bad' : 'delta-parity')}">
                        ${comparison.formattedSpeedup}
                    </div>
                    <div class="metric-subtext">Baseline vs Candidate Throughput Ratio</div>
                </div>

                <div class="metric-card">
                    <div class="metric-header">
                        <span class="metric-label">Median Latency Delta</span>
                        <span class="badge badge-primary">&Delta; P50</span>
                    </div>
                    <div class="metric-value ${comparison.deltaMedianPct < -0.5 ? 'delta-good' : (comparison.deltaMedianPct > 0.5 ? 'delta-bad' : 'delta-parity')}">
                        ${comparison.formattedDeltaMedian}
                    </div>
                    <div class="metric-subtext">Candidate vs Baseline Median Latency</div>
                </div>

                <div class="metric-card">
                    <div class="metric-header">
                        <span class="metric-label">Throughput Delta</span>
                        <span class="badge badge-primary">&Delta; Runs/sec</span>
                    </div>
                    <div class="metric-value ${comparison.deltaThroughputPct > 0.5 ? 'delta-good' : (comparison.deltaThroughputPct < -0.5 ? 'delta-bad' : 'delta-parity')}">
                        ${comparison.formattedDeltaThroughput}
                    </div>
                    <div class="metric-subtext">Simulated scheduling runs per second</div>
                </div>

                <div class="metric-card">
                    <div class="metric-header">
                        <span class="metric-label">Jitter / Stability Delta</span>
                        <span class="badge badge-neutral">&Delta; StdDev</span>
                    </div>
                    <div class="metric-value ${comparison.deltaStdDevPct < -0.5 ? 'delta-good' : (comparison.deltaStdDevPct > 0.5 ? 'delta-bad' : 'delta-parity')}">
                        ${comparison.formattedDeltaStdDev}
                    </div>
                    <div class="metric-subtext">Dispersion change across repetitions</div>
                </div>
            </div>

            <!-- Detailed Comparison Table -->
            <div class="card">
                <div class="card-header">
                    <div>
                        <h2 class="card-title">Side-by-Side Benchmark Metrics</h2>
                        <p class="card-description">Detailed execution properties measured on host engine</p>
                    </div>
                </div>
                <div class="table-responsive" style="margin: 0; box-shadow: none; border: none;">
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th style="width: 220px;">Metric / Property</th>
                                <th>
                                    <strong>Baseline: Run #${comparison.baseline.id}</strong><br/>
                                    <small style="color: var(--color-slate-500); font-weight: normal;">Type: <code>${comparison.baseline.runType}</code></small>
                                </th>
                                <th>
                                    <strong>Candidate: Run #${comparison.candidate.id}</strong><br/>
                                    <small style="color: var(--color-slate-500); font-weight: normal;">Type: <code>${comparison.candidate.runType}</code></small>
                                </th>
                                <th>Performance Difference (&Delta;)</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td style="font-weight: 600;">Algorithm</td>
                                <td><code><c:out value="${comparison.baseline.algorithmName}"/></code></td>
                                <td><code><c:out value="${comparison.candidate.algorithmName}"/></code></td>
                                <td>${comparison.baseline.algorithmName == comparison.candidate.algorithmName ? '<span class="badge badge-success">IDENTICAL</span>' : '<span class="badge badge-warning">DIFFERENT</span>'}</td>
                            </tr>
                            <tr>
                                <td style="font-weight: 600;">Process Count</td>
                                <td>${comparison.baseline.processCount}</td>
                                <td>${comparison.candidate.processCount}</td>
                                <td>${comparison.baseline.processCount == comparison.candidate.processCount ? '<span class="badge badge-success">MATCHED</span>' : '<span class="badge badge-warning">DIFFERENT</span>'}</td>
                            </tr>
                            <tr>
                                <td style="font-weight: 600;">Reproducibility Seed</td>
                                <td><code>${comparison.baseline.seed}</code></td>
                                <td><code>${comparison.candidate.seed}</code></td>
                                <td>${comparison.baseline.seed == comparison.candidate.seed ? '<span class="badge badge-success">MATCHED</span>' : '<span class="badge badge-warning">DIFFERENT</span>'}</td>
                            </tr>
                            <tr>
                                <td style="font-weight: 600;">Median Latency (P50)</td>
                                <td>${String.format('%.2f', comparison.baseline.medianMicros)} &micro;s</td>
                                <td>${String.format('%.2f', comparison.candidate.medianMicros)} &micro;s</td>
                                <td class="${comparison.deltaMedianPct < -0.5 ? 'delta-good' : (comparison.deltaMedianPct > 0.5 ? 'delta-bad' : 'delta-parity')}">
                                    ${comparison.formattedDeltaMedian}
                                </td>
                            </tr>
                            <tr>
                                <td style="font-weight: 600;">Mean Latency</td>
                                <td>${String.format('%.2f', comparison.baseline.meanMicros)} &micro;s</td>
                                <td>${String.format('%.2f', comparison.candidate.meanMicros)} &micro;s</td>
                                <td class="${comparison.deltaMeanPct < -0.5 ? 'delta-good' : (comparison.deltaMeanPct > 0.5 ? 'delta-bad' : 'delta-parity')}">
                                    ${comparison.formattedDeltaMean}
                                </td>
                            </tr>
                            <tr>
                                <td style="font-weight: 600;">Min Latency (Fastest)</td>
                                <td>${String.format('%.2f', comparison.baseline.minMicros)} &micro;s</td>
                                <td>${String.format('%.2f', comparison.candidate.minMicros)} &micro;s</td>
                                <td>${String.format('%+.2f', (comparison.deltaMinNanos / 1000.0))} &micro;s</td>
                            </tr>
                            <tr>
                                <td style="font-weight: 600;">Max Latency (Slowest)</td>
                                <td>${String.format('%.2f', comparison.baseline.maxMicros)} &micro;s</td>
                                <td>${String.format('%.2f', comparison.candidate.maxMicros)} &micro;s</td>
                                <td>${String.format('%+.2f', (comparison.deltaMaxNanos / 1000.0))} &micro;s</td>
                            </tr>
                            <tr>
                                <td style="font-weight: 600;">Standard Deviation</td>
                                <td>${String.format('%.2f', comparison.baseline.stdDevMicros)} &micro;s</td>
                                <td>${String.format('%.2f', comparison.candidate.stdDevMicros)} &micro;s</td>
                                <td class="${comparison.deltaStdDevPct < -0.5 ? 'delta-good' : (comparison.deltaStdDevPct > 0.5 ? 'delta-bad' : 'delta-parity')}">
                                    ${comparison.formattedDeltaStdDev}
                                </td>
                            </tr>
                            <tr>
                                <td style="font-weight: 600;">Throughput (Runs/Sec)</td>
                                <td>${String.format('%.1f', comparison.baselineThroughput)} runs/s</td>
                                <td>${String.format('%.1f', comparison.candidateThroughput)} runs/s</td>
                                <td class="${comparison.deltaThroughputPct > 0.5 ? 'delta-good' : (comparison.deltaThroughputPct < -0.5 ? 'delta-bad' : 'delta-parity')}">
                                    ${comparison.formattedDeltaThroughput}
                                </td>
                            </tr>
                            <tr>
                                <td style="font-weight: 600;">Invariants Validated</td>
                                <td><span class="badge ${comparison.baseline.validationPassed ? 'badge-success' : 'badge-danger'}">${comparison.baseline.validationPassed ? 'PASSED' : 'FAILED'}</span></td>
                                <td><span class="badge ${comparison.candidate.validationPassed ? 'badge-success' : 'badge-danger'}">${comparison.candidate.validationPassed ? 'PASSED' : 'FAILED'}</span></td>
                                <td>${comparison.baseline.validationPassed && comparison.candidate.validationPassed ? '<span class="badge badge-success">BOTH VALID</span>' : '<span class="badge badge-danger">INVALID</span>'}</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <div class="alert alert-info">
                <div>
                    <strong>Scientific Measurement Methodology:</strong>
                    Micro-benchmarks isolate pure algorithmic execution time on the host JVM. Zero sleep delays or synthetic pauses are introduced. A speedup factor &gt; 1.00 indicates performance improvement.
                </div>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<script src="<%= request.getContextPath() %>/assets/js/app.js"></script>
</body>
</html>
