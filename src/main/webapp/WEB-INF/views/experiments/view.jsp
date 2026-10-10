<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<html>
<head><title>Experiment Details</title></head>
<body>
<h2>Experiment Run: ${experiment.id}</h2>
<a href="<%= request.getContextPath() %>/experiments">Back</a>
<p>Algorithm: ${experiment.algorithmName}</p>
<p>Status: ${experiment.runStatus}</p>
<p>Validation: ${experiment.validationStatus}</p>
<c:if test="${not empty experiment.errorMessage}">
    <p style="color:red;"><c:out value="${experiment.errorMessage}"/></p>
</c:if>

<c:if test="${experiment.runStatus == 'SUCCEEDED'}">
    <h3>Aggregate Metrics</h3>
    <p>Avg Wait Time: ${experiment.result.aggregateMetrics.averageWaitingTime}</p>
    <p>Avg Turnaround: ${experiment.result.aggregateMetrics.averageTurnaroundTime}</p>
    <p>Context Switches: ${experiment.result.aggregateMetrics.contextSwitchCount}</p>
    <p>Throughput: ${experiment.result.aggregateMetrics.throughput}</p>
    
    <h3>Gantt Chart (Text Representation)</h3>
    <table border="1">
        <tr><th>Process</th><th>Start</th><th>End</th></tr>
        <c:forEach var="seg" items="${experiment.result.executionTimeline}">
            <tr>
                <td><c:out value="${seg.processId}"/></td>
                <td>${seg.startTime}</td>
                <td>${seg.endTime}</td>
            </tr>
        </c:forEach>
    </table>
</c:if>

</body>
</html>