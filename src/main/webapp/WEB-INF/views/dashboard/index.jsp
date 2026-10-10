<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head><title>Dashboard - Orchestrivo</title></head>
<body>
<h2>Dashboard</h2>
<form action="<%= request.getContextPath() %>/logout" method="post">
    <button type="submit">Logout</button>
</form>
<hr/>
<a href="<%= request.getContextPath() %>/workloads">My Workloads</a> | 
<a href="<%= request.getContextPath() %>/experiments">My Experiments</a>
</body>
</html>