<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>My Workloads - Orchestrivo</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/app.css">
</head>
<body>

<jsp:include page="/WEB-INF/views/layout/navbar.jsp"/>

<div class="app-container">
    <div class="breadcrumb-bar">
        <a href="<%= request.getContextPath() %>/dashboard">Dashboard</a>
        <span class="breadcrumb-separator">/</span>
        <span>Workloads</span>
    </div>

    <div class="page-header">
        <div class="page-title-group">
            <h1>Workload Management</h1>
            <p class="page-subtitle">Configure, generate, and import process workloads for scheduling evaluation</p>
        </div>
        <div class="page-actions">
            <a href="<%= request.getContextPath() %>/workloads/new" class="btn btn-primary">+ Manual Workload</a>
            <a href="<%= request.getContextPath() %>/workloads/import" class="btn btn-secondary">Import CSV</a>
            <a href="<%= request.getContextPath() %>/workloads/generate" class="btn btn-secondary">Generate Seeded</a>
        </div>
    </div>

    <!-- Filter & Search Bar -->
    <div class="filter-bar">
        <form action="<%= request.getContextPath() %>/workloads" method="get" class="filter-form">
            <div style="flex: 1; min-width: 220px;">
                <label class="form-label" style="font-size: 11px;">Search Name:</label>
                <input type="text" name="search" class="form-input" value="<c:out value='${search}'/>" placeholder="Filter by workload name..."/>
            </div>
            <div>
                <label class="form-label" style="font-size: 11px;">Sort By:</label>
                <select name="sortBy" class="form-select" style="min-width: 140px;">
                    <option value="created_at" ${sortBy == 'created_at' ? 'selected' : ''}>Created Date</option>
                    <option value="name" ${sortBy == 'name' ? 'selected' : ''}>Workload Name</option>
                    <option value="id" ${sortBy == 'id' ? 'selected' : ''}>Workload ID</option>
                </select>
            </div>
            <div>
                <label class="form-label" style="font-size: 11px;">Order:</label>
                <select name="sortDir" class="form-select" style="min-width: 120px;">
                    <option value="DESC" ${sortDir == 'DESC' ? 'selected' : ''}>Descending</option>
                    <option value="ASC" ${sortDir == 'ASC' ? 'selected' : ''}>Ascending</option>
                </select>
            </div>
            <div style="margin-top: 18px;">
                <button type="submit" class="btn btn-secondary">Filter & Sort</button>
                <c:if test="${not empty search}">
                    <a href="<%= request.getContextPath() %>/workloads" class="btn btn-outline" style="margin-left: 6px;">Clear</a>
                </c:if>
            </div>
        </form>
    </div>

    <c:choose>
        <c:when test="${empty workloads}">
            <div class="empty-state">
                <div class="empty-state-icon">📁</div>
                <h3 class="empty-state-title">No workloads found</h3>
                <p class="empty-state-text">Get started by creating a manual process specification, importing an RFC-compliant CSV, or generating a deterministic seeded workload.</p>
                <div class="page-actions" style="justify-content: center;">
                    <a href="<%= request.getContextPath() %>/workloads/new" class="btn btn-primary">Create Workload</a>
                    <a href="<%= request.getContextPath() %>/workloads/import" class="btn btn-secondary">Import CSV</a>
                    <a href="<%= request.getContextPath() %>/workloads/generate" class="btn btn-secondary">Generate Seeded</a>
                </div>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-responsive">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Workload Name</th>
                            <th>Source</th>
                            <th>Processes</th>
                            <th>Created Date</th>
                            <th style="text-align: right;">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="w" items="${workloads}">
                        <tr>
                            <td><strong>#${w.id}</strong></td>
                            <td>
                                <div>
                                    <strong><a href="<%= request.getContextPath() %>/workloads/view?id=${w.id}"><c:out value="${w.name}"/></a></strong>
                                    <c:if test="${not empty w.description}">
                                        <div style="font-size: 12px; color: var(--color-slate-500); margin-top: 2px;">
                                            <c:out value="${w.description}"/>
                                        </div>
                                    </c:if>
                                </div>
                            </td>
                            <td>
                                <span class="badge ${w.sourceType == 'GENERATED' ? 'badge-info' : (w.sourceType == 'CSV_IMPORT' ? 'badge-warning' : 'badge-neutral')}">
                                    <c:out value="${w.sourceType}"/>
                                </span>
                            </td>
                            <td>
                                <span class="badge badge-neutral">
                                    <c:out value="${w.workload != null ? w.workload.processes.size() : 0}"/> processes
                                </span>
                            </td>
                            <td><c:out value="${w.createdAt}"/></td>
                            <td class="actions-cell">
                                <div class="table-actions-group">
                                    <a href="<%= request.getContextPath() %>/workloads/view?id=${w.id}" class="btn btn-sm btn-primary">Run / View</a>
                                    <a href="<%= request.getContextPath() %>/workloads/edit?id=${w.id}" class="btn btn-sm btn-secondary">Edit</a>
                                    <a href="<%= request.getContextPath() %>/workloads/export?id=${w.id}" class="btn btn-sm btn-outline">CSV</a>
                                    <form action="<%= request.getContextPath() %>/workloads/duplicate" method="post" style="display:inline;">
                                        <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}'/>"/>
                                        <input type="hidden" name="id" value="${w.id}"/>
                                        <button type="submit" class="btn btn-sm btn-secondary" onclick="return confirm('Duplicate workload #${w.id}?');">Clone</button>
                                    </form>
                                    <form action="<%= request.getContextPath() %>/workloads/delete" method="post" style="display:inline;">
                                        <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}'/>"/>
                                        <input type="hidden" name="id" value="${w.id}"/>
                                        <button type="submit" class="btn btn-sm btn-outline-danger" onclick="return confirm('Delete workload #${w.id} and all its processes?');">Delete</button>
                                    </form>
                                </div>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>

            <c:if test="${pagedResult.totalPages > 1}">
                <div class="pagination-container">
                    <div>Showing page <strong>${pagedResult.page}</strong> of <strong>${pagedResult.totalPages}</strong> (${pagedResult.totalCount} total workloads)</div>
                    <div class="pagination-links">
                        <c:if test="${pagedResult.hasPrevious()}">
                            <a href="<%= request.getContextPath() %>/workloads?page=${pagedResult.page - 1}&search=<c:out value='${search}'/>&sortBy=<c:out value='${sortBy}'/>&sortDir=<c:out value='${sortDir}'/>" class="page-btn">&laquo; Previous</a>
                        </c:if>
                        <c:if test="${pagedResult.hasNext()}">
                            <a href="<%= request.getContextPath() %>/workloads?page=${pagedResult.page + 1}&search=<c:out value='${search}'/>&sortBy=<c:out value='${sortBy}'/>&sortDir=<c:out value='${sortDir}'/>" class="page-btn">Next &raquo;</a>
                        </c:if>
                    </div>
                </div>
            </c:if>
        </c:otherwise>
    </c:choose>
</div>

<script src="<%= request.getContextPath() %>/assets/js/app.js"></script>
</body>
</html>