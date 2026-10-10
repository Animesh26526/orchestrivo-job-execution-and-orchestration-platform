<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<html>
<head><title>Login - Orchestrivo</title></head>
<body>
<h2>Login</h2>
<c:if test="${not empty error}">
    <p style="color:red;"><c:out value="${error}"/></p>
</c:if>
<form action="<%= request.getContextPath() %>/login" method="post">
    <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}'/>" />
    <label>Email: <input type="email" name="email" required></label><br/>
    <label>Password: <input type="password" name="password" required></label><br/>
    <button type="submit">Login</button>
</form>
<a href="<%= request.getContextPath() %>/register">Register</a>
</body>
</html>