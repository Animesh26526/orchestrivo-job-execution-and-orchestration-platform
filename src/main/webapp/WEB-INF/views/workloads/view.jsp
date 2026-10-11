<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
    <title>Workload: <c:out value="${workload.name}"/> - Orchestrivo</title>
    <style>
        body { font-family: sans-serif; margin: 20px; line-height: 1.5; }
        table { border-collapse: collapse; margin-top: 10px; width: 60%; }
        th, td { border: 1px solid #ddd; padding: 6px 12px; text-align: left; }
        th { background: #f4f4f4; }
        .box { background: #f9f9f9; border: 1px solid #e0e0e0; padding: 15px; margin: 15px 0; width: 60%; }
    </style>
</head>
<body>
<h2>Workload: <c:out value="${workload.name}"/></h2>
<p>
    <a href="<%= request.getContextPath() %>/workloads">Back to Workloads</a> | 
    <a href="<%= request.getContextPath() %>/workloads/edit?id=${workload.id}">Edit Workload</a> | 
    <a href="<%= request.getContextPath() %>/workloads/export?id=${workload.id}">Export CSV</a>
</p>

<p><strong>Description:</strong> <c:out value="${workload.description}"/></p>
<p><strong>Source Type:</strong> <c:out value="${workload.sourceType}"/> 
    <c:if test="${not empty workload.generationSeed}">(Seed: ${workload.generationSeed})</c:if>
</p>
<p><strong>Created:</strong> <c:out value="${workload.createdAt}"/></p>

<h3>Process List (${workload.workload != null ? workload.workload.processes.size() : 0} processes)</h3>
<table>
    <thead>
        <tr>
            <th>Process ID</th>
            <th>Arrival Time</th>
            <th>Burst Time</th>
        </tr>
    </thead>
    <tbody>
    <c:forEach var="p" items="${workload.workload.processes}">
        <tr>
            <td><strong><c:out value="${p.processId}"/></strong></td>
            <td>${p.arrivalTime}</td>
            <td>${p.burstTime}</td>
        </tr>
    </c:forEach>
    </tbody>
</table>

<div class="box">
    <h3>Run Scheduling Experiment</h3>
    <form action="<%= request.getContextPath() %>/experiments/run" method="post">
        <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}'/>"/>
        <input type="hidden" name="workloadId" value="${workload.id}"/>
        
        <label>Algorithm: 
            <select name="algorithmName">
                <option value="FCFS">First-Come, First-Served (FCFS)</option>
                <option value="SJF">Shortest Job First (SJF - Non-preemptive)</option>
                <option value="SRTF">Shortest Remaining Time First (SRTF - Preemptive)</option>
                <option value="ROUND_ROBIN">Round Robin (RR)</option>
            </select>
        </label><br/><br/>
        
        <label>Time Quantum (for Round Robin): 
            <input type="number" name="quantum" value="3" min="1"/>
        </label><br/><br/>
        
        <button type="submit">Run Simulation</button>
    </form>
</div>

</body>
</html>