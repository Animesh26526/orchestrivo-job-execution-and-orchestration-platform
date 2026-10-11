<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Execution Queue & Jobs - Orchestrivo</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/app.css">
</head>
<body>

<jsp:include page="/WEB-INF/views/layout/navbar.jsp"/>

<div class="app-container">
    <div class="breadcrumb-bar">
        <a href="<%= request.getContextPath() %>/dashboard">Dashboard</a>
        <span class="breadcrumb-separator">/</span>
        <span>Queue & Background Jobs</span>
    </div>

    <div class="page-header">
        <div class="page-title-group">
            <h1>Execution Queue & Background Jobs</h1>
            <p class="page-subtitle">Persistent MySQL-backed asynchronous job engine and real-time execution worker status</p>
        </div>
        <div class="page-actions">
            <a href="<%= request.getContextPath() %>/workloads" class="btn btn-primary">+ Run Experiment from Workload</a>
        </div>
    </div>

    <c:if test="${not empty metrics}">
        <div class="metrics-grid">
            <div class="metric-card">
                <div class="metric-header">
                    <span class="metric-label">Queued Jobs</span>
                    <span class="badge badge-warning">Waiting</span>
                </div>
                <div class="metric-value">${metrics.queuedCount}</div>
                <div class="metric-subtext">Pending background execution</div>
            </div>
            <div class="metric-card">
                <div class="metric-header">
                    <span class="metric-label">Running Jobs</span>
                    <span class="badge badge-primary">Active</span>
                </div>
                <div class="metric-value">${metrics.runningCount}</div>
                <div class="metric-subtext">Currently executing on workers</div>
            </div>
            <div class="metric-card">
                <div class="metric-header">
                    <span class="metric-label">Completed</span>
                    <span class="badge badge-success">Succeeded</span>
                </div>
                <div class="metric-value">${metrics.completedCount}</div>
                <div class="metric-subtext">Successfully persisted results</div>
            </div>
            <div class="metric-card">
                <div class="metric-header">
                    <span class="metric-label">Failed</span>
                    <span class="badge badge-danger">Errors</span>
                </div>
                <div class="metric-value">${metrics.failedCount}</div>
                <div class="metric-subtext">Transient or permanent failures</div>
            </div>
            <div class="metric-card">
                <div class="metric-header">
                    <span class="metric-label">Avg Queue Wait</span>
                    <span class="badge badge-neutral">Latency</span>
                </div>
                <div class="metric-value"><c:out value="${metrics.formattedAvgQueueWaitMs}"/></div>
                <div class="metric-subtext">Time in queue prior to worker claim</div>
            </div>
            <div class="metric-card">
                <div class="metric-header">
                    <span class="metric-label">Avg Engine Exec</span>
                    <span class="badge badge-neutral">Latency</span>
                </div>
                <div class="metric-value"><c:out value="${metrics.formattedAvgExecutionDurationMs}"/></div>
                <div class="metric-subtext">Wall-clock algorithm computation duration</div>
            </div>
        </div>
    </c:if>

    <div class="filter-bar">
        <form action="<%= request.getContextPath() %>/jobs" method="get" class="filter-form">
            <div>
                <label class="form-label" style="font-size: 11px;">Status Filter:</label>
                <select name="status" class="form-select" style="min-width: 140px;">
                    <option value="ALL" ${selectedStatus == 'ALL' ? 'selected' : ''}>All Statuses</option>
                    <option value="QUEUED" ${selectedStatus == 'QUEUED' ? 'selected' : ''}>QUEUED</option>
                    <option value="RUNNING" ${selectedStatus == 'RUNNING' ? 'selected' : ''}>RUNNING</option>
                    <option value="COMPLETED" ${selectedStatus == 'COMPLETED' ? 'selected' : ''}>COMPLETED</option>
                    <option value="FAILED" ${selectedStatus == 'FAILED' ? 'selected' : ''}>FAILED</option>
                    <option value="CANCELLED" ${selectedStatus == 'CANCELLED' ? 'selected' : ''}>CANCELLED</option>
                </select>
            </div>
            <div>
                <label class="form-label" style="font-size: 11px;">Sort By:</label>
                <select name="sortBy" class="form-select" style="min-width: 140px;">
                    <option value="created_at" ${sortBy == 'created_at' ? 'selected' : ''}>Submission Time</option>
                    <option value="id" ${sortBy == 'id' ? 'selected' : ''}>Job ID</option>
                    <option value="status" ${sortBy == 'status' ? 'selected' : ''}>Status</option>
                    <option value="execution_duration_ms" ${sortBy == 'execution_duration_ms' ? 'selected' : ''}>Execution Duration</option>
                </select>
            </div>
            <div>
                <label class="form-label" style="font-size: 11px;">Direction:</label>
                <select name="sortDir" class="form-select" style="min-width: 120px;">
                    <option value="DESC" ${sortDir == 'DESC' ? 'selected' : ''}>Descending</option>
                    <option value="ASC" ${sortDir == 'ASC' ? 'selected' : ''}>Ascending</option>
                </select>
            </div>
            <div style="margin-top: 18px;">
                <button type="submit" class="btn btn-secondary">Apply Filter</button>
                <c:if test="${selectedStatus != 'ALL'}">
                    <a href="<%= request.getContextPath() %>/jobs" class="btn btn-outline" style="margin-left: 6px;">Reset</a>
                </c:if>
            </div>
        </form>
    </div>

    <c:choose>
        <c:when test="${empty jobs}">
            <div class="empty-state">
                <div class="empty-state-icon">📋</div>
                <h3 class="empty-state-title">No background execution jobs found</h3>
                <p class="empty-state-text">Submit an experiment run from your workloads to dispatch background asynchronous scheduling jobs.</p>
                <a href="<%= request.getContextPath() %>/workloads" class="btn btn-primary">Go to Workloads</a>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-responsive">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Job ID</th>
                            <th>Workload</th>
                            <th>Algorithm</th>
                            <th>Status</th>
                            <th>Attempts</th>
                            <th>Enqueued At</th>
                            <th>Queue Wait</th>
                            <th>Exec Time</th>
                            <th>Result</th>
                            <th style="text-align: right;">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="j" items="${jobs}">
                        <tr>
                            <td>
                                <strong><a href="<%= request.getContextPath() %>/jobs/view?id=${j.id}">#${j.id}</a></strong>
                            </td>
                            <td>
                                <a href="<%= request.getContextPath() %>/workloads/view?id=${j.workloadId}">
                                    <c:out value="${j.workloadName}" default="Workload #${j.workloadId}"/>
                                </a>
                            </td>
                            <td><strong><c:out value="${j.algorithmName}"/></strong></td>
                            <td>
                                <span class="badge ${j.status == 'COMPLETED' ? 'badge-success' : (j.status == 'RUNNING' ? 'badge-primary' : (j.status == 'QUEUED' ? 'badge-warning' : (j.status == 'FAILED' ? 'badge-danger' : 'badge-neutral')))}">
                                    <c:out value="${j.status}"/>
                                </span>
                            </td>
                            <td>${j.attemptCount} / ${j.maxAttempts}</td>
                            <td><c:out value="${j.createdAt}"/></td>
                            <td>
                                <c:choose>
                                    <c:when test="${j.queueWaitMs != null}">${j.queueWaitMs} ms</c:when>
                                    <c:otherwise><span style="color:#94a3b8;">-</span></c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                <c:choose>
                                    <c:when test="${j.executionDurationMs != null}">${j.executionDurationMs} ms</c:when>
                                    <c:otherwise><span style="color:#94a3b8;">-</span></c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                <c:choose>
                                    <c:when test="${j.experimentRunId != null}">
                                        <a href="<%= request.getContextPath() %>/experiments/view?id=${j.experimentRunId}" class="btn btn-sm btn-outline">
                                            Run #${j.experimentRunId} &rarr;
                                        </a>
                                    </c:when>
                                    <c:when test="${not empty j.errorMessage}">
                                        <span class="badge badge-danger" title="<c:out value='${j.errorMessage}'/>">Error</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span style="color:#94a3b8; font-size:12px;">In progress</span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td class="actions-cell">
                                <div class="table-actions-group">
                                    <a href="<%= request.getContextPath() %>/jobs/view?id=${j.id}" class="btn btn-sm btn-secondary">Details</a>
                                    <c:if test="${j.eligibleForRetry}">
                                        <form action="<%= request.getContextPath() %>/jobs/retry" method="post" style="display:inline;">
                                            <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}'/>" />
                                            <input type="hidden" name="id" value="${j.id}" />
                                            <button type="submit" class="btn btn-sm btn-primary" onclick="return confirm('Retry failed job #${j.id}?');">Retry</button>
                                        </form>
                                    </c:if>
                                    <c:if test="${j.eligibleForCancellation}">
                                        <form action="<%= request.getContextPath() %>/jobs/cancel" method="post" style="display:inline;">
                                            <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}'/>" />
                                            <input type="hidden" name="id" value="${j.id}" />
                                            <button type="submit" class="btn btn-sm btn-outline-danger" onclick="return confirm('Cancel queued job #${j.id}?');">Cancel</button>
                                        </form>
                                    </c:if>
                                </div>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>

            <c:if test="${pagedResult.totalPages > 1}">
                <div class="pagination-container">
                    <div>Showing page <strong>${pagedResult.page}</strong> of <strong>${pagedResult.totalPages}</strong> (${pagedResult.totalCount} total jobs)</div>
                    <div class="pagination-links">
                        <c:if test="${pagedResult.hasPrevious()}">
                            <a href="<%= request.getContextPath() %>/jobs?page=${pagedResult.page - 1}&status=<c:out value='${selectedStatus}'/>&sortBy=<c:out value='${sortBy}'/>&sortDir=<c:out value='${sortDir}'/>" class="page-btn">&laquo; Previous</a>
                        </c:if>
                        <c:if test="${pagedResult.hasNext()}">
                            <a href="<%= request.getContextPath() %>/jobs?page=${pagedResult.page + 1}&status=<c:out value='${selectedStatus}'/>&sortBy=<c:out value='${sortBy}'/>&sortDir=<c:out value='${sortDir}'/>" class="page-btn">Next &raquo;</a>
                        </c:if>
                    </div>
                </div>
            </c:if>
        </c:otherwise>
    </c:choose>
</div>

<script src="<%= request.getContextPath() %>/assets/js/app.js"></script>
</body>
</html>
