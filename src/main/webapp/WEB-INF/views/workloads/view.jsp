<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Workload: <c:out value="${workload.name}"/> - Orchestrivo</title>
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
        <span>Workload #${workload.id}</span>
    </div>

    <div class="page-header">
        <div class="page-title-group">
            <h1>Workload: <c:out value="${workload.name}"/></h1>
            <p class="page-subtitle"><c:out value="${workload.description}"/></p>
        </div>
        <div class="page-actions">
            <a href="<%= request.getContextPath() %>/workloads" class="btn btn-secondary">&larr; Back to Workloads</a>
            <a href="<%= request.getContextPath() %>/jobs/batch?workloadId=${workload.id}" class="btn btn-primary">&boxplus; Batch Run Algorithms</a>
            <a href="<%= request.getContextPath() %>/experiments/new?workloadId=${workload.id}" class="btn btn-secondary">Run Single Algorithm</a>
            <a href="<%= request.getContextPath() %>/workloads/edit?id=${workload.id}" class="btn btn-secondary">Edit Workload</a>
            <a href="<%= request.getContextPath() %>/workloads/export?id=${workload.id}" class="btn btn-outline">Export CSV</a>
        </div>
    </div>

    <!-- Metadata Card -->
    <div class="card">
        <div class="card-header">
            <h2 class="card-title">Specification Summary</h2>
            <span class="badge ${workload.sourceType == 'GENERATED' ? 'badge-info' : (workload.sourceType == 'CSV_IMPORT' ? 'badge-warning' : 'badge-neutral')}">
                <c:out value="${workload.sourceType}"/>
            </span>
        </div>
        <div class="table-responsive" style="margin: 0; box-shadow: none; border: none;">
            <table class="data-table">
                <tbody>
                    <tr>
                        <td style="width: 200px; font-weight: 600;">Workload ID:</td>
                        <td>#${workload.id}</td>
                    </tr>
                    <tr>
                        <td style="font-weight: 600;">Description:</td>
                        <td><c:out value="${workload.description}"/></td>
                    </tr>
                    <tr>
                        <td style="font-weight: 600;">Source Type:</td>
                        <td>
                            <c:out value="${workload.sourceType}"/>
                            <c:if test="${not empty workload.generationSeed}">
                                &bull; Deterministic Seed: <code id="seedDisplay">${workload.generationSeed}</code>
                                <button type="button" class="btn btn-secondary" style="font-size: 11px; padding: 2px 8px; margin-left: 6px;" onclick="navigator.clipboard.writeText('${workload.generationSeed}').then(() => alert('Seed copied to clipboard!'))">📋 Copy</button>
                                <a href="<%= request.getContextPath() %>/workloads/generate?seed=${workload.generationSeed}" class="btn btn-secondary" style="font-size: 11px; padding: 2px 8px; margin-left: 4px;">🔄 Reproduce</a>
                            </c:if>
                        </td>
                    </tr>
                    <tr>
                        <td style="font-weight: 600;">Created Timestamp:</td>
                        <td><c:out value="${workload.createdAt}"/></td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>

    <div class="form-grid-2">
        <!-- Process Table -->
        <div class="card" style="margin-bottom: 0;">
            <div class="card-header">
                <div>
                    <h2 class="card-title">Process Specifications</h2>
                    <p class="card-description">
                        <c:out value="${workload.workload != null ? workload.workload.processes.size() : 0}"/> active processes
                    </p>
                </div>
            </div>
            <div class="table-responsive" style="margin: 0; box-shadow: none; border: none; max-height: 400px; overflow-y: auto;">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Process ID</th>
                            <th>Arrival Time</th>
                            <th>Burst Time</th>
                        </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="p" items="${workload.workload.processes}">
                        <tr>
                            <td><strong><code><c:out value="${p.processId}"/></code></strong></td>
                            <td>${p.arrivalTime}</td>
                            <td>${p.burstTime}</td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty workload.workload.processes}">
                        <tr>
                            <td colspan="3" style="text-align: center; color: #94a3b8; padding: 20px;">No processes found in this workload.</td>
                        </tr>
                    </c:if>
                    </tbody>
                </table>
            </div>
        </div>

        <!-- Launch Simulation Card -->
        <div class="card" style="margin-bottom: 0;">
            <div class="card-header">
                <div>
                    <h2 class="card-title">Run Scheduling Experiment</h2>
                    <p class="card-description">Select algorithm to dispatch background execution worker</p>
                </div>
            </div>
            <form action="<%= request.getContextPath() %>/experiments/run" method="post">
                <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}'/>"/>
                <input type="hidden" name="workloadId" value="${workload.id}"/>

                <div class="form-group">
                    <label class="form-label" for="algorithmName">Scheduling Algorithm:</label>
                    <select id="algorithmName" name="algorithmName" class="form-select" required>
                        <option value="FCFS">First-Come, First-Served (FCFS)</option>
                        <option value="SJF">Shortest Job First (SJF - Non-preemptive)</option>
                        <option value="SRTF">Shortest Remaining Time First (SRTF - Preemptive)</option>
                        <option value="ROUND_ROBIN">Round Robin (RR)</option>
                    </select>
                </div>

                <div class="form-group" id="quantumGroup" style="display: none;">
                    <label class="form-label" for="quantumInput">Time Quantum (for Round Robin):</label>
                    <input type="number" id="quantumInput" name="quantum" class="form-input" value="3" min="1" max="100"/>
                    <div class="form-hint">Time slice allocated to each process per round</div>
                </div>

                <div style="margin-top: 24px;">
                    <button type="submit" class="btn btn-primary" style="width: 100%;">
                        Dispatch Experiment to Engine &rarr;
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

<script src="<%= request.getContextPath() %>/assets/js/app.js"></script>
</body>
</html>