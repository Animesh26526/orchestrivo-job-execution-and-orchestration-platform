<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
    <title>Generate Seeded Workload - Orchestrivo</title>
    <style>
        body { font-family: sans-serif; margin: 20px; line-height: 1.5; }
        .error { color: #d9534f; background: #fdf7f7; border: 1px solid #eed3d7; padding: 10px; margin-bottom: 15px; }
        .note { background: #eef7fb; border: 1px solid #bce8f1; padding: 10px; margin-bottom: 15px; }
    </style>
</head>
<body>
<h2>Generate Seeded Workload</h2>
<p><a href="<%= request.getContextPath() %>/workloads">Back to Workloads</a></p>

<div class="note">
    <strong>Reproducible Workload Generation:</strong><br/>
    Process arrival and burst parameters are generated deterministically using a fixed pseudo-random seed. Using the same seed and parameters reproduces the identical process workload.
</div>

<c:if test="${not empty error}">
    <div class="error"><c:out value="${error}"/></div>
</c:if>

<form action="<%= request.getContextPath() %>/workloads/generate" method="post">
    <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}'/>"/>

    <label>Workload Name: <br/>
        <input type="text" name="name" value="Generated Workload" required size="40"/>
    </label><br/><br/>

    <label>Process Count (1 - 200): <br/>
        <input type="number" name="processCount" value="10" min="1" max="200" required/>
    </label><br/><br/>

    <label>Arrival Time Range: <br/>
        Min: <input type="number" name="minArrival" value="0" min="0" required/>
        Max: <input type="number" name="maxArrival" value="15" min="0" required/>
    </label><br/><br/>

    <label>Burst Time Range: <br/>
        Min: <input type="number" name="minBurst" value="1" min="1" required/>
        Max: <input type="number" name="maxBurst" value="10" min="1" required/>
    </label><br/><br/>

    <label>Random Seed: <br/>
        <input type="number" name="seed" value="42" required/>
    </label><br/><br/>

    <button type="submit">Generate Workload</button>
</form>

</body>
</html>
