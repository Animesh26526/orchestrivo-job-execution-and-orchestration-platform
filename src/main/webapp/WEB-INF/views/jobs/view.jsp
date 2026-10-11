<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Job #${job.id} Status - Orchestrivo</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/app.css">
</head>
<body>

<jsp:include page="/WEB-INF/views/layout/navbar.jsp"/>

<div class="app-container">
    <div class="breadcrumb-bar">
        <a href="<%= request.getContextPath() %>/dashboard">Dashboard</a>
        <span class="breadcrumb-separator">/</span>
        <a href="<%= request.getContextPath() %>/jobs">Queue & Jobs</a>
        <span class="breadcrumb-separator">/</span>
        <span>Job #${job.id}</span>
    </div>

    <div class="page-header">
        <div class="page-title-group">
            <h1>Execution Job #${job.id}</h1>
            <p class="page-subtitle">Background simulation worker dispatch and persistence lifecycle</p>
        </div>
        <div class="page-actions">
            <a href="<%= request.getContextPath() %>/jobs" class="btn btn-secondary">&larr; Back to Queue</a>
            <c:if test="${job.experimentRunId != null}">
                <a href="<%= request.getContextPath() %>/experiments/view?id=${job.experimentRunId}" class="btn btn-primary">
                    View Experiment Results & Gantt Chart &rarr;
                </a>
            </c:if>
            <c:if test="${job.eligibleForRetry}">
                <form action="<%= request.getContextPath() %>/jobs/retry" method="post" style="display:inline;">
                    <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}'/>" />
                    <input type="hidden" name="id" value="${job.id}" />
                    <button type="submit" class="btn btn-primary" onclick="return confirm('Retry execution job #${job.id}?');">Retry Job</button>
                </form>
            </c:if>
            <c:if test="${job.eligibleForCancellation}">
                <form action="<%= request.getContextPath() %>/jobs/cancel" method="post" style="display:inline;">
                    <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}'/>" />
                    <input type="hidden" name="id" value="${job.id}" />
                    <button type="submit" class="btn btn-outline-danger" onclick="return confirm('Cancel queued job #${job.id}?');">Cancel Job</button>
                </form>
            </c:if>
        </div>
    </div>

    <div class="metrics-grid">
        <div class="metric-card">
            <div class="metric-header">
                <span class="metric-label">Execution Status</span>
            </div>
            <div style="margin-top: 4px;">
                <span id="jobStatusBadge" class="badge ${job.status == 'COMPLETED' ? 'badge-success' : (job.status == 'RUNNING' ? 'badge-primary' : (job.status == 'QUEUED' ? 'badge-warning' : (job.status == 'FAILED' ? 'badge-danger' : 'badge-neutral')))}" style="font-size: 14px; padding: 6px 12px;">
                    <c:out value="${job.status}"/>
                </span>
            </div>
            <div class="metric-subtext" id="jobProgressText">
                Attempt ${job.attemptCount} of ${job.maxAttempts}
            </div>
        </div>

        <div class="metric-card">
            <div class="metric-header">
                <span class="metric-label">Algorithm</span>
                <span class="badge badge-primary">Model</span>
            </div>
            <div class="metric-value" style="font-size: 20px;"><c:out value="${job.algorithmName}"/></div>
            <div class="metric-subtext">Parameters: <c:out value="${job.algorithmParameters}" default="None"/></div>
        </div>

        <div class="metric-card">
            <div class="metric-header">
                <span class="metric-label">Queue Wait Time</span>
                <span class="badge badge-neutral">Latency</span>
            </div>
            <div class="metric-value">
                <c:choose>
                    <c:when test="${job.queueWaitMs != null}">${job.queueWaitMs} ms</c:when>
                    <c:otherwise><span style="color:#94a3b8;">Pending</span></c:otherwise>
                </c:choose>
            </div>
            <div class="metric-subtext">Enqueued to worker pickup</div>
        </div>

        <div class="metric-card">
            <div class="metric-header">
                <span class="metric-label">Engine Computation</span>
                <span class="badge badge-neutral">Execution</span>
            </div>
            <div class="metric-value">
                <c:choose>
                    <c:when test="${job.executionDurationMs != null}">${job.executionDurationMs} ms</c:when>
                    <c:otherwise><span style="color:#94a3b8;">Pending</span></c:otherwise>
                </c:choose>
            </div>
            <div class="metric-subtext">Wall-clock algorithm runtime</div>
        </div>
    </div>

    <c:if test="${not empty job.errorMessage}">
        <div class="alert alert-danger">
            <div>
                <strong>Execution Error:</strong>
                <p style="margin-top: 4px;"><c:out value="${job.errorMessage}"/></p>
            </div>
        </div>
    </c:if>

    <div id="jobResultContainer" class="card" style="${job.status == 'COMPLETED' ? '' : 'display:none;'}">
        <div class="card-header">
            <div>
                <h2 class="card-title">Completed Simulation Outcome</h2>
                <p class="card-description">The background worker has finished execution and persisted the experiment run.</p>
            </div>
            <div>
                <a href="<%= request.getContextPath() %>/experiments/view?id=${job.experimentRunId}" class="btn btn-primary">
                    Open Experiment Run #${job.experimentRunId} &rarr;
                </a>
            </div>
        </div>
    </div>

    <div class="card">
        <div class="card-header">
            <h2 class="card-title">Job Execution Metadata</h2>
            <span class="badge badge-neutral">Lifecycle</span>
        </div>
        <div class="table-responsive" style="box-shadow: none; border: none; margin: 0;">
            <table class="data-table">
                <tbody>
                    <tr>
                        <td style="width: 220px; font-weight: 600;">Workload:</td>
                        <td>
                            <a href="<%= request.getContextPath() %>/workloads/view?id=${job.workloadId}">
                                <c:out value="${job.workloadName}" default="Workload #${job.workloadId}"/>
                            </a>
                        </td>
                    </tr>
                    <tr>
                        <td style="font-weight: 600;">Enqueued At:</td>
                        <td><c:out value="${job.createdAt}"/></td>
                    </tr>
                    <tr>
                        <td style="font-weight: 600;">Started At:</td>
                        <td><c:out value="${job.startedAt}" default="Not started yet"/></td>
                    </tr>
                    <tr>
                        <td style="font-weight: 600;">Completed At:</td>
                        <td><c:out value="${job.completedAt}" default="In progress"/></td>
                    </tr>
                    <tr>
                        <td style="font-weight: 600;">Claimed Worker:</td>
                        <td><code><c:out value="${job.claimedBy}" default="None"/></code></td>
                    </tr>
                    <c:if test="${not empty job.idempotencyKey}">
                        <tr>
                            <td style="font-weight: 600;">Idempotency Key:</td>
                            <td><code><c:out value="${job.idempotencyKey}"/></code></td>
                        </tr>
                    </c:if>
                </tbody>
            </table>
        </div>
    </div>

    <div class="card">
        <div class="card-header">
            <div>
                <h2 class="card-title">Immutable Workload Input Snapshot</h2>
                <p class="card-description">Exact process specification captured at submission time to ensure reproducibility.</p>
            </div>
        </div>
        <div class="code-box" style="max-height: 250px; overflow-y: auto;">
            <c:out value="${job.inputSnapshot}"/>
        </div>
    </div>
</div>

<script src="<%= request.getContextPath() %>/assets/js/app.js"></script>
<c:if test="${job.status == 'QUEUED' || job.status == 'RUNNING'}">
    <script>
        startJobStatusPolling('${job.id}', '<%= request.getContextPath() %>');
    </script>
</c:if>
</body>
</html>
