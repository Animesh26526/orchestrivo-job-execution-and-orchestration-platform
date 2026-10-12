<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>My Experiments - Orchestrivo</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/app.css">
</head>
<body>

<jsp:include page="/WEB-INF/views/layout/navbar.jsp"/>

<div class="app-container">
    <div class="breadcrumb-bar">
        <a href="<%= request.getContextPath() %>/dashboard">Dashboard</a>
        <span class="breadcrumb-separator">/</span>
        <span>Experiments</span>
    </div>

    <div class="page-header">
        <div class="page-title-group">
            <h1>Experiment Execution History</h1>
            <p class="page-subtitle">Historical simulation telemetry, immutable snapshots, and multi-algorithm evaluation</p>
        </div>
        <div class="page-actions">
            <a href="<%= request.getContextPath() %>/workloads" class="btn btn-primary">+ Run Experiment from Workload</a>
        </div>
    </div>

    <!-- Filter Bar -->
    <div class="filter-bar">
        <form action="<%= request.getContextPath() %>/experiments" method="get" class="filter-form">
            <div>
                <label class="form-label" style="font-size: 11px;">Workload Filter:</label>
                <select name="workloadId" class="form-select" style="min-width: 180px;">
                    <option value="">-- All Workloads --</option>
                    <c:forEach var="w" items="${workloads}">
                        <option value="${w.id}" ${selectedWorkloadId == w.id ? 'selected' : ''}><c:out value="${w.name}"/> (#${w.id})</option>
                    </c:forEach>
                </select>
            </div>
            <div>
                <label class="form-label" style="font-size: 11px;">Algorithm:</label>
                <select name="algorithm" class="form-select" style="min-width: 140px;">
                    <option value="">-- All Algorithms --</option>
                    <option value="FCFS" ${selectedAlgorithm == 'FCFS' ? 'selected' : ''}>FCFS</option>
                    <option value="SJF" ${selectedAlgorithm == 'SJF' ? 'selected' : ''}>SJF</option>
                    <option value="SRTF" ${selectedAlgorithm == 'SRTF' ? 'selected' : ''}>SRTF</option>
                    <option value="ROUND_ROBIN" ${selectedAlgorithm == 'ROUND_ROBIN' ? 'selected' : ''}>Round Robin</option>
                </select>
            </div>
            <div>
                <label class="form-label" style="font-size: 11px;">Sort By:</label>
                <select name="sortBy" class="form-select" style="min-width: 140px;">
                    <option value="created_at" ${sortBy == 'created_at' ? 'selected' : ''}>Date Executed</option>
                    <option value="id" ${sortBy == 'id' ? 'selected' : ''}>Run ID</option>
                    <option value="total_duration" ${sortBy == 'total_duration' ? 'selected' : ''}>Total Duration</option>
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
                <button type="submit" class="btn btn-secondary">Apply Filters</button>
                <a href="<%= request.getContextPath() %>/experiments" class="btn btn-outline" style="margin-left: 6px;">Reset</a>
            </div>
        </form>
    </div>

    <c:choose>
        <c:when test="${empty experiments}">
            <div class="empty-state">
                <div class="empty-state-icon">🔬</div>
                <h3 class="empty-state-title">No experiments recorded</h3>
                <p class="empty-state-text">Select a workload from your catalog to execute CPU scheduling simulations with FCFS, SJF, SRTF, or Round Robin algorithms.</p>
                <a href="<%= request.getContextPath() %>/workloads" class="btn btn-primary">Go to Workloads</a>
            </div>
        </c:when>
        <c:otherwise>
            <form action="<%= request.getContextPath() %>/experiments/compare" method="get">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
                    <div>
                        <button type="submit" class="btn btn-secondary">Compare Selected Runs Side-by-Side</button>
                    </div>
                    <div style="font-size: 12px; color: var(--color-slate-500);">
                        Select 2 to 10 experiments to generate a comparative analysis matrix
                    </div>
                </div>

                <div class="table-responsive">
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th style="width: 40px; text-align: center;">Select</th>
                                <th>Run ID</th>
                                <th>Workload</th>
                                <th>Algorithm</th>
                                <th>Parameters</th>
                                <th>Run Status</th>
                                <th>Validation</th>
                                <th>Avg Wait</th>
                                <th>Avg Turnaround</th>
                                <th>Duration</th>
                                <th style="text-align: right;">Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="e" items="${experiments}">
                            <tr>
                                <td style="text-align: center;">
                                    <input type="checkbox" name="runId" value="${e.id}"/>
                                </td>
                                <td><strong><a href="<%= request.getContextPath() %>/experiments/view?id=${e.id}">#${e.id}</a></strong></td>
                                <td>
                                    <a href="<%= request.getContextPath() %>/workloads/view?id=${e.workloadId}">
                                        Workload #${e.workloadId}
                                    </a>
                                </td>
                                <td><strong><c:out value="${e.algorithmName}"/></strong></td>
                                <td><small style="color: var(--color-slate-500);"><c:out value="${e.algorithmParameters}" default="-"/></small></td>
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
                                <td><c:out value="${e.result != null && e.result.aggregateMetrics != null ? e.result.aggregateMetrics.averageWaitingTime : '-'}"/></td>
                                <td><c:out value="${e.result != null && e.result.aggregateMetrics != null ? e.result.aggregateMetrics.averageTurnaroundTime : '-'}"/></td>
                                <td><c:out value="${e.result != null && e.result.aggregateMetrics != null ? e.result.aggregateMetrics.totalDuration : '-'}"/></td>
                                <td class="actions-cell">
                                    <div class="table-actions-group">
                                        <a href="<%= request.getContextPath() %>/experiments/view?id=${e.id}" class="btn btn-sm btn-secondary">View / Gantt</a>
                                        <a href="<%= request.getContextPath() %>/experiments/export?id=${e.id}" class="btn btn-sm btn-outline">CSV</a>
                                        <form action="<%= request.getContextPath() %>/experiments/delete" method="post" style="display:inline;">
                                            <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}'/>" />
                                            <input type="hidden" name="id" value="${e.id}" />
                                            <button type="submit" class="btn btn-sm btn-outline-danger" onclick="return confirm('Delete experiment run #${e.id}?');">Delete</button>
                                        </form>
                                    </div>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
            </form>

            <c:if test="${pagedResult.totalPages > 1}">
                <div class="pagination-container">
                    <div>Showing page <strong>${pagedResult.page}</strong> of <strong>${pagedResult.totalPages}</strong> (${pagedResult.totalCount} total runs)</div>
                    <div class="pagination-links">
                        <c:if test="${pagedResult.hasPrevious()}">
                            <a href="<%= request.getContextPath() %>/experiments?page=${pagedResult.page - 1}&workloadId=${selectedWorkloadId}&algorithm=<c:out value='${selectedAlgorithm}'/>&sortBy=<c:out value='${sortBy}'/>&sortDir=<c:out value='${sortDir}'/>" class="page-btn">&laquo; Previous</a>
                        </c:if>
                        <c:if test="${pagedResult.hasNext()}">
                            <a href="<%= request.getContextPath() %>/experiments?page=${pagedResult.page + 1}&workloadId=${selectedWorkloadId}&algorithm=<c:out value='${selectedAlgorithm}'/>&sortBy=<c:out value='${sortBy}'/>&sortDir=<c:out value='${sortDir}'/>" class="page-btn">Next &raquo;</a>
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