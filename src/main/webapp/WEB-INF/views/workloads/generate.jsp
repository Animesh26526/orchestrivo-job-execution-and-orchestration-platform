<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Generate Seeded Workload - Orchestrivo</title>
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
        <span>Generate Seeded Workload</span>
    </div>

    <div class="page-header">
        <div class="page-title-group">
            <h1>Deterministic Workload Generator</h1>
            <p class="page-subtitle">Generate synthetic process workloads using seeded pseudo-random numbers</p>
        </div>
        <div class="page-actions">
            <a href="<%= request.getContextPath() %>/workloads" class="btn btn-secondary">&larr; Back to Workloads</a>
        </div>
    </div>

    <div class="alert alert-info">
        <div>
            <strong>Reproducible Generation Protocol:</strong>
            <p style="margin-top: 4px;">
                Using a fixed random seed ensures identical process arrival and burst distributions across any execution host. This provides mathematical reproducibility for scientific benchmarking and algorithm comparison.
            </p>
        </div>
    </div>

    <c:if test="${not empty error}">
        <div class="alert alert-danger">
            <c:out value="${error}"/>
        </div>
    </c:if>

    <div class="card" style="max-width: 700px;">
        <form action="<%= request.getContextPath() %>/workloads/generate" method="post">
            <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}'/>"/>

            <div class="form-group">
                <label class="form-label" for="genName">Workload Name</label>
                <input type="text" id="genName" name="name" class="form-input" value="Synthetic Seeded Workload" required>
            </div>

            <div class="form-grid-2">
                <div class="form-group">
                    <label class="form-label" for="processCount">Process Count (1 - 200)</label>
                    <input type="number" id="processCount" name="processCount" class="form-input" value="10" min="1" max="200" required>
                    <div class="form-hint">Number of processes to generate</div>
                </div>

                <div class="form-group">
                    <label class="form-label" for="seed">
                        Pseudo-Random Seed <span style="font-weight: normal; color: var(--color-slate-500);">(Optional)</span>
                    </label>
                    <div style="display: flex; gap: 8px;">
                        <input type="number" id="seed" name="seed" class="form-input" value="<c:out value='${param.seed}'/>" placeholder="Leave empty to auto-generate">
                        <button type="button" class="btn btn-secondary" onclick="document.getElementById('seed').value = Math.floor(Math.random() * 9000000000) + 1000000000;" title="Generate random seed" style="white-space: nowrap; padding: 0 10px;">🎲 Random</button>
                    </div>
                    <div class="form-hint">Leave blank to automatically generate a cryptographic seed, or enter a number for exact reproducibility.</div>
                </div>
            </div>

            <div class="form-grid-2">
                <div class="form-group">
                    <label class="form-label" for="minArrival">Min Arrival Time (&ge; 0)</label>
                    <input type="number" id="minArrival" name="minArrival" class="form-input" value="0" min="0" required>
                </div>
                <div class="form-group">
                    <label class="form-label" for="maxArrival">Max Arrival Time</label>
                    <input type="number" id="maxArrival" name="maxArrival" class="form-input" value="15" min="0" required>
                </div>
            </div>

            <div class="form-grid-2">
                <div class="form-group">
                    <label class="form-label" for="minBurst">Min Burst Duration (&ge; 1)</label>
                    <input type="number" id="minBurst" name="minBurst" class="form-input" value="1" min="1" required>
                </div>
                <div class="form-group">
                    <label class="form-label" for="maxBurst">Max Burst Duration</label>
                    <input type="number" id="maxBurst" name="maxBurst" class="form-input" value="10" min="1" required>
                </div>
            </div>

            <div style="margin-top: 24px; display: flex; gap: 10px;">
                <button type="submit" class="btn btn-primary">Generate Workload</button>
                <a href="<%= request.getContextPath() %>/workloads" class="btn btn-secondary">Cancel</a>
            </div>
        </form>
    </div>
</div>

<script src="<%= request.getContextPath() %>/assets/js/app.js"></script>
</body>
</html>
