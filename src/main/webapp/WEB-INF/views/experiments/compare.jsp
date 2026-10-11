<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
    <title>Algorithm Comparison - Orchestrivo</title>
    <style>
        body { font-family: sans-serif; margin: 20px; line-height: 1.5; color: #333; }
        .nav { margin-bottom: 15px; }
        .nav a { margin-right: 15px; text-decoration: none; color: #0066cc; }
        .banner { padding: 12px 16px; border-radius: 4px; margin-bottom: 20px; max-width: 1000px; }
        .banner-success { background: #e6f4ea; border: 1px solid #ceead6; color: #137333; }
        .banner-warning { background: #fce8e6; border: 1px solid #fad2cf; color: #c5221f; }
        table { border-collapse: collapse; width: 100%; max-width: 1000px; margin-top: 15px; margin-bottom: 25px; }
        th, td { border: 1px solid #ddd; padding: 10px 14px; text-align: left; }
        th { background: #f4f6f8; font-weight: 600; }
        .highlight-best { background: #e6f4ea !important; font-weight: bold; color: #137333; }
        .note { font-size: 0.9em; color: #444; background: #fff8e1; border-left: 4px solid #ffb300; padding: 12px 16px; margin: 20px 0; max-width: 1000px; border-radius: 0 4px 4px 0; }
        .note pre { font-family: inherit; margin: 5px 0 0 0; white-space: pre-wrap; line-height: 1.6; }
        .btn { display: inline-block; padding: 6px 14px; background: #0066cc; color: #fff; text-decoration: none; border-radius: 4px; font-size: 0.9em; border: none; cursor: pointer; }
        .btn:hover { background: #0052a3; }
        .tag-winner { font-size: 0.75em; background: #137333; color: white; padding: 2px 6px; border-radius: 3px; margin-left: 6px; vertical-align: middle; }
    </style>
</head>
<body>

<div class="nav">
    <a href="<%= request.getContextPath() %>/experiments">&larr; Back to Experiments</a>
    <a href="<%= request.getContextPath() %>/dashboard">Dashboard</a>
</div>

<h2>Multi-Algorithm Experiment Comparison</h2>

<c:choose>
    <c:when test="${comparison.compatible}">
        <div class="banner banner-success">
            <strong>Workload Compatibility Verified:</strong> <c:out value="${comparison.compatibilityMessage}"/>
        </div>
    </c:when>
    <c:otherwise>
        <div class="banner banner-warning">
            <strong>Warning:</strong> <c:out value="${comparison.compatibilityMessage}"/>
        </div>
    </c:otherwise>
</c:choose>

<div style="margin-bottom: 15px;">
    <form action="<%= request.getContextPath() %>/experiments/compare" method="get" style="display:inline;">
        <input type="hidden" name="export" value="csv"/>
        <c:forEach var="run" items="${comparison.runs}">
            <input type="hidden" name="runId" value="${run.id}"/>
        </c:forEach>
        <button type="submit" class="btn">Export Comparison as CSV</button>
    </form>
</div>

<h3>Side-by-Side Performance Comparison</h3>
<table>
    <thead>
        <tr>
            <th style="width: 250px;">Metric / Property</th>
            <c:forEach var="run" items="${comparison.runs}">
                <th>
                    <strong><c:out value="${run.algorithmName}"/></strong><br/>
                    <small>Run #${run.id} (WL #${run.workloadId})</small>
                </th>
            </c:forEach>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td><strong>Parameters</strong></td>
            <c:forEach var="run" items="${comparison.runs}">
                <td><c:out value="${run.algorithmParameters}" default="None"/></td>
            </c:forEach>
        </tr>
        <tr>
            <td><strong>Run Status</strong></td>
            <c:forEach var="run" items="${comparison.runs}">
                <td><c:out value="${run.runStatus}"/></td>
            </c:forEach>
        </tr>
        <tr>
            <td><strong>Validation Status</strong></td>
            <c:forEach var="run" items="${comparison.runs}">
                <td><c:out value="${run.validationStatus}"/></td>
            </c:forEach>
        </tr>
        <tr>
            <td><strong>Average Waiting Time</strong></td>
            <c:forEach var="run" items="${comparison.runs}">
                <td class="${run.id == comparison.minWaitingRunId ? 'highlight-best' : ''}">
                    <c:out value="${run.result != null ? run.result.aggregateMetrics.averageWaitingTime : 'N/A'}"/>
                    <c:if test="${run.id == comparison.minWaitingRunId}"><span class="tag-winner">MIN</span></c:if>
                </td>
            </c:forEach>
        </tr>
        <tr>
            <td><strong>Average Turnaround Time</strong></td>
            <c:forEach var="run" items="${comparison.runs}">
                <td class="${run.id == comparison.minTurnaroundRunId ? 'highlight-best' : ''}">
                    <c:out value="${run.result != null ? run.result.aggregateMetrics.averageTurnaroundTime : 'N/A'}"/>
                    <c:if test="${run.id == comparison.minTurnaroundRunId}"><span class="tag-winner">MIN</span></c:if>
                </td>
            </c:forEach>
        </tr>
        <tr>
            <td><strong>Average Response Time</strong></td>
            <c:forEach var="run" items="${comparison.runs}">
                <td class="${run.id == comparison.minResponseRunId ? 'highlight-best' : ''}">
                    <c:out value="${run.result != null ? run.result.aggregateMetrics.averageResponseTime : 'N/A'}"/>
                    <c:if test="${run.id == comparison.minResponseRunId}"><span class="tag-winner">MIN</span></c:if>
                </td>
            </c:forEach>
        </tr>
        <tr>
            <td><strong>Throughput (proc/unit)</strong></td>
            <c:forEach var="run" items="${comparison.runs}">
                <td class="${run.id == comparison.maxThroughputRunId ? 'highlight-best' : ''}">
                    <c:out value="${run.result != null ? run.result.aggregateMetrics.throughput : 'N/A'}"/>
                    <c:if test="${run.id == comparison.maxThroughputRunId}"><span class="tag-winner">MAX</span></c:if>
                </td>
            </c:forEach>
        </tr>
        <tr>
            <td><strong>Context Switches</strong></td>
            <c:forEach var="run" items="${comparison.runs}">
                <td class="${run.id == comparison.minContextSwitchRunId ? 'highlight-best' : ''}">
                    <c:out value="${run.result != null ? run.result.aggregateMetrics.contextSwitchCount : 'N/A'}"/>
                    <c:if test="${run.id == comparison.minContextSwitchRunId}"><span class="tag-winner">MIN</span></c:if>
                </td>
            </c:forEach>
        </tr>
        <tr>
            <td><strong>Total Duration</strong></td>
            <c:forEach var="run" items="${comparison.runs}">
                <td><c:out value="${run.result != null ? run.result.aggregateMetrics.totalDuration : 'N/A'}"/></td>
            </c:forEach>
        </tr>
        <tr>
            <td><strong>CPU Busy Time</strong></td>
            <c:forEach var="run" items="${comparison.runs}">
                <td><c:out value="${run.result != null ? run.result.aggregateMetrics.cpuBusyTime : 'N/A'}"/></td>
            </c:forEach>
        </tr>
        <tr>
            <td><strong>CPU Idle Time</strong></td>
            <c:forEach var="run" items="${comparison.runs}">
                <td><c:out value="${run.result != null ? run.result.aggregateMetrics.cpuIdleTime : 'N/A'}"/></td>
            </c:forEach>
        </tr>
        <tr>
            <td><strong>Actions</strong></td>
            <c:forEach var="run" items="${comparison.runs}">
                <td><a href="<%= request.getContextPath() %>/experiments/view?id=${run.id}">View Details</a></td>
            </c:forEach>
        </tr>
    </tbody>
</table>

<div class="note">
    <strong>Theoretical & Operational Trade-Off Observations</strong>
    <pre><c:out value="${comparison.tradeOffNotes}"/></pre>
</div>

</body>
</html>
