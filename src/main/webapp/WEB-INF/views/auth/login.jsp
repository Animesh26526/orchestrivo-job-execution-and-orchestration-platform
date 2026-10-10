<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head><title>Login - Orchestrivo</title></head>
<body>
<h2>Login</h2>
<% if(request.getAttribute("error") != null) { %>
    <p style="color:red;"><%= request.getAttribute("error") %></p>
<% } %>
<form action="<%= request.getContextPath() %>/login" method="post">
    <label>Email: <input type="email" name="email" required></label><br/>
    <label>Password: <input type="password" name="password" required></label><br/>
    <button type="submit">Login</button>
</form>
<a href="<%= request.getContextPath() %>/register">Register</a>
</body>
</html>