<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Micro-Benchmarks - Orchestrivo</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/app.css">
</head>
<body>

<jsp:include page="/WEB-INF/views/layout/navbar.jsp"/>

<div class="app-container">
    <div class="breadcrumb-bar">
        <a href="<%= request.getContextPath() %>/dashboard">Dashboard</a>
        <span class="breadcrumb-separator">/</span>
        <span>Algorithm Benchmarks</span>
    </div>

    <div class="page-header">
        <div class="page-title-group">
            <h1>Algorithm Engine Micro-Benchmarks</h1>
            <p class="page-subtitle">Compare recorded algorithm execution latencies across synthetic workloads and iterations</p>
        </div>
        <div class="page-actions">
            <a href="<%= request.getContextPath() %>/benchmarks/run" class="btn btn-primary">+ Run New Benchmark</a>
        </div>
    </div>

    <c:choose>
        <c:when test="${empty benchmarks}">
            <div class="empty-state">
                <div class="empty-state-icon">⚡</div>
                <h3 class="empty-state-title">No micro-benchmarks executed yet</h3>
                <p class="empty-state-text">Run high-iteration statistical timing benchmarks on FCFS, SJF, SRTF, or Round Robin to profile engine host latency.</p>
                <a href="<%= request.getContextPath() %>/benchmarks/run" class="btn btn-primary">Run First Benchmark</a>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-responsive">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Benchmark ID</th>
                            <th>Algorithm</th>
                            <th>Process Count</th>
                            <th>Warmup / Measured</th>
                            <th>Median (&micro;s)</th>
                            <th>Min (&micro;s)</th>
                            <th>Max (&micro;s)</th>
                            <th>Std Dev (&micro;s)</th>
                            <th>Validation</th>
                            <th>Date</th>
                            <th style="text-align: right;">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="b" items="${benchmarks}">
                        <tr>
                            <td><strong><a href="<%= request.getContextPath() %>/benchmarks/view?id=${b.id}">#${b.id}</a></strong></td>
                            <td><strong><c:out value="${b.algorithmName}"/></strong></td>
                            <td>${b.processCount}</td>
                            <td>${b.warmupIterations} / ${b.measuredIterations}</td>
                            <td><strong>${b.medianMicros} &micro;s</strong></td>
                            <td>${b.minMicros} &micro;s</td>
                            <td>${b.maxMicros} &micro;s</td>
                            <td>${b.stdDevMicros} &micro;s</td>
                            <td>
                                <span class="badge ${b.validationPassed ? 'badge-success' : 'badge-danger'}">
                                    ${b.validationPassed ? 'PASSED' : 'FAILED'}
                                </span>
                            </td>
                            <td><c:out value="${b.createdAt}"/></td>
                            <td class="actions-cell">
                                <div class="table-actions-group">
                                    <a href="<%= request.getContextPath() %>/benchmarks/view?id=${b.id}" class="btn btn-sm btn-secondary">Report</a>
                                    <form action="<%= request.getContextPath() %>/benchmarks/delete" method="post" style="display:inline;">
                                        <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}'/>"/>
                                        <input type="hidden" name="id" value="${b.id}"/>
                                        <button type="submit" class="btn btn-sm btn-outline-danger" onclick="return confirm('Delete benchmark run #${b.id}?');">Delete</button>
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
                    <div>Showing page <strong>${pagedResult.page}</strong> of <strong>${pagedResult.totalPages}</strong> (${pagedResult.totalCount} total benchmarks)</div>
                    <div class="pagination-links">
                        <c:if test="${pagedResult.hasPrevious()}">
                            <a href="<%= request.getContextPath() %>/benchmarks?page=${pagedResult.page - 1}&pageSize=${pagedResult.pageSize}" class="page-btn">&laquo; Previous</a>
                        </c:if>
                        <c:if test="${pagedResult.hasNext()}">
                            <a href="<%= request.getContextPath() %>/benchmarks?page=${pagedResult.page + 1}&pageSize=${pagedResult.pageSize}" class="page-btn">Next &raquo;</a>
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
