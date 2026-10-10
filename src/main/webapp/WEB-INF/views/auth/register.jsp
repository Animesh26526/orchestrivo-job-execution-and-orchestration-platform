<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<html>
<head><title>Register - Orchestrivo</title></head>
<body>
<h2>Register</h2>
<c:if test="${not empty error}">
    <p style="color:red;"><c:out value="${error}"/></p>
</c:if>
<form action="<%= request.getContextPath() %>/register" method="post">
    <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}'/>" />
    <label>Email: <input type="email" name="email" required></label><br/>
    <label>Password: <input type="password" name="password" required></label><br/>
    <button type="submit">Register</button>
</form>
<a href="<%= request.getContextPath() %>/login">Login</a>
</body>
</html>