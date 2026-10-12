<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Batch Algorithm Execution - Orchestrivo</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/app.css">
</head>
<body>

<jsp:include page="/WEB-INF/views/layout/navbar.jsp"/>

<div class="app-container">
    <div class="breadcrumb-bar">
        <a href="<%= request.getContextPath() %>/dashboard">Dashboard</a>
        <span class="breadcrumb-separator">/</span>
        <a href="<%= request.getContextPath() %>/jobs">Queue & Jobs</a>
        <span class="breadcrumb-separator">/</span>
        <span>Batch Execution</span>
    </div>

    <div class="page-header">
        <div class="page-title-group">
            <h1>Batch Algorithm Execution</h1>
            <p class="page-subtitle">Enqueue multiple scheduling algorithms against a common workload snapshot in a single atomic dispatch</p>
        </div>
        <div class="page-actions">
            <a href="<%= request.getContextPath() %>/jobs" class="btn btn-secondary">&larr; Back to Queue</a>
        </div>
    </div>

    <c:if test="${not empty error}">
        <div class="alert alert-danger" style="margin-bottom: 24px;">
            <div>
                <strong>Batch Submission Failed:</strong>
                <p style="margin-top: 4px;"><c:out value="${error}"/></p>
            </div>
        </div>
    </c:if>

    <c:if test="${not empty batchJobs}">
        <div class="card" style="margin-bottom: 24px; border-left: 4px solid #3b82f6;">
            <div class="card-header">
                <div>
                    <h2 class="card-title">&boxplus; Batch Execution: <code><c:out value="${batchId}"/></code></h2>
                    <p class="card-description">${batchJobs.size()} simulation jobs enqueued for execution across worker threads</p>
                </div>
                <div>
                    <a href="<%= request.getContextPath() %>/jobs/batch?batchId=${batchId}" class="btn btn-sm btn-outline" id="refreshBatchBtn">&#x21bb; Refresh Status</a>
                </div>
            </div>
            <div class="table-responsive" style="margin: 0; box-shadow: none; border: none;">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Job ID</th>
                            <th>Algorithm</th>
                            <th>Parameters</th>
                            <th>Status</th>
                            <th>Attempts</th>
                            <th>Wait Time</th>
                            <th>Duration (High-Res)</th>
                            <th style="text-align: right;">Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:set var="anyPending" value="false"/>
                        <c:forEach var="bj" items="${batchJobs}">
                            <c:if test="${bj.status == 'QUEUED' or bj.status == 'RUNNING'}">
                                <c:set var="anyPending" value="true"/>
                            </c:if>
                            <tr>
                                <td><strong><a href="<%= request.getContextPath() %>/jobs/view?id=${bj.id}">#${bj.id}</a></strong></td>
                                <td><strong><c:out value="${bj.algorithmName}"/></strong></td>
                                <td><c:out value="${bj.formattedParameters}"/></td>
                                <td>
                                    <span class="badge ${bj.status == 'COMPLETED' ? 'badge-success' : (bj.status == 'RUNNING' ? 'badge-primary' : (bj.status == 'QUEUED' ? 'badge-warning' : 'badge-danger'))}">
                                        <c:out value="${bj.status}"/>
                                    </span>
                                </td>
                                <td>${bj.attemptCount} / ${bj.maxAttempts}</td>
                                <td>
                                    <c:choose>
                                        <c:when test="${bj.queueWaitMs != null}">${bj.queueWaitMs} ms</c:when>
                                        <c:otherwise><span style="color:#94a3b8;">-</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${bj.executionDurationNanos != null or bj.executionDurationMs != null}">
                                            <c:out value="${bj.formattedDuration}"/>
                                        </c:when>
                                        <c:otherwise><span style="color:#94a3b8;">Pending</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td style="text-align: right;">
                                    <c:choose>
                                        <c:when test="${bj.experimentRunId != null}">
                                            <a href="<%= request.getContextPath() %>/experiments/view?id=${bj.experimentRunId}" class="btn btn-sm btn-primary">
                                                View Results &rarr;
                                            </a>
                                        </c:when>
                                        <c:otherwise>
                                            <a href="<%= request.getContextPath() %>/jobs/view?id=${bj.id}" class="btn btn-sm btn-outline">
                                                Inspect
                                            </a>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
            <c:if test="${anyPending}">
                <div style="padding: 12px 20px; background: #f8fafc; border-top: 1px solid #e2e8f0; font-size: 13px; color: #64748b; display: flex; align-items: center; justify-content: space-between;">
                    <span><span class="spinner" style="display:inline-block; vertical-align:middle; margin-right:6px;"></span> Background workers are actively processing this batch. This table automatically refreshes every 2.5 seconds.</span>
                    <span id="pollCounter">Auto-refresh active</span>
                </div>
                <script>
                    setTimeout(function() {
                        window.location.reload();
                    }, 2500);
                </script>
            </c:if>
        </div>
    </c:if>

    <div class="card" style="max-width: 800px;">
        <div class="card-header">
            <div>
                <h2 class="card-title">Configure New Batch Dispatch</h2>
                <p class="card-description">Select the target workload and the suite of scheduling algorithms to execute concurrently</p>
            </div>
        </div>

        <form action="<%= request.getContextPath() %>/jobs/batch" method="post" id="batchForm">
            <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}'/>" />

            <div class="form-group" style="margin-bottom: 20px;">
                <label for="workloadSelect" class="form-label" style="font-weight: 600;">Target Workload *</label>
                <select name="workloadId" id="workloadSelect" class="form-select" required style="width: 100%;">
                    <option value="" disabled ${empty selectedWorkload and empty selectedWorkloadId ? 'selected' : ''}>-- Choose a workload --</option>
                    <c:forEach var="w" items="${workloads}">
                        <option value="${w.id}" ${(selectedWorkload != null && selectedWorkload.id == w.id) || (selectedWorkloadId != null && selectedWorkloadId == w.id) ? 'selected' : ''}>
                            <c:out value="${w.name}"/> (<c:out value="${w.processCount}"/> processes, <c:out value="${w.sourceType}"/>)
                        </option>
                    </c:forEach>
                </select>
                <div class="form-help" style="margin-top: 4px; font-size: 12px; color: #64748b;">
                    All selected algorithms will be executed against an identical, immutable snapshot of this workload's process specifications.
                </div>
            </div>

            <div class="form-group" style="margin-bottom: 20px;">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                    <label class="form-label" style="font-weight: 600; margin: 0;">Select Scheduling Algorithms *</label>
                    <button type="button" class="btn btn-sm btn-outline" id="selectAllBtn" onclick="toggleSelectAll()">Select All (Complete Suite)</button>
                </div>

                <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 12px; margin-top: 8px;">
                    <label style="display: flex; align-items: flex-start; gap: 10px; padding: 12px; border: 1px solid #e2e8f0; border-radius: 6px; cursor: pointer; background: #fafafa;">
                        <input type="checkbox" name="algorithms" value="FCFS" class="algo-checkbox" checked style="margin-top: 3px;">
                        <div>
                            <strong>FCFS (First-Come, First-Served)</strong>
                            <div style="font-size: 12px; color: #64748b;">Standard non-preemptive baseline algorithm</div>
                        </div>
                    </label>

                    <label style="display: flex; align-items: flex-start; gap: 10px; padding: 12px; border: 1px solid #e2e8f0; border-radius: 6px; cursor: pointer; background: #fafafa;">
                        <input type="checkbox" name="algorithms" value="SJF" class="algo-checkbox" checked style="margin-top: 3px;">
                        <div>
                            <strong>SJF (Shortest Job First)</strong>
                            <div style="font-size: 12px; color: #64748b;">Non-preemptive shortest burst optimization</div>
                        </div>
                    </label>

                    <label style="display: flex; align-items: flex-start; gap: 10px; padding: 12px; border: 1px solid #e2e8f0; border-radius: 6px; cursor: pointer; background: #fafafa;">
                        <input type="checkbox" name="algorithms" value="SRTF" class="algo-checkbox" checked style="margin-top: 3px;">
                        <div>
                            <strong>SRTF (Shortest Remaining Time First)</strong>
                            <div style="font-size: 12px; color: #64748b;">Preemptive shortest remaining time optimization</div>
                        </div>
                    </label>

                    <label style="display: flex; align-items: flex-start; gap: 10px; padding: 12px; border: 1px solid #e2e8f0; border-radius: 6px; cursor: pointer; background: #fafafa;">
                        <input type="checkbox" name="algorithms" value="ROUND_ROBIN" class="algo-checkbox" checked id="rrCheckbox" onchange="toggleRrQuantum()" style="margin-top: 3px;">
                        <div style="flex: 1;">
                            <strong>Round Robin (Preemptive)</strong>
                            <div style="font-size: 12px; color: #64748b;">Time-sliced fair execution</div>
                            <div id="rrQuantumGroup" style="margin-top: 8px;">
                                <label for="quantumInput" style="font-size: 11px; font-weight: 600; color: #475569;">Time Quantum (ticks):</label>
                                <input type="number" name="quantum" id="quantumInput" value="3" min="1" max="100" class="form-input" style="width: 80px; padding: 4px 8px; font-size: 13px;">
                            </div>
                        </div>
                    </label>
                </div>
            </div>

            <div style="background: #f1f5f9; padding: 14px; border-radius: 6px; margin-bottom: 24px; font-size: 13px; color: #475569;">
                <strong>Batch Constraints:</strong> Maximum 20 jobs per batch. Submission is atomically validated before persistence. All algorithm outputs are fully verified and produce persistent Gantt charts.
            </div>

            <div style="display: flex; gap: 12px; justify-content: flex-end;">
                <a href="<%= request.getContextPath() %>/jobs" class="btn btn-secondary">Cancel</a>
                <button type="submit" class="btn btn-primary" id="submitBatchBtn">
                    &boxplus; Enqueue Batch Execution
                </button>
            </div>
        </form>
    </div>
</div>

<script>
    function toggleSelectAll() {
        const checkboxes = document.querySelectorAll('.algo-checkbox');
        const anyUnchecked = Array.from(checkboxes).some(cb => !cb.checked);
        checkboxes.forEach(cb => cb.checked = anyUnchecked);
        toggleRrQuantum();
    }

    function toggleRrQuantum() {
        const rr = document.getElementById('rrCheckbox');
        const group = document.getElementById('rrQuantumGroup');
        if (group) {
            group.style.opacity = rr.checked ? '1' : '0.4';
            document.getElementById('quantumInput').disabled = !rr.checked;
        }
    }

    document.getElementById('batchForm').addEventListener('submit', function(e) {
        const checked = document.querySelectorAll('.algo-checkbox:checked');
        if (checked.length === 0) {
            e.preventDefault();
            alert('Please select at least one algorithm for batch execution.');
            return false;
        }
        const btn = document.getElementById('submitBatchBtn');
        btn.disabled = true;
        btn.innerText = 'Enqueuing Batch...';
    });
</script>

</body>
</html>
