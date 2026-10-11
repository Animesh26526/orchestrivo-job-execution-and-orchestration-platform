<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
    <title>Experiment #${experiment.id} Details - Orchestrivo</title>
    <style>
        body { font-family: sans-serif; margin: 20px; line-height: 1.5; color: #333; }
        .nav { margin-bottom: 15px; }
        .nav a { margin-right: 15px; text-decoration: none; color: #0066cc; }
        .card { background: #fdfdfd; border: 1px solid #ddd; border-radius: 4px; padding: 15px; margin: 15px 0; max-width: 900px; }
        .metric-grid { display: flex; flex-wrap: wrap; gap: 15px; margin: 15px 0; }
        .metric-box { background: #f0f4f8; border: 1px solid #d0dbe5; border-radius: 4px; padding: 12px 18px; min-width: 160px; }
        .metric-label { font-size: 0.85em; color: #666; text-transform: uppercase; letter-spacing: 0.5px; }
        .metric-value { font-size: 1.4em; font-weight: bold; color: #1a365d; margin-top: 4px; }
        table { border-collapse: collapse; width: 100%; max-width: 900px; margin-top: 10px; margin-bottom: 20px; }
        th, td { border: 1px solid #ddd; padding: 8px 12px; text-align: left; }
        th { background: #f4f6f8; font-weight: 600; }
        tr:nth-child(even) { background: #fafbfc; }
        .badge { display: inline-block; padding: 3px 8px; border-radius: 3px; font-size: 0.85em; font-weight: bold; }
        .badge-succeeded { background: #e6f4ea; color: #137333; }
        .badge-failed { background: #fce8e6; color: #c5221f; }
        .badge-valid { background: #e6f4ea; color: #137333; }
        .badge-invalid { background: #fce8e6; color: #c5221f; }
        .badge-error { background: #fef7e0; color: #b06000; }
        .timeline-bar-container { display: flex; width: 100%; max-width: 900px; border: 1px solid #ccc; border-radius: 4px; overflow: hidden; height: 36px; margin: 15px 0; background: #eee; }
        .timeline-block { display: flex; align-items: center; justify-content: center; font-size: 0.8em; font-weight: bold; color: #fff; text-shadow: 0 1px 1px rgba(0,0,0,0.4); border-right: 1px solid rgba(255,255,255,0.5); overflow: hidden; }
        .btn { display: inline-block; padding: 6px 12px; background: #0066cc; color: #fff; text-decoration: none; border-radius: 4px; font-size: 0.9em; border: none; cursor: pointer; }
        .btn:hover { background: #0052a3; }
        .note { font-size: 0.9em; color: #555; background: #fff8e1; border-left: 4px solid #ffb300; padding: 8px 12px; margin: 10px 0; max-width: 900px; }
    </style>
</head>
<body>

<div class="nav">
    <a href="<%= request.getContextPath() %>/experiments">&larr; Back to Experiments</a>
    <a href="<%= request.getContextPath() %>/dashboard">Dashboard</a>
    <a href="<%= request.getContextPath() %>/workloads/view?id=${experiment.workloadId}">View Workload</a>
</div>

<h2>Experiment Run #${experiment.id}</h2>

<div class="card">
    <table style="width: auto; margin: 0; border: none;">
        <tr><td style="border:none; padding: 4px 10px; font-weight:bold;">Algorithm:</td><td style="border:none; padding: 4px 10px;"><c:out value="${experiment.algorithmName}"/></td></tr>
        <tr><td style="border:none; padding: 4px 10px; font-weight:bold;">Parameters:</td><td style="border:none; padding: 4px 10px;"><c:out value="${experiment.algorithmParameters}" default="None"/></td></tr>
        <tr>
            <td style="border:none; padding: 4px 10px; font-weight:bold;">Run Status:</td>
            <td style="border:none; padding: 4px 10px;">
                <span class="badge ${experiment.runStatus == 'SUCCEEDED' ? 'badge-succeeded' : 'badge-failed'}">
                    <c:out value="${experiment.runStatus}"/>
                </span>
            </td>
        </tr>
        <tr>
            <td style="border:none; padding: 4px 10px; font-weight:bold;">Validation:</td>
            <td style="border:none; padding: 4px 10px;">
                <span class="badge ${experiment.validationStatus == 'VALID' ? 'badge-valid' : (experiment.validationStatus == 'INVALID' ? 'badge-invalid' : 'badge-error')}">
                    <c:out value="${experiment.validationStatus}"/>
                </span>
            </td>
        </tr>
        <tr><td style="border:none; padding: 4px 10px; font-weight:bold;">Executed At:</td><td style="border:none; padding: 4px 10px;"><c:out value="${experiment.createdAt}"/></td></tr>
    </table>
    <c:if test="${not empty experiment.errorMessage}">
        <p style="color:#c5221f; font-weight:bold; margin-top: 10px;">Error: <c:out value="${experiment.errorMessage}"/></p>
    </c:if>
    <div style="margin-top: 15px;">
        <a class="btn" href="<%= request.getContextPath() %>/experiments/export?id=${experiment.id}">Export Run as CSV</a>
    </div>
</div>

<c:if test="${experiment.runStatus == 'SUCCEEDED' && experiment.result != null}">
    <h3>Aggregate Simulation Metrics</h3>
    <div class="metric-grid">
        <div class="metric-box">
            <div class="metric-label">Avg Waiting Time</div>
            <div class="metric-value"><c:out value="${experiment.result.aggregateMetrics.averageWaitingTime}"/></div>
        </div>
        <div class="metric-box">
            <div class="metric-label">Avg Turnaround Time</div>
            <div class="metric-value"><c:out value="${experiment.result.aggregateMetrics.averageTurnaroundTime}"/></div>
        </div>
        <div class="metric-box">
            <div class="metric-label">Avg Response Time</div>
            <div class="metric-value"><c:out value="${experiment.result.aggregateMetrics.averageResponseTime}"/></div>
        </div>
        <div class="metric-box">
            <div class="metric-label">Context Switches</div>
            <div class="metric-value"><c:out value="${experiment.result.aggregateMetrics.contextSwitchCount}"/></div>
        </div>
        <div class="metric-box">
            <div class="metric-label">Total Duration</div>
            <div class="metric-value"><c:out value="${experiment.result.aggregateMetrics.totalDuration}"/></div>
        </div>
        <div class="metric-box">
            <div class="metric-label">Throughput</div>
            <div class="metric-value"><c:out value="${experiment.result.aggregateMetrics.throughput}"/></div>
        </div>
        <div class="metric-box">
            <div class="metric-label">CPU Busy Time</div>
            <div class="metric-value"><c:out value="${experiment.result.aggregateMetrics.cpuBusyTime}"/></div>
        </div>
        <div class="metric-box">
            <div class="metric-label">CPU Idle Time</div>
            <div class="metric-value"><c:out value="${experiment.result.aggregateMetrics.cpuIdleTime}"/></div>
        </div>
    </div>

    <h3>Gantt Chart & Execution Timeline</h3>
    <c:if test="${experiment.result.aggregateMetrics.totalDuration > 0}">
        <div class="timeline-bar-container">
            <c:set var="colors" value="${['#1976d2', '#388e3c', '#f57c00', '#7b1fa2', '#c2185b', '#0097a7', '#5d4037', '#616161']}"/>
            <c:forEach var="seg" items="${experiment.result.executionTimeline}" varStatus="loop">
                <c:set var="segDuration" value="${seg.endTime - seg.startTime}"/>
                <c:set var="pct" value="${(segDuration * 100.0) / experiment.result.aggregateMetrics.totalDuration}"/>
                <c:set var="colorIdx" value="${loop.index % 8}"/>
                <div class="timeline-block" 
                     style="width: ${pct}%; background-color: ${seg.processId == 'IDLE' ? '#9e9e9e' : colors[colorIdx]};"
                     title="<c:out value='${seg.processId}'/>: [${seg.startTime} - ${seg.endTime}] (${segDuration} time units)">
                    <c:out value="${seg.processId}"/>
                </div>
            </c:forEach>
        </div>
    </c:if>

    <table>
        <thead>
            <tr>
                <th>Segment</th>
                <th>Process ID</th>
                <th>Start Time</th>
                <th>End Time</th>
                <th>Duration</th>
            </tr>
        </thead>
        <tbody>
        <c:forEach var="seg" items="${experiment.result.executionTimeline}" varStatus="status">
            <tr>
                <td>${status.count}</td>
                <td><strong><c:out value="${seg.processId}"/></strong></td>
                <td>${seg.startTime}</td>
                <td>${seg.endTime}</td>
                <td>${seg.endTime - seg.startTime}</td>
            </tr>
        </c:forEach>
        </tbody>
    </table>

    <h3>Per-Process Metrics</h3>
    <table>
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
                <td><strong><c:out value="${pm.processId}"/></strong></td>
                <td>${pm.firstStartTime}</td>
                <td>${pm.completionTime}</td>
                <td>${pm.waitingTime}</td>
                <td>${pm.turnaroundTime}</td>
                <td>${pm.responseTime}</td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</c:if>

<h3>Immutable Workload Input Snapshot</h3>
<div class="note">
    <strong>Immutable Snapshot:</strong> The table below reflects the exact process specifications captured at execution time. Even if the underlying workload is subsequently modified or deleted, this snapshot preserves historical scientific reproducibility.
</div>
<table>
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
            <td><strong><c:out value="${p.processId}"/></strong></td>
            <td>${p.arrivalTime}</td>
            <td>${p.burstTime}</td>
        </tr>
    </c:forEach>
    <c:if test="${empty experiment.snapshotProcesses}">
        <tr>
            <td colspan="3" style="text-align:center; color: #888;">No snapshot data recorded for this run.</td>
        </tr>
    </c:if>
    </tbody>
</table>

</body>
</html>