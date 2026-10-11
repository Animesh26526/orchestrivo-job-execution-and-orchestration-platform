<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
    <title>My Workloads - Orchestrivo</title>
    <style>
        body { font-family: sans-serif; margin: 20px; line-height: 1.5; }
        table { border-collapse: collapse; width: 100%; margin-top: 15px; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f4f4f4; }
        .actions form { display: inline; margin: 0; }
        .filter-bar { margin: 15px 0; background: #fafafa; padding: 10px; border: 1px solid #eee; }
        .pagination { margin-top: 15px; }
    </style>
</head>
<body>
<h2>My Workloads</h2>
<p>
    <a href="<%= request.getContextPath() %>/dashboard">Dashboard</a> | 
    <a href="<%= request.getContextPath() %>/workloads/new">Create Manual</a> | 
    <a href="<%= request.getContextPath() %>/workloads/import">Import CSV</a> | 
    <a href="<%= request.getContextPath() %>/workloads/generate">Generate Seeded</a>
</p>

<div class="filter-bar">
    <form action="<%= request.getContextPath() %>/workloads" method="get">
        <label>Search Name: <input type="text" name="search" value="<c:out value='${search}'/>" placeholder="Filter by name..."/></label>
        <label>Sort By: 
            <select name="sortBy">
                <option value="created_at" ${sortBy == 'created_at' ? 'selected' : ''}>Created Date</option>
                <option value="name" ${sortBy == 'name' ? 'selected' : ''}>Workload Name</option>
                <option value="id" ${sortBy == 'id' ? 'selected' : ''}>Workload ID</option>
            </select>
        </label>
        <label>Order: 
            <select name="sortDir">
                <option value="DESC" ${sortDir == 'DESC' ? 'selected' : ''}>Descending</option>
                <option value="ASC" ${sortDir == 'ASC' ? 'selected' : ''}>Ascending</option>
            </select>
        </label>
        <button type="submit">Filter & Sort</button>
        <c:if test="${not empty search}">
            <a href="<%= request.getContextPath() %>/workloads">Clear Search</a>
        </c:if>
    </form>
</div>

<c:choose>
    <c:when test="${empty workloads}">
        <p><em>No workloads found matching your criteria. Create, import, or generate a new workload above.</em></p>
    </c:when>
    <c:otherwise>
        <table>
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Name</th>
                    <th>Source</th>
                    <th>Processes</th>
                    <th>Created</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
            <c:forEach var="w" items="${workloads}">
                <tr>
                    <td>${w.id}</td>
                    <td>
                        <strong><a href="<%= request.getContextPath() %>/workloads/view?id=${w.id}"><c:out value="${w.name}"/></a></strong>
                        <c:if test="${not empty w.description}"><br/><small><c:out value="${w.description}"/></small></c:if>
                    </td>
                    <td><c:out value="${w.sourceType}"/></td>
                    <td><c:out value="${w.workload != null ? w.workload.processes.size() : 0}"/></td>
                    <td><c:out value="${w.createdAt}"/></td>
                    <td class="actions">
                        <a href="<%= request.getContextPath() %>/workloads/view?id=${w.id}">View</a> | 
                        <a href="<%= request.getContextPath() %>/workloads/edit?id=${w.id}">Edit</a> | 
                        <a href="<%= request.getContextPath() %>/workloads/export?id=${w.id}">Export CSV</a> | 
                        <form action="<%= request.getContextPath() %>/workloads/duplicate" method="post" style="display:inline;">
                            <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}'/>"/>
                            <input type="hidden" name="id" value="${w.id}"/>
                            <button type="submit" onclick="return confirm('Duplicate this workload?');">Duplicate</button>
                        </form>
                        | 
                        <form action="<%= request.getContextPath() %>/workloads/delete" method="post" style="display:inline;">
                            <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}'/>"/>
                            <input type="hidden" name="id" value="${w.id}"/>
                            <button type="submit" onclick="return confirm('Delete this workload and all its data?');">Delete</button>
                        </form>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>

        <div class="pagination">
            Page ${pagedResult.page} of ${pagedResult.totalPages} (Total: ${pagedResult.totalCount})
            <c:if test="${pagedResult.hasPrevious()}">
                <a href="<%= request.getContextPath() %>/workloads?page=${pagedResult.page - 1}&search=<c:out value='${search}'/>&sortBy=<c:out value='${sortBy}'/>&sortDir=<c:out value='${sortDir}'/>">&laquo; Previous</a>
            </c:if>
            <c:if test="${pagedResult.hasNext()}">
                <a href="<%= request.getContextPath() %>/workloads?page=${pagedResult.page + 1}&search=<c:out value='${search}'/>&sortBy=<c:out value='${sortBy}'/>&sortDir=<c:out value='${sortDir}'/>">Next &raquo;</a>
            </c:if>
        </div>
    </c:otherwise>
</c:choose>

</body>
</html>