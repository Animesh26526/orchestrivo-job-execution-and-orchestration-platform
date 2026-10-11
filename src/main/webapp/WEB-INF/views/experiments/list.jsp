<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
    <title>My Experiments - Orchestrivo</title>
    <style>
        body { font-family: sans-serif; margin: 20px; line-height: 1.5; }
        table { border-collapse: collapse; width: 100%; margin-top: 15px; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f4f4f4; }
        .filter-bar { margin: 15px 0; background: #fafafa; padding: 10px; border: 1px solid #eee; }
        .pagination { margin-top: 15px; }
        .valid { color: #2e7d32; font-weight: bold; }
        .invalid { color: #c62828; font-weight: bold; }
    </style>
</head>
<body>
<h2>My Experiments</h2>
<p><a href="<%= request.getContextPath() %>/dashboard">Dashboard</a> | <a href="<%= request.getContextPath() %>/workloads">My Workloads</a></p>

<div class="filter-bar">
    <form action="<%= request.getContextPath() %>/experiments" method="get">
        <label>Workload: 
            <select name="workloadId">
                <option value="">-- All Workloads --</option>
                <c:forEach var="w" items="${workloads}">
                    <option value="${w.id}" ${selectedWorkloadId == w.id ? 'selected' : ''}><c:out value="${w.name}"/> (#${w.id})</option>
                </c:forEach>
            </select>
        </label>
        <label>Algorithm: 
            <select name="algorithm">
                <option value="">-- All Algorithms --</option>
                <option value="FCFS" ${selectedAlgorithm == 'FCFS' ? 'selected' : ''}>FCFS</option>
                <option value="SJF" ${selectedAlgorithm == 'SJF' ? 'selected' : ''}>SJF</option>
                <option value="SRTF" ${selectedAlgorithm == 'SRTF' ? 'selected' : ''}>SRTF</option>
                <option value="ROUND_ROBIN" ${selectedAlgorithm == 'ROUND_ROBIN' ? 'selected' : ''}>Round Robin</option>
            </select>
        </label>
        <label>Sort: 
            <select name="sortBy">
                <option value="created_at" ${sortBy == 'created_at' ? 'selected' : ''}>Date Created</option>
                <option value="id" ${sortBy == 'id' ? 'selected' : ''}>Run ID</option>
                <option value="total_duration" ${sortBy == 'total_duration' ? 'selected' : ''}>Total Duration</option>
            </select>
        </label>
        <label>Order: 
            <select name="sortDir">
                <option value="DESC" ${sortDir == 'DESC' ? 'selected' : ''}>Descending</option>
                <option value="ASC" ${sortDir == 'ASC' ? 'selected' : ''}>Ascending</option>
            </select>
        </label>
        <button type="submit">Filter</button>
        <a href="<%= request.getContextPath() %>/experiments">Clear</a>
    </form>
</div>

<c:choose>
    <c:when test="${empty experiments}">
        <p><em>No experiment runs found. Run an experiment from a workload details page.</em></p>
    </c:when>
    <c:otherwise>
        <form action="<%= request.getContextPath() %>/experiments/compare" method="get">
            <button type="submit">Compare Selected Runs</button>
            <table>
                <thead>
                    <tr>
                        <th>Select</th>
                        <th>Run ID</th>
                        <th>Workload</th>
                        <th>Algorithm</th>
                        <th>Parameters</th>
                        <th>Status</th>
                        <th>Validation</th>
                        <th>Avg Wait</th>
                        <th>Avg Turnaround</th>
                        <th>Duration</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                <c:forEach var="e" items="${experiments}">
                    <tr>
                        <td><input type="checkbox" name="runId" value="${e.id}"/></td>
                        <td><strong><a href="<%= request.getContextPath() %>/experiments/view?id=${e.id}">#${e.id}</a></strong></td>
                        <td>Workload #${e.workloadId}</td>
                        <td><c:out value="${e.algorithmName}"/></td>
                        <td><small><c:out value="${e.algorithmParameters}"/></small></td>
                        <td><c:out value="${e.runStatus}"/></td>
                        <td>
                            <c:choose>
                                <c:when test="${e.validationStatus == 'VALID'}"><span class="valid">VALID</span></c:when>
                                <c:otherwise><span class="invalid"><c:out value="${e.validationStatus}"/></span></c:otherwise>
                            </c:choose>
                        </td>
                        <td><c:out value="${e.result != null && e.result.aggregateMetrics != null ? e.result.aggregateMetrics.averageWaitingTime : '-'}"/></td>
                        <td><c:out value="${e.result != null && e.result.aggregateMetrics != null ? e.result.aggregateMetrics.averageTurnaroundTime : '-'}"/></td>
                        <td><c:out value="${e.result != null && e.result.aggregateMetrics != null ? e.result.aggregateMetrics.totalDuration : '-'}"/></td>
                        <td>
                            <a href="<%= request.getContextPath() %>/experiments/view?id=${e.id}">View</a> | 
                            <a href="<%= request.getContextPath() %>/experiments/export?id=${e.id}">Export CSV</a>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
            <br/>
            <button type="submit">Compare Selected Runs</button>
        </form>

        <div class="pagination">
            Page ${pagedResult.page} of ${pagedResult.totalPages} (Total: ${pagedResult.totalCount})
            <c:if test="${pagedResult.hasPrevious()}">
                <a href="<%= request.getContextPath() %>/experiments?page=${pagedResult.page - 1}&workloadId=<c:out value='${selectedWorkloadId}'/>&algorithm=<c:out value='${selectedAlgorithm}'/>&sortBy=<c:out value='${sortBy}'/>&sortDir=<c:out value='${sortDir}'/>">&laquo; Previous</a>
            </c:if>
            <c:if test="${pagedResult.hasNext()}">
                <a href="<%= request.getContextPath() %>/experiments?page=${pagedResult.page + 1}&workloadId=<c:out value='${selectedWorkloadId}'/>&algorithm=<c:out value='${selectedAlgorithm}'/>&sortBy=<c:out value='${sortBy}'/>&sortDir=<c:out value='${sortDir}'/>">Next &raquo;</a>
            </c:if>
        </div>
    </c:otherwise>
</c:choose>

</body>
</html>