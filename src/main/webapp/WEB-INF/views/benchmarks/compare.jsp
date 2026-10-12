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
        <div class="page-actions" style="display: flex; gap: 8px; align-items: center; flex-wrap: wrap;">
            <a href="<%= request.getContextPath() %>/benchmarks" class="btn btn-secondary">&larr; Back to Benchmarks</a>
            <a href="<%= request.getContextPath() %>/benchmarks/guide" class="btn btn-outline">Methodology Guide</a>
            <c:if test="${not empty comparison}">
                <c:choose>
                    <c:when test="${isFeatured}">
                        <form action="<%= request.getContextPath() %>/benchmarks/feature" method="post" style="display: inline; margin: 0;">
                            <input type="hidden" name="action" value="unpin">
                            <input type="hidden" name="baselineId" value="${comparison.baseline.id}">
                            <input type="hidden" name="candidateId" value="${comparison.candidate.id}">
                            <button type="submit" class="btn btn-warning" title="Remove as featured comparison on Dashboard">★ Pinned on Dashboard (Click to Unpin)</button>
                        </form>
                    </c:when>
                    <c:otherwise>
                        <form action="<%= request.getContextPath() %>/benchmarks/feature" method="post" style="display: inline; margin: 0;">
                            <input type="hidden" name="action" value="pin">
                            <input type="hidden" name="baselineId" value="${comparison.baseline.id}">
                            <input type="hidden" name="candidateId" value="${comparison.candidate.id}">
                            <button type="submit" class="btn btn-secondary" title="Feature this comparison permanently on the Dashboard">☆ Pin to Dashboard</button>
                        </form>
                    </c:otherwise>
                </c:choose>
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
                    <div class="metric-subtext">Baseline vs Candidate Latency Ratio (P50)</div>
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
                        <span class="metric-label">Actual Batch Throughput Delta</span>
                        <span class="badge badge-primary">&Delta; Measured Runs/sec</span>
                    </div>
                    <div class="metric-value ${comparison.deltaActualBatchThroughputPct > 0.5 ? 'delta-good' : (comparison.deltaActualBatchThroughputPct < -0.5 ? 'delta-bad' : 'delta-parity')}">
                        ${comparison.formattedDeltaActualBatchThroughput}
                    </div>
                    <div class="metric-subtext">Measured: valid runs / wall-clock batch seconds</div>
                </div>

                <div class="metric-card">
                    <div class="metric-header">
                        <span class="metric-label">Latency-Derived Throughput (Est.)</span>
                        <span class="badge badge-neutral">&Delta; 1/Median Est.</span>
                    </div>
                    <div class="metric-value ${comparison.deltaLatencyDerivedThroughputEstimatePct > 0.5 ? 'delta-good' : (comparison.deltaLatencyDerivedThroughputEstimatePct < -0.5 ? 'delta-bad' : 'delta-parity')}">
                        ${comparison.formattedDeltaLatencyDerivedThroughputEstimate}
                    </div>
                    <div class="metric-subtext">Theoretical reciprocal estimate: 1 / P50</div>
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
                                <th style="width: 240px;">Metric / Property</th>
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
                            <c:if test="${comparison.baseline.algorithmName == 'ROUND_ROBIN' || comparison.candidate.algorithmName == 'ROUND_ROBIN'}">
                            <tr>
                                <td style="font-weight: 600;">Time Quantum (q)</td>
                                <td>${comparison.baseline.quantum > 0 ? comparison.baseline.quantum : 'N/A'}</td>
                                <td>${comparison.candidate.quantum > 0 ? comparison.candidate.quantum : 'N/A'}</td>
                                <td>${comparison.baseline.quantum == comparison.candidate.quantum ? '<span class="badge badge-success">MATCHED</span>' : '<span class="badge badge-warning">DIFFERENT</span>'}</td>
                            </tr>
                            </c:if>
                            <tr>
                                <td style="font-weight: 600;">Reproducibility Seed</td>
                                <td><code>${comparison.baseline.seed}</code></td>
                                <td><code>${comparison.candidate.seed}</code></td>
                                <td>${comparison.baseline.seed == comparison.candidate.seed ? '<span class="badge badge-success">MATCHED</span>' : '<span class="badge badge-warning">DIFFERENT</span>'}</td>
                            </tr>
                            <tr>
                                <td style="font-weight: 600;">Warm-up / Measured Runs</td>
                                <td>${comparison.baseline.warmupIterations} warm-up / ${comparison.baseline.measuredIterations} measured</td>
                                <td>${comparison.candidate.warmupIterations} warm-up / ${comparison.candidate.measuredIterations} measured</td>
                                <td>${(comparison.baseline.warmupIterations == comparison.candidate.warmupIterations && comparison.baseline.measuredIterations == comparison.candidate.measuredIterations) ? '<span class="badge badge-success">MATCHED</span>' : '<span class="badge badge-warning">DIFFERENT</span>'}</td>
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
                            <tr style="background-color: var(--color-slate-50, #f8fafc);">
                                <td style="font-weight: 700;">Actual Batch Throughput (runs/sec)</td>
                                <td><strong>${String.format('%.1f', comparison.baselineActualBatchThroughput)}</strong> runs/s</td>
                                <td><strong>${String.format('%.1f', comparison.candidateActualBatchThroughput)}</strong> runs/s</td>
                                <td class="${comparison.deltaActualBatchThroughputPct > 0.5 ? 'delta-good' : (comparison.deltaActualBatchThroughputPct < -0.5 ? 'delta-bad' : 'delta-parity')}">
                                    ${comparison.formattedDeltaActualBatchThroughput}
                                </td>
                            </tr>
                            <tr>
                                <td style="font-weight: 600; color: var(--color-slate-600);">Latency-Derived Throughput Estimate (1/P50)</td>
                                <td>${String.format('%.1f', comparison.baselineLatencyDerivedThroughputEstimate)} runs/s</td>
                                <td>${String.format('%.1f', comparison.candidateLatencyDerivedThroughputEstimate)} runs/s</td>
                                <td class="${comparison.deltaLatencyDerivedThroughputEstimatePct > 0.5 ? 'delta-good' : (comparison.deltaLatencyDerivedThroughputEstimatePct < -0.5 ? 'delta-bad' : 'delta-parity')}">
                                    ${comparison.formattedDeltaLatencyDerivedThroughputEstimate}
                                </td>
                            </tr>
                            <tr>
                                <td style="font-weight: 600;">Batch Wall-Clock Duration</td>
                                <td>${comparison.baseline.formattedBatchElapsed}</td>
                                <td>${comparison.candidate.formattedBatchElapsed}</td>
                                <td>${comparison.baseline.validRunCount} / ${comparison.baseline.measuredIterations} vs ${comparison.candidate.validRunCount} / ${comparison.candidate.measuredIterations} valid runs</td>
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
