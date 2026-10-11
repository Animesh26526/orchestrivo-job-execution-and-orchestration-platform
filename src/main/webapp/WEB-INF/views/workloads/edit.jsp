<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
    <title>Edit Workload - Orchestrivo</title>
    <style>
        body { font-family: sans-serif; margin: 20px; line-height: 1.5; }
        .error { color: #d9534f; background: #fdf7f7; border: 1px solid #eed3d7; padding: 10px; margin-bottom: 15px; }
        table { border-collapse: collapse; margin-top: 10px; }
        th, td { border: 1px solid #ccc; padding: 6px 10px; text-align: left; }
        th { background: #eee; }
    </style>
</head>
<body>
<h2>Edit Workload: <c:out value="${workload.name}"/></h2>
<p><a href="<%= request.getContextPath() %>/workloads/view?id=${workload.id}">Cancel & View</a> | <a href="<%= request.getContextPath() %>/workloads">Back to List</a></p>

<c:if test="${not empty error}">
    <div class="error"><c:out value="${error}"/></div>
</c:if>

<form action="<%= request.getContextPath() %>/workloads/edit" method="post">
    <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}'/>"/>
    <input type="hidden" name="id" value="${workload.id}"/>

    <label>Workload Name: <br/>
        <input type="text" name="name" value="<c:out value='${workload.name}'/>" required size="50"/>
    </label><br/><br/>

    <label>Description: <br/>
        <textarea name="description" rows="3" cols="60"><c:out value="${workload.description}"/></textarea>
    </label><br/><br/>

    <h3>Processes</h3>
    <p><small>Arrival times must be &ge; 0. Burst times must be &gt; 0. Process IDs must be unique.</small></p>

    <table id="processTable">
        <thead>
            <tr>
                <th>Process ID</th>
                <th>Arrival Time</th>
                <th>Burst Time</th>
                <th>Action</th>
            </tr>
        </thead>
        <tbody>
        <c:forEach var="p" items="${workload.workload.processes}">
            <tr>
                <td><input type="text" name="processId" value="<c:out value='${p.processId}'/>" required size="10"/></td>
                <td><input type="number" name="arrivalTime" value="${p.arrivalTime}" min="0" required size="10"/></td>
                <td><input type="number" name="burstTime" value="${p.burstTime}" min="1" required size="10"/></td>
                <td><button type="button" onclick="removeRow(this)">Remove</button></td>
            </tr>
        </c:forEach>
        </tbody>
    </table>

    <br/>
    <button type="button" onclick="addRow()">+ Add Process</button>
    <br/><br/>
    <button type="submit">Save Changes</button>
</form>

<script>
let counter = ${workload.workload != null ? workload.workload.processes.size() + 1 : 1};
function addRow() {
    const table = document.getElementById('processTable').getElementsByTagName('tbody')[0];
    const newRow = table.insertRow();
    newRow.innerHTML = '<td><input type="text" name="processId" value="P' + (counter++) + '" required size="10"/></td>' +
                      '<td><input type="number" name="arrivalTime" value="0" min="0" required size="10"/></td>' +
                      '<td><input type="number" name="burstTime" value="5" min="1" required size="10"/></td>' +
                      '<td><button type="button" onclick="removeRow(this)">Remove</button></td>';
}
function removeRow(btn) {
    const row = btn.parentNode.parentNode;
    if (row.parentNode.rows.length > 1) {
        row.parentNode.removeChild(row);
    } else {
        alert("A workload must contain at least one process.");
    }
}
</script>

</body>
</html>
