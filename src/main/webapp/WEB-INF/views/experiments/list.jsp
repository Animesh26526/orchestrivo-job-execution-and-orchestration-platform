<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<html>
<head><title>My Experiments</title></head>
<body>
<h2>My Experiments</h2>
<a href="<%= request.getContextPath() %>/dashboard">Back</a>
<ul>
<c:forEach var="e" items="${experiments}">
    <li>
        <a href="<%= request.getContextPath() %>/experiments/view?id=${e.id}">
            Run ${e.id} - <c:out value="${e.algorithmName}"/> (<c:out value="${e.runStatus}"/>)
        </a>
    </li>
</c:forEach>
</ul>
</body>
</html>