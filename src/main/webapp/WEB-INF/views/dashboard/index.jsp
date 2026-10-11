<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard - Orchestrivo</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/app.css">
</head>
<body>

<jsp:include page="/WEB-INF/views/layout/navbar.jsp"/>

<div class="app-container">
    <div class="page-header">
        <div class="page-title-group">
            <h1>Engineering Dashboard</h1>
            <p class="page-subtitle">Welcome back, <c:out value="${sessionScope.user.email}"/> &mdash; Orchestrivo CPU scheduling & execution telemetry</p>
        </div>
        <div class="page-actions">
            <a href="<%= request.getContextPath() %>/workloads/new" class="btn btn-secondary">+ New Workload</a>
            <a href="<%= request.getContextPath() %>/workloads/import" class="btn btn-secondary">Import CSV</a>
            <a href="<%= request.getContextPath() %>/workloads/generate" class="btn btn-secondary">Generate Seeded</a>
            <a href="<%= request.getContextPath() %>/benchmarks/run" class="btn btn-primary">+ Run Benchmark</a>
        </div>
    </div>

    <!-- Summary Metrics Grid -->
    <div class="metrics-grid">
        <div class="metric-card">
            <div class="metric-header">
                <span class="metric-label">Total Workloads</span>
                <span class="badge badge-primary">Catalog</span>
            </div>
            <div class="metric-value"><c:out value="${workloads != null ? workloads.size() : 0}"/></div>
            <div class="metric-subtext">Configured process datasets</div>
        </div>

        <div class="metric-card">
            <div class="metric-header">
                <span class="metric-label">Completed Experiments</span>
                <span class="badge badge-success">Telemetry</span>
            </div>
            <div class="metric-value"><c:out value="${experiments != null ? experiments.size() : 0}"/></div>
            <div class="metric-subtext">Historical simulation executions</div>
        </div>

        <div class="metric-card">
            <div class="metric-header">
                <span class="metric-label">Active Queue</span>
                <span class="badge ${queueMetrics != null && queueMetrics.runningCount > 0 ? 'badge-primary' : (queueMetrics != null && queueMetrics.queuedCount > 0 ? 'badge-warning' : 'badge-neutral')}">Engine</span>
            </div>
            <div class="metric-value">
                <c:choose>
                    <c:when test="${queueMetrics != null}">
                        ${queueMetrics.queuedCount + queueMetrics.runningCount}
                    </c:when>
                    <c:otherwise>0</c:otherwise>
                </c:choose>
            </div>
            <div class="metric-subtext">
                <c:choose>
                    <c:when test="${queueMetrics != null}">
                        ${queueMetrics.queuedCount} queued &bull; ${queueMetrics.runningCount} running
                    </c:when>
                    <c:otherwise>Queue engine idle</c:otherwise>
                </c:choose>
            </div>
        </div>

        <div class="metric-card">
            <div class="metric-header">
                <span class="metric-label">Micro-Benchmarks</span>
                <span class="badge badge-info">Profiling</span>
            </div>
            <div class="metric-value"><c:out value="${benchmarks != null ? benchmarks.size() : 0}"/></div>
            <div class="metric-subtext">Host JIT execution benchmarks</div>
        </div>
    </div>

    <div class="form-grid-2" style="margin-bottom: 24px;">
        <!-- Recent Experiments -->
        <div class="card" style="margin-bottom: 0;">
            <div class="card-header">
                <div>
                    <h2 class="card-title">Recent Simulation Runs</h2>
                    <p class="card-description">Latest algorithm executions and metrics</p>
                </div>
                <a href="<%= request.getContextPath() %>/experiments" class="btn btn-sm btn-outline">View All &rarr;</a>
            </div>

            <c:choose>
                <c:when test="${empty experiments}">
                    <div class="empty-state" style="padding: 24px;">
                        <p class="empty-state-text">No experiment runs recorded yet. Launch an experiment from your workloads.</p>
                        <a href="<%= request.getContextPath() %>/workloads" class="btn btn-sm btn-primary">Go to Workloads</a>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="table-responsive" style="margin: 0; box-shadow: none; border: none;">
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>Run</th>
                                    <th>Algorithm</th>
                                    <th>Status</th>
                                    <th>Validation</th>
                                    <th>Avg Wait</th>
                                    <th style="text-align: right;">Action</th>
                                </tr>
                            </thead>
                            <tbody>
                            <c:forEach var="e" items="${experiments}" end="4">
                                <tr>
                                    <td><strong><a href="<%= request.getContextPath() %>/experiments/view?id=${e.id}">#${e.id}</a></strong></td>
                                    <td><c:out value="${e.algorithmName}"/></td>
                                    <td>
                                        <span class="badge ${e.runStatus == 'SUCCEEDED' ? 'badge-success' : 'badge-danger'}">
                                            <c:out value="${e.runStatus}"/>
                                        </span>
                                    </td>
                                    <td>
                                        <span class="badge ${e.validationStatus == 'VALID' ? 'badge-success' : 'badge-danger'}">
                                            <c:out value="${e.validationStatus}"/>
                                        </span>
                                    </td>
                                    <td>${e.result != null && e.result.aggregateMetrics != null ? e.result.aggregateMetrics.averageWaitingTime : '-'}</td>
                                    <td class="actions-cell">
                                        <a href="<%= request.getContextPath() %>/experiments/view?id=${e.id}" class="btn btn-sm btn-secondary">View</a>
                                    </td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>

        <!-- Recent Background Jobs -->
        <div class="card" style="margin-bottom: 0;">
            <div class="card-header">
                <div>
                    <h2 class="card-title">Background Execution Jobs</h2>
                    <p class="card-description">Asynchronous queue processing history</p>
                </div>
                <a href="<%= request.getContextPath() %>/jobs" class="btn btn-sm btn-outline">View Queue &rarr;</a>
            </div>

            <c:choose>
                <c:when test="${empty recentJobs}">
                    <div class="empty-state" style="padding: 24px;">
                        <p class="empty-state-text">No background jobs currently in queue.</p>
                        <a href="<%= request.getContextPath() %>/workloads" class="btn btn-sm btn-primary">Run Experiment</a>
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
                                    <th>Exec Time</th>
                                    <th style="text-align: right;">Action</th>
                                </tr>
                            </thead>
                            <tbody>
                            <c:forEach var="j" items="${recentJobs}">
                                <tr>
                                    <td><strong><a href="<%= request.getContextPath() %>/jobs/view?id=${j.id}">#${j.id}</a></strong></td>
                                    <td><c:out value="${j.algorithmName}"/></td>
                                    <td>
                                        <span class="badge ${j.status == 'COMPLETED' ? 'badge-success' : (j.status == 'RUNNING' ? 'badge-primary' : (j.status == 'QUEUED' ? 'badge-warning' : 'badge-danger'))}">
                                            <c:out value="${j.status}"/>
                                        </span>
                                    </td>
                                    <td>${j.executionDurationMs != null ? j.executionDurationMs : 0} ms</td>
                                    <td class="actions-cell">
                                        <a href="<%= request.getContextPath() %>/jobs/view?id=${j.id}" class="btn btn-sm btn-secondary">Status</a>
                                    </td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>

    <!-- Recent Audit Logs -->
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

<script src="<%= request.getContextPath() %>/assets/js/app.js"></script>
</body>
</html>