<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
    <title>Import CSV Workload - Orchestrivo</title>
    <style>
        body { font-family: sans-serif; margin: 20px; line-height: 1.5; }
        .error { color: #d9534f; background: #fdf7f7; border: 1px solid #eed3d7; padding: 10px; margin-bottom: 15px; }
        .sample { background: #f8f9fa; border: 1px solid #e9ecef; padding: 10px; font-family: monospace; }
    </style>
</head>
<body>
<h2>Import Workload from CSV</h2>
<p><a href="<%= request.getContextPath() %>/workloads">Back to Workloads</a></p>

<c:if test="${not empty error}">
    <div class="error"><c:out value="${error}"/></div>
</c:if>

<c:if test="${not empty errors}">
    <div class="error">
        <strong>Validation Errors:</strong>
        <ul>
            <c:forEach var="e" items="${errors}">
                <li><c:out value="${e}"/></li>
            </c:forEach>
        </ul>
    </div>
</c:if>

<div class="sample">
    <strong>Expected CSV Format:</strong><br/>
    process_id,arrival_time,burst_time<br/>
    P1,0,5<br/>
    P2,1,3<br/>
    P3,2,1<br/>
    P4,4,2<br/>
</div>
<p><small>Headers required. Max 500 rows. Process IDs must be unique. Arrival times &ge; 0. Burst times &gt; 0.</small></p>

<form action="<%= request.getContextPath() %>/workloads/import" method="post" enctype="multipart/form-data">
    <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}'/>"/>

    <label>Workload Name: <br/>
        <input type="text" name="name" value="<c:out value='${name}'/>" required size="50"/>
    </label><br/><br/>

    <label>Description: <br/>
        <textarea name="description" rows="2" cols="60"><c:out value="${description}"/></textarea>
    </label><br/><br/>

    <label>Option A: Upload CSV File: <br/>
        <input type="file" name="csvFile" accept=".csv,text/csv"/>
    </label><br/><br/>

    <label>Option B: Or Paste CSV Text Directly: <br/>
        <textarea name="csvContent" rows="8" cols="60" placeholder="process_id,arrival_time,burst_time&#10;P1,0,5&#10;P2,1,3"></textarea>
    </label><br/><br/>

    <button type="submit">Import Workload</button>
</form>

</body>
</html>
