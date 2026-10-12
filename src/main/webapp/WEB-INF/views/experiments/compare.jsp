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
        .delta-pill {
            font-size: 11px;
            font-weight: 600;
            padding: 2px 6px;
            border-radius: 4px;
            display: inline-block;
            margin-top: 4px;
            background: #f1f5f9;
            color: #475569;
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

    <!-- Mode Badge & Compatibility Status Banner -->
    <c:choose>
        <c:when test="${comparison.mode == 'MODE_A_CONTROLLED'}">
            <div class="alert alert-success" style="display: flex; align-items: flex-start; gap: 14px;">
                <span class="badge badge-success" style="font-size: 12px; padding: 6px 10px; white-space: nowrap;">Mode A: Controlled</span>
                <div>
                    <strong>Controlled Benchmark Comparison:</strong>
                    <p style="margin-top: 4px; margin-bottom: 0;"><c:out value="${comparison.compatibilityMessage}"/></p>
                </div>
            </div>
        </c:when>
        <c:when test="${comparison.mode == 'MODE_B_WORKLOAD_SCALING'}">
            <div class="alert alert-info" style="display: flex; align-items: flex-start; gap: 14px;">
                <span class="badge badge-primary" style="font-size: 12px; padding: 6px 10px; white-space: nowrap;">Mode B: Workload Scaling</span>
                <div>
                    <strong>Workload Scaling Analysis:</strong>
                    <p style="margin-top: 4px; margin-bottom: 0;"><c:out value="${comparison.compatibilityMessage}"/></p>
                </div>
            </div>
        </c:when>
        <c:otherwise>
            <div class="alert alert-warning" style="display: flex; align-items: flex-start; gap: 14px;">
                <span class="badge badge-warning" style="font-size: 12px; padding: 6px 10px; white-space: nowrap;">Mode C: Exploratory</span>
                <div>
                    <strong>Exploratory / Incompatible Workload Notice:</strong>
                    <p style="margin-top: 4px; margin-bottom: 0;"><c:out value="${comparison.compatibilityMessage}"/></p>
                </div>
            </div>
        </c:otherwise>
    </c:choose>

    <!-- Side-by-side Matrix Card -->
    <div class="card">
        <div class="card-header">
            <div>
                <h2 class="card-title">Comparative Performance Matrix</h2>
                <p class="card-description">
                    <c:choose>
                        <c:when test="${comparison.mode == 'MODE_A_CONTROLLED'}">
                            Identical workloads: lowest waiting &amp; turnaround times and highest throughput are highlighted as best performers.
                        </c:when>
                        <c:when test="${comparison.mode == 'MODE_B_WORKLOAD_SCALING'}">
                            Same algorithm across varying workloads. Percentage deltas indicate scaling relative to Run #${comparison.runs[0].id}.
                        </c:when>
                        <c:otherwise>
                            Differing algorithms and workloads. Deltas relative to Run #${comparison.runs[0].id} provided for exploratory analysis only.
                        </c:otherwise>
                    </c:choose>
                </p>
            </div>
        </div>
        <div class="table-responsive" style="margin: 0; box-shadow: none; border: none;">
            <table class="data-table">
                <thead>
                    <tr>
                        <th style="width: 220px;">Metric / Property</th>
                        <c:forEach var="run" items="${comparison.runs}" varStatus="status">
                            <th>
                                <strong><c:out value="${run.algorithmName}"/></strong><br/>
                                <small style="color: var(--color-slate-500); font-weight: normal;">
                                    <a href="<%= request.getContextPath() %>/experiments/view?id=${run.id}">Run #${run.id}</a> (WL #${run.workloadId})
                                    <c:if test="${status.first}"><span class="badge badge-secondary" style="margin-left: 4px; font-size: 10px;">BASELINE</span></c:if>
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
                            <td class="${(comparison.mode == 'MODE_A_CONTROLLED' && run.id == comparison.minWaitingRunId) ? 'highlight-winner' : ''}">
                                <div><c:out value="${run.result != null ? run.result.aggregateMetrics.averageWaitingTime : 'N/A'}"/>
                                <c:if test="${comparison.mode == 'MODE_A_CONTROLLED' && run.id == comparison.minWaitingRunId}"><span class="tag-winner">LOWEST</span></c:if></div>
                                <div><span class="delta-pill">Δ ${comparison.getWaitDelta(run)}</span></div>
                            </td>
                        </c:forEach>
                    </tr>
                    <tr>
                        <td style="font-weight: 600;">Average Turnaround Time</td>
                        <c:forEach var="run" items="${comparison.runs}">
                            <td class="${(comparison.mode == 'MODE_A_CONTROLLED' && run.id == comparison.minTurnaroundRunId) ? 'highlight-winner' : ''}">
                                <div><c:out value="${run.result != null ? run.result.aggregateMetrics.averageTurnaroundTime : 'N/A'}"/>
                                <c:if test="${comparison.mode == 'MODE_A_CONTROLLED' && run.id == comparison.minTurnaroundRunId}"><span class="tag-winner">LOWEST</span></c:if></div>
                                <div><span class="delta-pill">Δ ${comparison.getTurnaroundDelta(run)}</span></div>
                            </td>
                        </c:forEach>
                    </tr>
                    <tr>
                        <td style="font-weight: 600;">Average Response Time</td>
                        <c:forEach var="run" items="${comparison.runs}">
                            <td class="${(comparison.mode == 'MODE_A_CONTROLLED' && run.id == comparison.minResponseRunId) ? 'highlight-winner' : ''}">
                                <div><c:out value="${run.result != null ? run.result.aggregateMetrics.averageResponseTime : 'N/A'}"/>
                                <c:if test="${comparison.mode == 'MODE_A_CONTROLLED' && run.id == comparison.minResponseRunId}"><span class="tag-winner">LOWEST</span></c:if></div>
                                <div><span class="delta-pill">Δ ${comparison.getResponseDelta(run)}</span></div>
                            </td>
                        </c:forEach>
                    </tr>
                    <tr>
                        <td style="font-weight: 600;">Simulated Throughput</td>
                        <c:forEach var="run" items="${comparison.runs}">
                            <td class="${(comparison.mode == 'MODE_A_CONTROLLED' && run.id == comparison.maxThroughputRunId) ? 'highlight-winner' : ''}">
                                <div><c:out value="${run.result != null ? run.result.aggregateMetrics.throughput : 'N/A'}"/>
                                <c:if test="${comparison.mode == 'MODE_A_CONTROLLED' && run.id == comparison.maxThroughputRunId}"><span class="tag-winner">HIGHEST</span></c:if></div>
                                <div><span class="delta-pill">Δ ${comparison.getThroughputDelta(run)}</span></div>
                            </td>
                        </c:forEach>
                    </tr>
                    <tr>
                        <td style="font-weight: 600;">Context Switch Count</td>
                        <c:forEach var="run" items="${comparison.runs}">
                            <td class="${(comparison.mode == 'MODE_A_CONTROLLED' && run.id == comparison.minContextSwitchRunId) ? 'highlight-winner' : ''}">
                                <div><c:out value="${run.result != null ? run.result.aggregateMetrics.contextSwitchCount : 'N/A'}"/>
                                <c:if test="${comparison.mode == 'MODE_A_CONTROLLED' && run.id == comparison.minContextSwitchRunId}"><span class="tag-winner">LOWEST</span></c:if></div>
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
