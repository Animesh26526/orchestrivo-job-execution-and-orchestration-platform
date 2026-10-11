<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Import CSV Workload - Orchestrivo</title>
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
        <span>Import CSV</span>
    </div>

    <div class="page-header">
        <div class="page-title-group">
            <h1>Import Workload from CSV</h1>
            <p class="page-subtitle">Upload an RFC-compliant CSV file or paste tabular process specifications directly</p>
        </div>
        <div class="page-actions">
            <a href="<%= request.getContextPath() %>/workloads" class="btn btn-secondary">&larr; Back to Workloads</a>
        </div>
    </div>

    <c:if test="${not empty error}">
        <div class="alert alert-danger">
            <c:out value="${error}"/>
        </div>
    </c:if>

    <c:if test="${not empty errors}">
        <div class="alert alert-danger" style="display: block;">
            <strong>Validation Errors Found:</strong>
            <ul style="margin-top: 8px; margin-left: 20px;">
                <c:forEach var="e" items="${errors}">
                    <li><c:out value="${e}"/></li>
                </c:forEach>
            </ul>
        </div>
    </c:if>

    <div class="card" style="margin-bottom: 24px;">
        <div class="card-header">
            <div>
                <h2 class="card-title">Expected CSV Specification</h2>
                <p class="card-description">Headers required. Maximum 500 rows. Process IDs must be unique. Arrival times &ge; 0. Burst times &gt; 0.</p>
            </div>
        </div>
        <div class="code-box">
process_id,arrival_time,burst_time
P1,0,5
P2,1,3
P3,2,1
P4,4,2
        </div>
    </div>

    <div class="card" style="max-width: 800px;">
        <form action="<%= request.getContextPath() %>/workloads/import" method="post" enctype="multipart/form-data">
            <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}'/>"/>

            <div class="form-group">
                <label class="form-label" for="importName">Workload Name</label>
                <input type="text" id="importName" name="name" class="form-input" value="<c:out value='${name}'/>" placeholder="e.g. Production Trace Sample" required>
            </div>

            <div class="form-group">
                <label class="form-label" for="importDesc">Description (Optional)</label>
                <textarea id="importDesc" name="description" class="form-textarea" rows="2" placeholder="Describe the source dataset..."><c:out value="${description}"/></textarea>
            </div>

            <div class="card" style="background-color: var(--color-slate-50); border: 1px dashed var(--color-slate-300); margin: 20px 0;">
                <div class="form-group" style="margin-bottom: 0;">
                    <label class="form-label" for="csvFile">Option A: Upload CSV File (.csv)</label>
                    <input type="file" id="csvFile" name="csvFile" class="form-input" accept=".csv,text/csv">
                    <div class="form-hint">RFC 4180 compliant CSV file up to 1 MB</div>
                </div>
            </div>

            <div class="form-group">
                <label class="form-label" for="csvContent">Option B: Or Paste CSV Text Directly</label>
                <textarea id="csvContent" name="csvContent" class="form-textarea" rows="6" style="font-family: var(--font-mono); font-size: 12px;" placeholder="process_id,arrival_time,burst_time&#10;P1,0,5&#10;P2,1,3"></textarea>
                <div class="form-hint">Used if no file is selected above</div>
            </div>

            <div style="margin-top: 24px; display: flex; gap: 10px;">
                <button type="submit" class="btn btn-primary">Import Workload</button>
                <a href="<%= request.getContextPath() %>/workloads" class="btn btn-secondary">Cancel</a>
            </div>
        </form>
    </div>
</div>

<script src="<%= request.getContextPath() %>/assets/js/app.js"></script>
</body>
</html>
