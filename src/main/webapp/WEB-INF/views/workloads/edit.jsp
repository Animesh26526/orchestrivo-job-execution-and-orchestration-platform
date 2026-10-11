<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Edit Workload: <c:out value="${workload.name}"/> - Orchestrivo</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/app.css">
</head>
<body>

<jsp:include page="/WEB-INF/views/layout/navbar.jsp"/>

<div class="app-container">
    <div class="breadcrumb-bar">
        <a href="<%= request.getContextPath() %>/dashboard">Dashboard</a>
        <span class="breadcrumb-separator">/</span>
        <a href="<%= request.getContextPath() %>/workloads">Workloads</a>
        <span class="breadcrumb-separator">/</span>
        <a href="<%= request.getContextPath() %>/workloads/view?id=${workload.id}">Workload #${workload.id}</a>
        <span class="breadcrumb-separator">/</span>
        <span>Edit</span>
    </div>

    <div class="page-header">
        <div class="page-title-group">
            <h1>Edit Workload: <c:out value="${workload.name}"/></h1>
            <p class="page-subtitle">Update metadata and modify process specifications</p>
        </div>
        <div class="page-actions">
            <a href="<%= request.getContextPath() %>/workloads/view?id=${workload.id}" class="btn btn-secondary">&larr; Cancel & View</a>
        </div>
    </div>

    <c:if test="${not empty error}">
        <div class="alert alert-danger">
            <c:out value="${error}"/>
        </div>
    </c:if>

    <div class="card" style="max-width: 800px;">
        <form action="<%= request.getContextPath() %>/workloads/edit" method="post">
            <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}'/>"/>
            <input type="hidden" name="id" value="${workload.id}"/>

            <div class="form-group">
                <label class="form-label" for="editName">Workload Name</label>
                <input type="text" id="editName" name="name" class="form-input" value="<c:out value='${workload.name}'/>" required>
            </div>

            <div class="form-group">
                <label class="form-label" for="editDesc">Description</label>
                <textarea id="editDesc" name="description" class="form-textarea" rows="2"><c:out value="${workload.description}"/></textarea>
            </div>

            <div class="card-header" style="margin-top: 24px; padding-bottom: 10px;">
                <div>
                    <h2 class="card-title">Process Specifications</h2>
                    <p class="card-description">Arrival times must be &ge; 0. Burst times must be &gt; 0. Process IDs must be unique.</p>
                </div>
                <button type="button" class="btn btn-sm btn-secondary" onclick="addProcessRow('processTable', 'P')">+ Add Process Row</button>
            </div>

            <div class="table-responsive" style="box-shadow: none; border: 1px solid var(--color-slate-200); margin-bottom: 16px;">
                <table class="data-table" id="processTable">
                    <thead>
                        <tr>
                            <th style="width: 28%;">Process ID</th>
                            <th style="width: 28%;">Arrival Time</th>
                            <th style="width: 28%;">Burst Time</th>
                            <th style="text-align: right;">Action</th>
                        </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="p" items="${workload.workload.processes}">
                        <tr>
                            <td><input type="text" name="processId" class="form-input" value="<c:out value='${p.processId}'/>" required style="font-family: monospace; font-weight: 600;" /></td>
                            <td><input type="number" name="arrivalTime" class="form-input" value="${p.arrivalTime}" min="0" required /></td>
                            <td><input type="number" name="burstTime" class="form-input" value="${p.burstTime}" min="1" required /></td>
                            <td class="actions-cell">
                                <button type="button" class="btn btn-outline-danger btn-sm" onclick="removeProcessRow(this)">Remove</button>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>

            <div style="margin-top: 20px; display: flex; gap: 10px;">
                <button type="submit" class="btn btn-primary">Save Changes</button>
                <a href="<%= request.getContextPath() %>/workloads/view?id=${workload.id}" class="btn btn-secondary">Cancel</a>
            </div>
        </form>
    </div>
</div>

<script src="<%= request.getContextPath() %>/assets/js/app.js"></script>
</body>
</html>
