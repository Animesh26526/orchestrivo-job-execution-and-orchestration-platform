<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<html>
<head><title>Workload Details</title></head>
<body>
<h2>Workload: <c:out value="${workload.name}"/></h2>
<a href="<%= request.getContextPath() %>/workloads">Back</a>
<p><c:out value="${workload.description}"/></p>

<h3>Run Experiment</h3>
<form action="<%= request.getContextPath() %>/experiments/run" method="post">
    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}"/>
    <input type="hidden" name="workloadId" value="${workload.id}"/>
    
    <label>Algorithm: 
        <select name="algorithmName">
            <option value="FCFS">FCFS</option>
            <option value="SJF">SJF</option>
            <option value="SRTF">SRTF</option>
            <option value="ROUND_ROBIN">Round Robin</option>
        </select>
    </label><br/>
    
    <label>Quantum (for RR): <input type="number" name="quantum" value="3" min="1"></label><br/>
    
    <button type="submit">Run</button>
</form>

</body>
</html>