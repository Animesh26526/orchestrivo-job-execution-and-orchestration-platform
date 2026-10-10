<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<html>
<head><title>My Workloads</title></head>
<body>
<h2>My Workloads</h2>
<a href="<%= request.getContextPath() %>/dashboard">Back</a> | 
<a href="<%= request.getContextPath() %>/workloads/new">Create New</a>
<ul>
<c:forEach var="w" items="${workloads}">
    <li>
        <a href="<%= request.getContextPath() %>/workloads/view?id=${w.id}"><c:out value="${w.name}"/></a>
        <form action="<%= request.getContextPath() %>/workloads/delete" method="post" style="display:inline;">
            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}"/>
            <input type="hidden" name="id" value="${w.id}"/>
            <button type="submit">Delete</button>
        </form>
    </li>
</c:forEach>
</ul>
</body>
</html>