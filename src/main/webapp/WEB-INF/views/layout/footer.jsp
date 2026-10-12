<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<footer style="margin-top: 60px; padding: 24px 0 32px; border-top: 1px solid var(--color-border); background: var(--color-bg-surface); color: var(--color-text-secondary); font-size: 13px;">
    <div class="container" style="display: flex; flex-wrap: wrap; justify-content: space-between; align-items: center; gap: 16px;">
        <div style="display: flex; align-items: center; gap: 10px;">
            <div class="nav-brand-logo" style="width: 24px; height: 24px; font-size: 13px;">O</div>
            <span><strong>Orchestrivo</strong> &mdash; CPU Scheduling Simulation &amp; Asynchronous Orchestration Platform</span>
        </div>
        <div style="display: flex; gap: 20px; align-items: center; flex-wrap: wrap;">
            <a href="<%= request.getContextPath() %>/benchmarks/guide" style="color: var(--color-text-secondary); text-decoration: none;">Methodology Guide</a>
            <a href="<%= request.getContextPath() %>/benchmarks/compare?export=csv" style="color: var(--color-text-secondary); text-decoration: none;">Export Benchmark CSV</a>
            <a href="<%= request.getContextPath() %>/demo" style="color: var(--color-text-secondary); text-decoration: none;">Demo Console</a>
            <span style="color: var(--color-border);">|</span>
            <span style="font-size: 12px; color: var(--color-slate-400);">v1.0 &bull; Java 17 &bull; Jakarta EE &bull; HikariCP</span>
        </div>
    </div>
</footer>
