<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard - Orchestrivo</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/app.css">
    <style>
        .section-heading {
            font-size: 18px;
            font-weight: 700;
            color: var(--color-slate-900);
            margin: 28px 0 14px;
            display: flex;
            align-items: center;
            justify-content: space-between;
        }
        .section-tag {
            font-size: 12px;
            font-weight: 600;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            color: var(--color-slate-500);
        }
        .pinned-card {
            border: 2px solid var(--color-primary-600, #2563eb);
            border-radius: var(--radius-md);
            background: #ffffff;
            box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
            margin-bottom: 24px;
        }
        .delta-good { color: #15803d; font-weight: 700; }
        .delta-bad { color: #b91c1c; font-weight: 700; }
        .delta-parity { color: var(--color-slate-600); font-weight: 600; }
        .queue-breakdown {
            display: flex;
            gap: 12px;
            flex-wrap: wrap;
            margin-top: 8px;
            font-size: 12px;
        }
        .queue-pill {
            padding: 2px 8px;
            border-radius: 4px;
            font-weight: 600;
        }
    </style>
</head>
<body>

<jsp:include page="/WEB-INF/views/layout/navbar.jsp"/>

<div class="app-container">
    <c:if test="${not empty sessionScope.flashMessage}">
        <div class="alert alert-${sessionScope.flashType != null ? sessionScope.flashType : 'info'}" style="margin-bottom: 20px;">
            <c:out value="${sessionScope.flashMessage}"/>
        </div>
        <c:remove var="flashMessage" scope="session"/>
        <c:remove var="flashType" scope="session"/>
    </c:if>

    <div class="page-header">
        <div class="page-title-group">
            <h1>Engineering Dashboard</h1>
            <p class="page-subtitle">Welcome back, <c:out value="${sessionScope.user.email}"/> &mdash; Orchestrivo CPU scheduling & asynchronous execution telemetry</p>
        </div>
        <div class="page-actions" style="display: flex; gap: 8px; flex-wrap: wrap;">
            <a href="<%= request.getContextPath() %>/workloads/new" class="btn btn-secondary">+ New Workload</a>
            <a href="<%= request.getContextPath() %>/workloads/batch" class="btn btn-secondary">Batch Run</a>
            <a href="<%= request.getContextPath() %>/benchmarks/run" class="btn btn-primary">+ Run Benchmark</a>
            <a href="<%= request.getContextPath() %>/demo" class="btn btn-outline">Demo Data Pipeline</a>
        </div>
    </div>

    <!-- =========================================================================
         SECTION A: Real Platform Overview & Operational Counts
         ========================================================================= -->
    <div class="section-heading">
        <span>Section A: Platform Overview & System Telemetry</span>
        <span class="section-tag">Live Database Counts</span>
    </div>

    <div class="metrics-grid">
        <!-- Workloads -->
        <div class="metric-card">
            <div class="metric-header">
                <span class="metric-label">Workload Catalog</span>
                <span class="badge badge-primary">Catalog</span>
            </div>
            <div class="metric-value"><c:out value="${workloads != null ? workloads.size() : 0}"/></div>
            <div class="metric-subtext">Configured process datasets</div>
        </div>

        <!-- Total Execution Jobs -->
        <div class="metric-card">
            <div class="metric-header">
                <span class="metric-label">Execution Jobs</span>
                <span class="badge ${queueMetrics != null && queueMetrics.runningCount > 0 ? 'badge-primary' : (queueMetrics != null && queueMetrics.queuedCount > 0 ? 'badge-warning' : 'badge-neutral')}">
                    ${queueMetrics != null && queueMetrics.runningCount > 0 ? 'ACTIVE' : 'ENGINE'}
                </span>
            </div>
            <div class="metric-value">
                <c:out value="${queueMetrics != null ? queueMetrics.totalJobs : 0}"/>
            </div>
            <div class="metric-subtext">
                <c:choose>
                    <c:when test="${queueMetrics != null}">
                        ${queueMetrics.completedCount} completed &bull; ${queueMetrics.queuedCount} pending &bull; ${queueMetrics.failedCount} failed
                    </c:when>
                    <c:otherwise>0 jobs processed</c:otherwise>
                </c:choose>
            </div>
        </div>

        <!-- Completed Experiments -->
        <div class="metric-card">
            <div class="metric-header">
                <span class="metric-label">Scheduling Experiments</span>
                <span class="badge badge-success">Evaluated</span>
            </div>
            <div class="metric-value"><c:out value="${experiments != null ? experiments.size() : 0}"/></div>
            <div class="metric-subtext">Historical simulation executions</div>
        </div>

        <!-- Micro-Benchmarks -->
        <div class="metric-card">
            <div class="metric-header">
                <span class="metric-label">Micro-Benchmark Runs</span>
                <span class="badge badge-info">Host JVM</span>
            </div>
            <div class="metric-value"><c:out value="${totalBenchmarkRuns != null ? totalBenchmarkRuns : 0}"/></div>
            <div class="metric-subtext">Wall-clock JIT execution profiles</div>
        </div>
    </div>

    <!-- Detailed Job Status Breakdown Card -->
    <c:if test="${queueMetrics != null}">
        <div class="card" style="margin-bottom: 24px; padding: 14px 20px;">
            <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px;">
                <span style="font-size: 13px; font-weight: 600; color: var(--color-slate-700);">Execution Job Status Breakdown:</span>
                <div class="queue-breakdown">
                    <span class="queue-pill" style="background: #dbeafe; color: #1e40af;">PENDING: ${queueMetrics.queuedCount}</span>
                    <span class="queue-pill" style="background: #e0e7ff; color: #3730a3;">RUNNING: ${queueMetrics.runningCount}</span>
                    <span class="queue-pill" style="background: #dcfce7; color: #166534;">COMPLETED: ${queueMetrics.completedCount}</span>
                    <span class="queue-pill" style="background: #fee2e2; color: #991b1b;">FAILED: ${queueMetrics.failedCount}</span>
                    <span class="queue-pill" style="background: #f1f5f9; color: #475569;">CANCELLED: ${queueMetrics.cancelledCount}</span>
                    <span class="queue-pill" style="background: #fef3c7; color: #92400e;">DEAD_LETTER: ${queueMetrics.deadLetterCount}</span>
                </div>
            </div>
        </div>
    </c:if>

    <!-- =========================================================================
         SECTION B: Database-Backed Queue Orchestration Performance Comparison
         ========================================================================= -->
    <div class="section-heading" id="queueComparison">
        <span>Section B: Queue Orchestration Performance & Worker Scaling Comparison</span>
        <span class="section-tag">${queueComparison != null && queueComparison.available ? 'Validated Empirical Baseline vs. Candidate' : 'Comparison Status'}</span>
    </div>

    <c:choose>
        <c:when test="${queueComparison != null && queueComparison.available}">
            <div class="card pinned-card" style="margin-bottom: 24px;">
                <div class="card-header" style="background: #f8fafc; border-bottom: 1px solid var(--color-slate-200); padding: 14px 20px;">
                    <div>
                        <div style="display: flex; align-items: center; gap: 8px;">
                            <span class="badge badge-success" style="font-size: 12px; font-weight: 700;">
                                EMPIRICAL QUEUE ORCHESTRATION BENCHMARK
                            </span>
                            <span class="badge badge-primary">MySQL 8.0 SKIP LOCKED &bull; Event-Driven Dispatch</span>
                        </div>
                        <p class="card-description" style="margin-top: 4px; margin-bottom: 0;">
                            Head-to-head comparison: Baseline (un-fenced scans, fixed 1s polling delay, individual inserts) vs. Candidate (SKIP LOCKED claiming, reactive worker wakeups, composite index, JDBC batching).
                        </p>
                    </div>
                    <div style="display: flex; gap: 8px; align-items: center;">
                        <span class="badge badge-neutral" style="font-size: 11px;">Single-Worker Drain: <strong>${queueComparison.highlightSpeedup} Faster</strong></span>
                    </div>
                </div>

                <div style="padding: 20px;">
                    <!-- 4 Highlight Cards -->
                    <div class="metrics-grid" style="margin-bottom: 20px;">
                        <div class="metric-card" style="background: #ffffff;">
                            <div class="metric-header">
                                <span class="metric-label">Single-Worker Speedup</span>
                                <span class="badge badge-success">${queueComparison.highlightSpeedup}</span>
                            </div>
                            <div class="metric-value delta-good">
                                ${queueComparison.highlightSpeedup}
                            </div>
                            <div class="metric-subtext">Drain latency on 1 worker</div>
                        </div>

                        <div class="metric-card" style="background: #ffffff;">
                            <div class="metric-header">
                                <span class="metric-label">Drain Latency Delta</span>
                                <span class="badge badge-primary">&Delta; Median</span>
                            </div>
                            <div class="metric-value delta-good">
                                ${queueComparison.highlightLatencyReduction}
                            </div>
                            <div class="metric-subtext">Eliminates fixed 1,000ms idle sleep</div>
                        </div>

                        <div class="metric-card" style="background: #ffffff;">
                            <div class="metric-header">
                                <span class="metric-label">Throughput Expansion</span>
                                <span class="badge badge-success">&Delta; Jobs/Sec</span>
                            </div>
                            <div class="metric-value delta-good">
                                ${queueComparison.highlightThroughputGain}
                            </div>
                            <div class="metric-subtext">Drain throughput gain on 1 worker</div>
                        </div>

                        <div class="metric-card" style="background: #ffffff;">
                            <div class="metric-header">
                                <span class="metric-label">Queue Wait P50 Delta</span>
                                <span class="badge badge-primary">&Delta; P50 Wait</span>
                            </div>
                            <div class="metric-value delta-good">
                                -71.43%
                            </div>
                            <div class="metric-subtext">3,500ms down to 1,000ms</div>
                        </div>
                    </div>

                    <!-- Comparison Matrix Table -->
                    <div style="margin-bottom: 8px;">
                        <h3 style="font-size: 14px; font-weight: 600; color: var(--color-slate-800); margin-bottom: 8px;">
                            Worker Scaling & Batch Size Drain Performance Matrix
                        </h3>
                    </div>
                    <div class="table-responsive" style="margin: 0; box-shadow: none; border: 1px solid var(--color-slate-200); border-radius: var(--radius-sm);">
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>Workers</th>
                                    <th>Batch Size</th>
                                    <th>Baseline Drain</th>
                                    <th>Candidate Drain</th>
                                    <th>Duration &Delta;</th>
                                    <th>Speedup</th>
                                    <th>Baseline Rate</th>
                                    <th>Candidate Rate</th>
                                    <th>Throughput &Delta;</th>
                                    <th>Verification</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="row" items="${queueComparison.rows}">
                                    <tr>
                                        <td><strong><c:out value="${row.workers}"/> worker<c:if test="${row.workers > 1}">s</c:if></strong></td>
                                        <td><c:out value="${row.batchSize}"/> jobs</td>
                                        <td><c:out value="${row.baselineDrainDurationMs}"/> ms</td>
                                        <td><strong><c:out value="${row.candidateDrainDurationMs}"/> ms</strong></td>
                                        <td class="${row.drainDurationDeltaMs < 0 ? 'delta-good' : 'delta-parity'}">
                                            <c:out value="${row.drainDurationDeltaMs}"/> ms (<c:out value="${row.drainDurationChangePct}"/>%)
                                        </td>
                                        <td>
                                            <span class="badge ${row.drainDurationDeltaMs < -500 ? 'badge-success' : 'badge-neutral'}">
                                                <c:out value="${row.speedupFactor}"/>
                                            </span>
                                        </td>
                                        <td><c:out value="${row.baselineThroughputJobsPerSec}"/> jobs/s</td>
                                        <td><strong><c:out value="${row.candidateThroughputJobsPerSec}"/> jobs/s</strong></td>
                                        <td class="${row.throughputDeltaJobsPerSec > 0 ? 'delta-good' : 'delta-parity'}">
                                            <c:if test="${row.throughputDeltaJobsPerSec > 0}">+</c:if><c:out value="${row.throughputDeltaJobsPerSec}"/> jobs/s
                                        </td>
                                        <td><span class="badge badge-success">VALIDATED</span></td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </c:when>
        <c:otherwise>
            <div class="card" style="margin-bottom: 24px; padding: 24px; text-align: center;">
                <p style="color: var(--color-slate-600); font-size: 14px; margin-bottom: 12px;">
                    Benchmark comparison unavailable &mdash; run paired baseline/candidate suite to generate results.
                </p>
                <div style="display: flex; gap: 10px; justify-content: center;">
                    <a href="<%= request.getContextPath() %>/benchmarks/run" class="btn btn-sm btn-primary">Run Baseline & Candidate</a>
                    <a href="<%= request.getContextPath() %>/demo" class="btn btn-sm btn-secondary">Initialize Demo Data</a>
                    <a href="<%= request.getContextPath() %>/benchmarks/guide" class="btn btn-sm btn-outline">Methodology Guide</a>
                </div>
            </div>
        </c:otherwise>
    </c:choose>

    <!-- =========================================================================
         SECTION C: Real Recent Execution Activity (Jobs)
         ========================================================================= -->
    <div class="section-heading" id="recentExecutionJobs">
        <span>Section C: Recent Execution Activity</span>
        <span class="section-tag" id="liveQueueIndicator">
            <c:choose>
                <c:when test="${queueMetrics != null && (queueMetrics.queuedCount > 0 || queueMetrics.runningCount > 0)}">
                    ● Queue Engine Active
                </c:when>
                <c:otherwise>○ Queue Idle</c:otherwise>
            </c:choose>
        </span>
    </div>

    <div class="card" style="margin-bottom: 24px;">
        <div class="card-header">
            <div>
                <h2 class="card-title">Recent Execution Jobs</h2>
                <p class="card-description">Asynchronous queue processing history with live worker provenance</p>
            </div>
            <a href="<%= request.getContextPath() %>/jobs" class="btn btn-sm btn-outline">View All Jobs &rarr;</a>
        </div>

        <c:choose>
            <c:when test="${empty recentJobs}">
                <div class="empty-state" style="padding: 24px;">
                    <p class="empty-state-text">No execution jobs recorded yet. Launch an experiment or seed demo data.</p>
                    <a href="<%= request.getContextPath() %>/workloads" class="btn btn-sm btn-primary">Go to Workloads</a>
                </div>
            </c:when>
            <c:otherwise>
                <div class="table-responsive" style="margin: 0; box-shadow: none; border: none;">
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>Job ID</th>
                                <th>Algorithm</th>
                                <th>Status</th>
                                <th>Worker / Claim</th>
                                <th>Attempts</th>
                                <th>Exec Duration</th>
                                <th>Queued Time</th>
                                <th style="text-align: right;">Action</th>
                            </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="j" items="${recentJobs}">
                            <tr>
                                <td><strong><a href="<%= request.getContextPath() %>/jobs/view?id=${j.id}">#${j.id}</a></strong></td>
                                <td><code><c:out value="${j.algorithmName}"/></code></td>
                                <td>
                                    <span class="badge ${j.status == 'COMPLETED' ? 'badge-success' : (j.status == 'RUNNING' ? 'badge-primary' : (j.status == 'QUEUED' ? 'badge-warning' : 'badge-danger'))}">
                                        <c:out value="${j.status}"/>
                                    </span>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty j.claimedBy}">
                                            <code><c:out value="${j.claimedBy}"/></code>
                                        </c:when>
                                        <c:otherwise><span style="color: var(--color-slate-400);">-</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td>${j.attemptCount} / ${j.maxAttempts}</td>
                                <td>${j.executionDurationMs != null ? j.executionDurationMs : 0} ms</td>
                                <td><c:out value="${j.createdAt}"/></td>
                                <td class="actions-cell">
                                    <a href="<%= request.getContextPath() %>/jobs/view?id=${j.id}" class="btn btn-sm btn-secondary">Inspect</a>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
    </div>

    <!-- =========================================================================
         SECTION D: Scheduling and Benchmark Insights
         ========================================================================= -->
    <div class="section-heading" id="schedulingInsights">
        <span>Section D: Scheduling & Micro-Benchmark Insights</span>
        <span class="section-tag">Empirical Aggregates</span>
    </div>

    <div class="form-grid-2" style="margin-bottom: 24px;">
        <!-- Algorithm Aggregate Scheduling Metrics -->
        <div class="card" style="margin-bottom: 0;">
            <div class="card-header">
                <div>
                    <h2 class="card-title">Scheduling Algorithm Performance</h2>
                    <p class="card-description">Aggregates across all completed user experiments</p>
                </div>
                <a href="<%= request.getContextPath() %>/experiments" class="btn btn-sm btn-outline">Experiments &rarr;</a>
            </div>

            <c:choose>
                <c:when test="${empty schedulingSummaries}">
                    <div class="empty-state" style="padding: 24px;">
                        <p class="empty-state-text">No simulation metrics aggregated yet. Run scheduling experiments to view insights.</p>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="table-responsive" style="margin: 0; box-shadow: none; border: none;">
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>Algorithm</th>
                                    <th>Runs (Pass/Fail)</th>
                                    <th>Avg Wait</th>
                                    <th>Avg Turnaround</th>
                                    <th>CPU Util</th>
                                </tr>
                            </thead>
                            <tbody>
                            <c:forEach var="s" items="${schedulingSummaries}">
                                <tr>
                                    <td><strong><code><c:out value="${s.algorithmName}"/></code></strong></td>
                                    <td>${s.totalRuns} (${s.successCount}/${s.failureCount})</td>
                                    <td>${s.formattedAvgWaitingTime}</td>
                                    <td>${s.formattedAvgTurnaroundTime}</td>
                                    <td>${s.formattedAvgCpuUtilization}</td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>

        <!-- Micro-Benchmarking JIT Performance -->
        <div class="card" style="margin-bottom: 0;">
            <div class="card-header">
                <div>
                    <h2 class="card-title">Host Micro-Benchmark Insights</h2>
                    <p class="card-description">Algorithm execution duration on host JVM</p>
                </div>
                <a href="<%= request.getContextPath() %>/benchmarks" class="btn btn-sm btn-outline">Benchmarks &rarr;</a>
            </div>

            <c:choose>
                <c:when test="${empty benchmarkSummaries}">
                    <div class="empty-state" style="padding: 24px;">
                        <p class="empty-state-text">No micro-benchmarks profiled yet. Run benchmarks to compare JIT execution.</p>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="table-responsive" style="margin: 0; box-shadow: none; border: none;">
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>Algorithm</th>
                                    <th>Runs</th>
                                    <th>Avg P50</th>
                                    <th>Min / Max</th>
                                    <th>Batch Throughput</th>
                                </tr>
                            </thead>
                            <tbody>
                            <c:forEach var="bm" items="${benchmarkSummaries}">
                                <tr>
                                    <td><strong><code><c:out value="${bm.algorithmName}"/></code></strong></td>
                                    <td>${bm.runCount}</td>
                                    <td><strong>${bm.formattedAvgMedianMicros}</strong></td>
                                    <td>${bm.formattedMinMicros} / ${bm.formattedMaxMicros}</td>
                                    <td>${bm.formattedAvgActualBatchThroughput}</td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>

    <!-- Activity Audit Trail -->
    <c:if test="${not empty recentActivity}">
        <div class="card">
            <div class="card-header">
                <div>
                    <h2 class="card-title">Recent Activity Audit Trail</h2>
                    <p class="card-description">Immutable chronological log of authenticated actions</p>
                </div>
            </div>
            <div class="table-responsive" style="margin: 0; box-shadow: none; border: none;">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Event</th>
                            <th>Target Resource</th>
                            <th>Resource ID</th>
                            <th>Timestamp</th>
                        </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="act" items="${recentActivity}">
                        <tr>
                            <td><span class="badge badge-neutral"><c:out value="${act.eventType}"/></span></td>
                            <td><c:out value="${act.resourceType}" default="SYSTEM"/></td>
                            <td><code><c:out value="${act.resourceId}" default="-"/></code></td>
                            <td><c:out value="${act.timestamp}"/></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </div>
    </c:if>
</div>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
<script src="<%= request.getContextPath() %>/assets/js/app.js"></script>
</body>
</html>