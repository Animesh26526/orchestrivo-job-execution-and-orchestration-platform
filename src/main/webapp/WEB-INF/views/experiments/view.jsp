<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Experiment Run #${experiment.id} - Orchestrivo</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/app.css">
</head>
<body>

<jsp:include page="/WEB-INF/views/layout/navbar.jsp"/>

<div class="app-container">
    <div class="breadcrumb-bar">
        <a href="<%= request.getContextPath() %>/dashboard">Dashboard</a>
        <span class="breadcrumb-separator">/</span>
        <a href="<%= request.getContextPath() %>/experiments">Experiments</a>
        <span class="breadcrumb-separator">/</span>
        <span>Run #${experiment.id}</span>
    </div>

    <div class="page-header">
        <div class="page-title-group">
            <h1>Experiment Run #${experiment.id}: <c:out value="${experiment.algorithmName}"/></h1>
            <p class="page-subtitle">Executed against Workload #${experiment.workloadId} &bull; <c:out value="${experiment.createdAt}"/></p>
        </div>
        <div class="page-actions">
            <a href="<%= request.getContextPath() %>/experiments" class="btn btn-secondary">&larr; Back to Experiments</a>
            <a href="<%= request.getContextPath() %>/workloads/view?id=${experiment.workloadId}" class="btn btn-secondary">Source Workload</a>
            <a href="<%= request.getContextPath() %>/experiments/export?id=${experiment.id}" class="btn btn-outline">Export Run CSV</a>
        </div>
    </div>

    <!-- Run Summary Card -->
    <div class="card">
        <div class="card-header">
            <h2 class="card-title">Run Specification & Status</h2>
            <div style="display: flex; gap: 8px;">
                <span class="badge ${experiment.runStatus == 'SUCCEEDED' ? 'badge-success' : 'badge-danger'}">
                    <c:out value="${experiment.runStatus}"/>
                </span>
                <span class="badge ${experiment.validationStatus == 'VALID' ? 'badge-success' : 'badge-danger'}">
                    <c:out value="${experiment.validationStatus}"/>
                </span>
            </div>
        </div>
        <div class="table-responsive" style="margin: 0; box-shadow: none; border: none;">
            <table class="data-table">
                <tbody>
                    <tr>
                        <td style="width: 200px; font-weight: 600;">Algorithm:</td>
                        <td><strong><c:out value="${experiment.algorithmName}"/></strong></td>
                    </tr>
                    <tr>
                        <td style="font-weight: 600;">Parameters:</td>
                        <td>
                            <c:choose>
                                <c:when test="${not empty experiment.formattedParameters}">
                                    <span style="font-weight: 500;"><c:out value="${experiment.formattedParameters}"/></span>
                                </c:when>
                                <c:otherwise>
                                    <span style="color: var(--color-slate-500);">None</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                    <tr>
                        <td style="font-weight: 600;">Validation Invariants:</td>
                        <td>
                            <c:choose>
                                <c:when test="${experiment.validationStatus == 'VALID'}">
                                    <span style="color: var(--color-success); font-weight: 500;">Passed all ScheduleValidator CPU invariant checks (No overlaps, pre-arrival integrity verified).</span>
                                </c:when>
                                <c:otherwise>
                                    <span style="color: var(--color-danger); font-weight: 500;">Failed invariant verification: <c:out value="${experiment.errorMessage}"/></span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                    <tr>
                        <td style="font-weight: 600;">Execution Timestamp:</td>
                        <td><c:out value="${experiment.createdAt}"/></td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>

    <c:if test="${not empty experiment.errorMessage}">
        <div class="alert alert-danger">
            <strong>Error Details:</strong> <c:out value="${experiment.errorMessage}"/>
        </div>
    </c:if>

    <c:if test="${experiment.runStatus == 'SUCCEEDED' && experiment.result != null}">
        <!-- Aggregate Metrics Grid -->
        <h2 style="font-size: 18px; font-weight: 600; margin-bottom: 12px; color: var(--color-slate-900);">Simulated Aggregate Scheduling Metrics</h2>
        <div class="metrics-grid">
            <div class="metric-card">
                <div class="metric-header">
                    <span class="metric-label">Avg Waiting Time</span>
                    <span class="badge badge-primary">Domain</span>
                </div>
                <div class="metric-value"><c:out value="${experiment.result.aggregateMetrics.formattedAverageWaitingTime}"/></div>
                <div class="metric-subtext">Time spent in ready queue</div>
            </div>
            <div class="metric-card">
                <div class="metric-header">
                    <span class="metric-label">Avg Turnaround Time</span>
                    <span class="badge badge-primary">Domain</span>
                </div>
                <div class="metric-value"><c:out value="${experiment.result.aggregateMetrics.formattedAverageTurnaroundTime}"/></div>
                <div class="metric-subtext">Arrival to completion duration</div>
            </div>
            <div class="metric-card">
                <div class="metric-header">
                    <span class="metric-label">Avg Response Time</span>
                    <span class="badge badge-primary">Domain</span>
                </div>
                <div class="metric-value"><c:out value="${experiment.result.aggregateMetrics.formattedAverageResponseTime}"/></div>
                <div class="metric-subtext">Arrival to initial CPU acquisition</div>
            </div>
            <div class="metric-card">
                <div class="metric-header">
                    <span class="metric-label">Context Switches</span>
                    <span class="badge badge-warning">Overhead</span>
                </div>
                <div class="metric-value"><c:out value="${experiment.result.aggregateMetrics.contextSwitchCount}"/></div>
                <div class="metric-subtext">Process preemptions / handoffs</div>
            </div>
            <div class="metric-card">
                <div class="metric-header">
                    <span class="metric-label">Total Duration</span>
                    <span class="badge badge-neutral">Timeline</span>
                </div>
                <div class="metric-value"><c:out value="${experiment.result.aggregateMetrics.totalDuration}"/></div>
                <div class="metric-subtext">Simulated scheduling clock ticks</div>
            </div>
            <div class="metric-card">
                <div class="metric-header">
                    <span class="metric-label">Throughput</span>
                    <span class="badge badge-success">Efficiency</span>
                </div>
                <div class="metric-value"><c:out value="${experiment.result.aggregateMetrics.formattedThroughput}"/></div>
                <div class="metric-subtext">Completed processes per unit time</div>
            </div>
            <div class="metric-card">
                <div class="metric-header">
                    <span class="metric-label">CPU Busy Time</span>
                    <span class="badge badge-success">Active</span>
                </div>
                <div class="metric-value"><c:out value="${experiment.result.aggregateMetrics.cpuBusyTime}"/></div>
                <div class="metric-subtext">Total active execution units</div>
            </div>
            <div class="metric-card">
                <div class="metric-header">
                    <span class="metric-label">CPU Idle Time</span>
                    <span class="badge badge-neutral">Idle</span>
                </div>
                <div class="metric-value"><c:out value="${experiment.result.aggregateMetrics.cpuIdleTime}"/></div>
                <div class="metric-subtext">Time units waiting for arrival</div>
            </div>
        </div>

        <div class="alert alert-info" style="margin-bottom: 24px;">
            <div>
                <strong>Domain vs Execution Latency Distinction:</strong>
                The numbers above reflect simulated operating system clock ticks according to the theoretical CPU scheduling model. They are strictly separate from host Java runtime nanoseconds/milliseconds measured by the execution engine.
            </div>
        </div>

        <!-- Gantt Chart Section -->
        <div class="gantt-wrapper">
            <div class="card-header" style="border: none; padding: 0; margin-bottom: 16px;">
                <div>
                    <h2 class="card-title">Gantt Chart Execution Timeline</h2>
                    <p class="card-description">Deterministic color-coded process allocation across simulated CPU timeline</p>
                </div>
            </div>

            <c:if test="${experiment.result.aggregateMetrics.totalDuration > 0}">
                <div class="gantt-scroll-container">
                    <div class="gantt-bar-container">
                        <c:set var="colors" value="${['#2563eb', '#059669', '#d97706', '#7c3aed', '#db2777', '#0891b2', '#4b5563', '#ea580c']}"/>
                        <c:forEach var="seg" items="${experiment.result.executionTimeline}" varStatus="loop">
                            <c:set var="segDuration" value="${seg.endTime - seg.startTime}"/>
                            <c:set var="pct" value="${(segDuration * 100.0) / experiment.result.aggregateMetrics.totalDuration}"/>
                            <c:set var="colorIdx" value="${loop.index % 8}"/>
                            <div class="gantt-block ${seg.processId == 'IDLE' ? 'idle' : ''}"
                                 style="width: ${pct}%; <c:if test="${seg.processId != 'IDLE'}">background-color: ${colors[colorIdx]};</c:if>"
                                 title="<c:out value='${seg.processId}'/>: [${seg.startTime} &rarr; ${seg.endTime}] (${segDuration} units)">
                                <c:out value="${seg.processId}"/>
                            </div>
                        </c:forEach>
                    </div>
                    <div class="gantt-axis">
                        <span>t = 0</span>
                        <span>t = ${experiment.result.aggregateMetrics.totalDuration / 2}</span>
                        <span>t = ${experiment.result.aggregateMetrics.totalDuration} (Total Duration)</span>
                    </div>
                </div>
            </c:if>

            <div class="gantt-legend">
                <div class="gantt-legend-item">
                    <div class="gantt-legend-color" style="background-color: var(--color-slate-400);"></div>
                    <span>IDLE (CPU waiting for ready processes)</span>
                </div>
                <div class="gantt-legend-item">
                    <div class="gantt-legend-color" style="background-color: #2563eb;"></div>
                    <span>Active Process Segments</span>
                </div>
            </div>
        </div>

        <!-- Tabular Execution Segments Alternative (Accessibility) -->
        <div class="card">
            <div class="card-header">
                <div>
                    <h2 class="card-title">Execution Timeline Segments (Tabular Alternative)</h2>
                    <p class="card-description">Screen-reader and tabular breakdown of consecutive schedule intervals</p>
                </div>
            </div>
            <div class="table-responsive" style="margin: 0; box-shadow: none; border: none; max-height: 320px; overflow-y: auto;">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Sequence</th>
                            <th>Process ID</th>
                            <th>Start Time</th>
                            <th>End Time</th>
                            <th>Segment Duration</th>
                        </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="seg" items="${experiment.result.executionTimeline}" varStatus="status">
                        <tr>
                            <td>${status.count}</td>
                            <td>
                                <strong><code><c:out value="${seg.processId}"/></code></strong>
                            </td>
                            <td>${seg.startTime}</td>
                            <td>${seg.endTime}</td>
                            <td>${seg.endTime - seg.startTime}</td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </div>

        <!-- Per-Process Metrics Table -->
        <div class="card">
            <div class="card-header">
                <div>
                    <h2 class="card-title">Per-Process Performance Metrics</h2>
                    <p class="card-description">Individual process turnaround, response, and waiting telemetry</p>
                </div>
            </div>
            <div class="table-responsive" style="margin: 0; box-shadow: none; border: none;">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Process ID</th>
                            <th>First Start</th>
                            <th>Completion</th>
                            <th>Waiting Time</th>
                            <th>Turnaround Time</th>
                            <th>Response Time</th>
                        </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="pm" items="${experiment.result.processMetrics}">
                        <tr>
                            <td><strong><code><c:out value="${pm.processId}"/></code></strong></td>
                            <td>${pm.firstStartTime}</td>
                            <td>${pm.completionTime}</td>
                            <td><strong>${pm.waitingTime}</strong></td>
                            <td><strong>${pm.turnaroundTime}</strong></td>
                            <td>${pm.responseTime}</td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </div>
    </c:if>

    <!-- Immutable Snapshot Table -->
    <div class="card">
        <div class="card-header">
            <div>
                <h2 class="card-title">Immutable Workload Input Snapshot</h2>
                <p class="card-description">Exact processes captured at simulation execution time to guarantee historical reproducibility</p>
            </div>
        </div>
        <div class="table-responsive" style="margin: 0; box-shadow: none; border: none;">
            <table class="data-table">
                <thead>
                    <tr>
                        <th>Process ID</th>
                        <th>Arrival Time</th>
                        <th>Burst Time</th>
                    </tr>
                </thead>
                <tbody>
                <c:forEach var="p" items="${experiment.snapshotProcesses}">
                    <tr>
                        <td><strong><code><c:out value="${p.processId}"/></code></strong></td>
                        <td>${p.arrivalTime}</td>
                        <td>${p.burstTime}</td>
                    </tr>
                </c:forEach>
                <c:if test="${empty experiment.snapshotProcesses}">
                    <tr>
                        <td colspan="3" style="text-align: center; color: #94a3b8; padding: 20px;">No input snapshot processes recorded for this historical run.</td>
                    </tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </div>
</div>

<script src="<%= request.getContextPath() %>/assets/js/app.js"></script>
</body>
</html>