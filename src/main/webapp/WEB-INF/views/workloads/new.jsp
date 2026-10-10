<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head><title>New Workload</title></head>
<body>
<h2>New Workload</h2>
<form action="<%= request.getContextPath() %>/workloads/new" method="post">
    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}"/>
    <label>Name: <input type="text" name="name" required></label><br/>
    <label>Description: <textarea name="description"></textarea></label><br/>
    
    <h3>Processes</h3>
    <!-- Basic minimal input for creating a workload -->
    <div id="processes">
        <div>
            ID: <input type="text" name="processId" value="P1" required/>
            Arrival: <input type="number" name="arrivalTime" value="0" required min="0"/>
            Burst: <input type="number" name="burstTime" value="5" required min="1"/>
        </div>
    </div>
    <button type="button" onclick="addProcess()">Add Process</button><br/><br/>
    
    <button type="submit">Create Workload</button>
</form>
<script>
    let counter = 2;
    function addProcess() {
        const div = document.createElement("div");
        div.innerHTML = 'ID: <input type="text" name="processId" value="P' + counter + '" required/> ' +
                        'Arrival: <input type="number" name="arrivalTime" value="0" required min="0"/> ' +
                        'Burst: <input type="number" name="burstTime" value="5" required min="1"/>';
        document.getElementById("processes").appendChild(div);
        counter++;
    }
</script>
</body>
</html>