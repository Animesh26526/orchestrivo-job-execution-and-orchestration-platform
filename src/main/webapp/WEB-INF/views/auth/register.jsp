<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head><title>Register - Orchestrivo</title></head>
<body>
<h2>Register</h2>
<% if(request.getAttribute("error") != null) { %>
    <p style="color:red;"><%= request.getAttribute("error") %></p>
<% } %>
<form action="<%= request.getContextPath() %>/register" method="post">
    <label>Email: <input type="email" name="email" required></label><br/>
    <label>Password: <input type="password" name="password" required></label><br/>
    <button type="submit">Register</button>
</form>
<a href="<%= request.getContextPath() %>/login">Login</a>
</body>
</html>