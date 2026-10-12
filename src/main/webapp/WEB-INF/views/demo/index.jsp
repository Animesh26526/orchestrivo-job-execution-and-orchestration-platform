<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Demo Data Pipeline & Seeder - Orchestrivo</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/app.css">
</head>
<body>

<jsp:include page="/WEB-INF/views/layout/navbar.jsp"/>

<div class="app-container" style="max-width: 800px;">
    <div class="breadcrumb-bar">
        <a href="<%= request.getContextPath() %>/dashboard">Dashboard</a>
        <span class="breadcrumb-separator">/</span>
        <span>Demo Data Pipeline</span>
    </div>

    <div class="page-header">
        <div class="page-title-group">
            <h1>Real Demo Data Seeding Pipeline</h1>
            <p class="page-subtitle">Populate Orchestrivo with genuine workloads, ~40 execution jobs, and canonical benchmarks</p>
        </div>
        <div class="page-actions">
            <a href="<%= request.getContextPath() %>/dashboard" class="btn btn-secondary">&larr; Back to Dashboard</a>
        </div>
    </div>

    <!-- Overview Card -->
    <div class="card">
        <div class="card-header">
            <h2 class="card-title">Demo Profile: <code>${demoEmail}</code></h2>
            <span class="badge ${demoUserExists ? 'badge-success' : 'badge-neutral'}">
                ${demoUserExists ? 'DEMO USER ACTIVE' : 'NOT INITIALIZED'}
            </span>
        </div>
        <div style="padding: 16px 0;">
            <p style="color: var(--color-slate-600); font-size: 14px; margin-top: 0;">
                The demo seeder generates real, non-mock records that pass through every validation check, scheduling algorithm, worker claim, and benchmark measurement in the genuine application pipeline.
            </p>

            <table class="data-table" style="margin-top: 12px;">
                <tbody>
                    <tr>
                        <td style="font-weight: 600; width: 220px;">Dedicated Demo User:</td>
                        <td><code>${demoEmail}</code> (Password: <code>${demoPassword}</code>)</td>
                    </tr>
                    <tr>
                        <td style="font-weight: 600;">Workloads:</td>
                        <td><strong>${workloadCount != null ? workloadCount : 0}</strong> created (Convoy Effect, Interactive, Uniform, 50-Process Scale, Mixed)</td>
                    </tr>
                    <tr>
                        <td style="font-weight: 600;">Execution Jobs:</td>
                        <td>
                            <strong>${queueMetrics != null ? queueMetrics.totalJobs : 0}</strong> total jobs
                            <c:if test="${queueMetrics != null}">
                                &bull; <span style="color: #15803d; font-weight: 600;">${queueMetrics.completedCount} Completed</span>
                                &bull; <span style="color: #b91c1c; font-weight: 600;">${queueMetrics.failedCount} Failed</span>
                                &bull; <span style="color: #64748b; font-weight: 600;">${queueMetrics.cancelledCount} Cancelled</span>
                                &bull; <span style="color: #d97706; font-weight: 600;">${queueMetrics.queuedCount} Queued</span>
                            </c:if>
                        </td>
                    </tr>
                    <tr>
                        <td style="font-weight: 600;">Micro-Benchmarks:</td>
                        <td><strong>${benchmarkCount != null ? benchmarkCount : 0}</strong> runs (includes Canonical Baseline vs. Candidate Pinned Pair)</td>
                    </tr>
                </tbody>
            </table>
        </div>

        <div style="display: flex; gap: 12px; margin-top: 20px; flex-wrap: wrap;">
            <form action="<%= request.getContextPath() %>/demo/seed" method="post" style="margin: 0;">
                <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}'/>" />
                <input type="hidden" name="reset" value="false">
                <input type="hidden" name="autoLogin" value="true">
                <button type="submit" class="btn btn-primary">Seed Demo Data (Idempotent) &rarr;</button>
            </form>

            <form action="<%= request.getContextPath() %>/demo/seed" method="post" style="margin: 0;">
                <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}'/>" />
                <input type="hidden" name="reset" value="true">
                <input type="hidden" name="autoLogin" value="true">
                <button type="submit" class="btn btn-danger" onclick="return confirm('Reset all existing data for the demo user and re-seed cleanly?');">Reset & Re-seed Cleanly</button>
            </form>
        </div>
    </div>
</div>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
<script src="<%= request.getContextPath() %>/assets/js/app.js"></script>
</body>
</html>
