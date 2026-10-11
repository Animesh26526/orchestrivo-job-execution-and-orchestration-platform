<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
    <title>Micro-Benchmarks - Orchestrivo</title>
    <style>
        body { font-family: sans-serif; margin: 20px; line-height: 1.5; color: #333; }
        .nav { margin-bottom: 15px; }
        .nav a { margin-right: 15px; text-decoration: none; color: #0066cc; }
        .btn { display: inline-block; padding: 6px 14px; background: #0066cc; color: #fff; text-decoration: none; border-radius: 4px; font-size: 0.9em; border: none; cursor: pointer; }
        .btn:hover { background: #0052a3; }
        .btn-danger { background: #c5221f; }
        .btn-danger:hover { background: #a51d1a; }
        table { border-collapse: collapse; width: 100%; max-width: 1000px; margin-top: 15px; }
        th, td { border: 1px solid #ddd; padding: 8px 12px; text-align: left; }
        th { background: #f4f6f8; font-weight: 600; }
        tr:nth-child(even) { background: #fafbfc; }
        .badge { display: inline-block; padding: 2px 8px; border-radius: 3px; font-size: 0.85em; font-weight: bold; }
        .badge-success { background: #e6f4ea; color: #137333; }
        .badge-danger { background: #fce8e6; color: #c5221f; }
        .note { font-size: 0.9em; color: #555; background: #f0f4f8; border-left: 4px solid #1a73e8; padding: 10px 14px; margin: 15px 0; max-width: 1000px; }
        .pagination { margin-top: 15px; }
        .pagination a, .pagination span { margin-right: 8px; text-decoration: none; padding: 4px 8px; border: 1px solid #ccc; border-radius: 3px; }
    </style>
</head>
<body>

<div class="nav">
    <a href="<%= request.getContextPath() %>/dashboard">&larr; Back to Dashboard</a>
    <a href="<%= request.getContextPath() %>/workloads">Workloads</a>
    <a href="<%= request.getContextPath() %>/experiments">Experiments</a>
</div>

<div style="display: flex; justify-content: space-between; align-items: center; max-width: 1000px;">
    <h2>Algorithm Engine Micro-Benchmarks</h2>
    <a class="btn" href="<%= request.getContextPath() %>/benchmarks/run">+ Run New Benchmark</a>
</div>

<div class="note">
    <strong>Micro-Benchmarking Methodology:</strong> Wall-clock execution timing is captured via <code>System.nanoTime()</code> across repeated measured iterations following untimed JIT warm-up cycles. Output schedules are independently validated for algorithmic correctness on every run.
    <br/><em>Note: Engine execution timing measures host simulation latency, which is strictly distinct from simulated scheduling domain metrics.</em>
</div>

<table>
    <thead>
        <tr>
            <th>ID</th>
            <th>Algorithm</th>
            <th>Processes</th>
            <th>Warmup / Measured</th>
            <th>Median (µs)</th>
            <th>Min (µs)</th>
            <th>Max (µs)</th>
            <th>Std Dev (µs)</th>
            <th>Validation</th>
            <th>Date</th>
            <th>Actions</th>
        </tr>
    </thead>
    <tbody>
    <c:forEach var="b" items="${benchmarks}">
        <tr>
            <td>#${b.id}</td>
            <td><strong><c:out value="${b.algorithmName}"/></strong></td>
            <td>${b.processCount}</td>
            <td>${b.warmupIterations} / ${b.measuredIterations}</td>
            <td><strong>${b.medianMicros}</strong></td>
            <td>${b.minMicros}</td>
            <td>${b.maxMicros}</td>
            <td>${b.stdDevMicros}</td>
            <td>
                <span class="badge ${b.validationPassed ? 'badge-success' : 'badge-danger'}">
                    ${b.validationPassed ? 'PASSED' : 'FAILED'}
                </span>
            </td>
            <td><c:out value="${b.createdAt}"/></td>
            <td>
                <a href="<%= request.getContextPath() %>/benchmarks/view?id=${b.id}">View</a>
                |
                <form action="<%= request.getContextPath() %>/benchmarks/delete" method="post" style="display:inline;" onsubmit="return confirm('Delete benchmark #${b.id}?');">
                    <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}'/>"/>
                    <input type="hidden" name="id" value="${b.id}"/>
                    <button type="submit" style="background:none; border:none; color:#c5221f; cursor:pointer; text-decoration:underline; padding:0; font-family:inherit;">Delete</button>
                </form>
            </td>
        </tr>
    </c:forEach>
    <c:if test="${empty benchmarks}">
        <tr>
            <td colspan="11" style="text-align: center; color: #888; padding: 20px;">No benchmarks executed yet.</td>
        </tr>
    </c:if>
    </tbody>
</table>

<c:if test="${pagedResult.totalPages > 1}">
    <div class="pagination">
        <c:if test="${pagedResult.page > 1}">
            <a href="?page=${pagedResult.page - 1}&pageSize=${pagedResult.pageSize}">&laquo; Previous</a>
        </c:if>
        <span>Page ${pagedResult.page} of ${pagedResult.totalPages} (Total: ${pagedResult.totalItems})</span>
        <c:if test="${pagedResult.page < pagedResult.totalPages}">
            <a href="?page=${pagedResult.page + 1}&pageSize=${pagedResult.pageSize}">Next &raquo;</a>
        </c:if>
    </div>
</c:if>

</body>
</html>
