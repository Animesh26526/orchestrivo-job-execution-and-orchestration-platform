<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Multi-Algorithm Comparison - Orchestrivo</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/app.css">
    <style>
        .highlight-winner {
            background-color: var(--color-success-bg) !important;
            color: var(--color-success) !important;
            font-weight: 700;
        }
        .tag-winner {
            font-size: 10px;
            background-color: var(--color-success);
            color: #fff;
            padding: 2px 6px;
            border-radius: 3px;
            margin-left: 6px;
            vertical-align: middle;
            font-weight: 600;
        }
    </style>
</head>
<body>

<jsp:include page="/WEB-INF/views/layout/navbar.jsp"/>

<div class="app-container">
    <div class="breadcrumb-bar">
        <a href="<%= request.getContextPath() %>/dashboard">Dashboard</a>
        <span class="breadcrumb-separator">/</span>
        <a href="<%= request.getContextPath() %>/experiments">Experiments</a>
        <span class="breadcrumb-separator">/</span>
        <span>Algorithm Comparison</span>
    </div>

    <div class="page-header">
        <div class="page-title-group">
            <h1>Multi-Algorithm Side-by-Side Comparison</h1>
            <p class="page-subtitle">Evaluating ${comparison.runs.size()} scheduling simulation runs across normalized domain metrics</p>
        </div>
        <div class="page-actions">
            <a href="<%= request.getContextPath() %>/experiments" class="btn btn-secondary">&larr; Back to Experiments</a>
            <form action="<%= request.getContextPath() %>/experiments/compare" method="get" style="display:inline;">
                <input type="hidden" name="export" value="csv"/>
                <c:forEach var="run" items="${comparison.runs}">
                    <input type="hidden" name="runId" value="${run.id}"/>
                </c:forEach>
                <button type="submit" class="btn btn-primary">Export Comparison CSV</button>
            </form>
        </div>
    </div>

    <!-- Compatibility Status Banner -->
    <c:choose>
        <c:when test="${comparison.compatible}">
            <div class="alert alert-success">
                <div>
                    <strong>Workload Compatibility Verified:</strong>
                    <p style="margin-top: 4px;"><c:out value="${comparison.compatibilityMessage}"/></p>
                </div>
            </div>
        </c:when>
        <c:otherwise>
            <div class="alert alert-warning">
                <div>
                    <strong>Uncontrolled Comparison Warning:</strong>
                    <p style="margin-top: 4px;"><c:out value="${comparison.compatibilityMessage}"/></p>
                </div>
            </div>
        </c:otherwise>
    </c:choose>

    <!-- Side-by-side Matrix Card -->
    <div class="card">
        <div class="card-header">
            <div>
                <h2 class="card-title">Comparative Performance Matrix</h2>
                <p class="card-description">Lowest waiting and turnaround times are highlighted as best performers for this specific workload.</p>
            </div>
        </div>
        <div class="table-responsive" style="margin: 0; box-shadow: none; border: none;">
            <table class="data-table">
                <thead>
                    <tr>
                        <th style="width: 220px;">Metric / Property</th>
                        <c:forEach var="run" items="${comparison.runs}">
                            <th>
                                <strong><c:out value="${run.algorithmName}"/></strong><br/>
                                <small style="color: var(--color-slate-500); font-weight: normal;">
                                    <a href="<%= request.getContextPath() %>/experiments/view?id=${run.id}">Run #${run.id}</a> (WL #${run.workloadId})
                                </small>
                            </th>
                        </c:forEach>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td style="font-weight: 600;">Algorithm Parameters</td>
                        <c:forEach var="run" items="${comparison.runs}">
                            <td><code><c:out value="${run.algorithmParameters}" default="Default"/></code></td>
                        </c:forEach>
                    </tr>
                    <tr>
                        <td style="font-weight: 600;">Execution Status</td>
                        <c:forEach var="run" items="${comparison.runs}">
                            <td>
                                <span class="badge ${run.runStatus == 'SUCCEEDED' ? 'badge-success' : 'badge-danger'}">
                                    <c:out value="${run.runStatus}"/>
                                </span>
                            </td>
                        </c:forEach>
                    </tr>
                    <tr>
                        <td style="font-weight: 600;">Validation Invariants</td>
                        <c:forEach var="run" items="${comparison.runs}">
                            <td>
                                <span class="badge ${run.validationStatus == 'VALID' ? 'badge-success' : 'badge-danger'}">
                                    <c:out value="${run.validationStatus}"/>
                                </span>
                            </td>
                        </c:forEach>
                    </tr>
                    <tr>
                        <td style="font-weight: 600;">Average Waiting Time</td>
                        <c:forEach var="run" items="${comparison.runs}">
                            <td class="${run.id == comparison.minWaitingRunId ? 'highlight-winner' : ''}">
                                <c:out value="${run.result != null ? run.result.aggregateMetrics.averageWaitingTime : 'N/A'}"/>
                                <c:if test="${run.id == comparison.minWaitingRunId}"><span class="tag-winner">LOWEST</span></c:if>
                            </td>
                        </c:forEach>
                    </tr>
                    <tr>
                        <td style="font-weight: 600;">Average Turnaround Time</td>
                        <c:forEach var="run" items="${comparison.runs}">
                            <td class="${run.id == comparison.minTurnaroundRunId ? 'highlight-winner' : ''}">
                                <c:out value="${run.result != null ? run.result.aggregateMetrics.averageTurnaroundTime : 'N/A'}"/>
                                <c:if test="${run.id == comparison.minTurnaroundRunId}"><span class="tag-winner">LOWEST</span></c:if>
                            </td>
                        </c:forEach>
                    </tr>
                    <tr>
                        <td style="font-weight: 600;">Average Response Time</td>
                        <c:forEach var="run" items="${comparison.runs}">
                            <td>
                                <c:out value="${run.result != null ? run.result.aggregateMetrics.averageResponseTime : 'N/A'}"/>
                            </td>
                        </c:forEach>
                    </tr>
                    <tr>
                        <td style="font-weight: 600;">Context Switch Count</td>
                        <c:forEach var="run" items="${comparison.runs}">
                            <td>
                                <c:out value="${run.result != null ? run.result.aggregateMetrics.contextSwitchCount : 'N/A'}"/>
                            </td>
                        </c:forEach>
                    </tr>
                    <tr>
                        <td style="font-weight: 600;">CPU Idle Duration</td>
                        <c:forEach var="run" items="${comparison.runs}">
                            <td>
                                <c:out value="${run.result != null ? run.result.aggregateMetrics.cpuIdleTime : 'N/A'}"/>
                            </td>
                        </c:forEach>
                    </tr>
                    <tr>
                        <td style="font-weight: 600;">Total Duration</td>
                        <c:forEach var="run" items="${comparison.runs}">
                            <td>
                                <c:out value="${run.result != null ? run.result.aggregateMetrics.totalDuration : 'N/A'}"/>
                            </td>
                        </c:forEach>
                    </tr>
                    <tr>
                        <td style="font-weight: 600;">Simulated Throughput</td>
                        <c:forEach var="run" items="${comparison.runs}">
                            <td>
                                <c:out value="${run.result != null ? run.result.aggregateMetrics.throughput : 'N/A'}"/>
                            </td>
                        </c:forEach>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>

    <div class="alert alert-info">
        <div>
            <strong>Evaluation Scope Note:</strong>
            Relative algorithm efficacy depends strictly on arrival distribution, burst variance, and quantum selection. An algorithm that minimizes average waiting time for this workload is not guaranteed to be universally optimal across all process sets.
        </div>
    </div>
</div>

<script src="<%= request.getContextPath() %>/assets/js/app.js"></script>
</body>
</html>
