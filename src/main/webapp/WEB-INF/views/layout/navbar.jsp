<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<header class="app-navbar">
    <div class="nav-inner">
        <a href="<%= request.getContextPath() %>/dashboard" class="nav-brand">
            <span class="nav-brand-logo">O</span>
            <span>Orchestrivo</span>
            <span class="nav-brand-tagline">Execution & Scheduling Engine</span>
        </a>
        <ul class="nav-links">
            <li>
                <a href="<%= request.getContextPath() %>/dashboard" class="nav-link ${pageContext.request.servletPath.startsWith('/dashboard') ? 'active' : ''}">
                    Dashboard
                </a>
            </li>
            <li>
                <a href="<%= request.getContextPath() %>/workloads" class="nav-link ${pageContext.request.servletPath.startsWith('/workloads') ? 'active' : ''}">
                    Workloads
                </a>
            </li>
            <li>
                <a href="<%= request.getContextPath() %>/experiments" class="nav-link ${pageContext.request.servletPath.startsWith('/experiments') ? 'active' : ''}">
                    Experiments
                </a>
            </li>
            <li>
                <a href="<%= request.getContextPath() %>/jobs" class="nav-link ${pageContext.request.servletPath.startsWith('/jobs') ? 'active' : ''}">
                    Queue & Jobs
                </a>
            </li>
            <li>
                <a href="<%= request.getContextPath() %>/benchmarks" class="nav-link ${pageContext.request.servletPath.startsWith('/benchmarks') ? 'active' : ''}">
                    Benchmarks
                </a>
            </li>
        </ul>
        <div class="nav-actions">
            <c:if test="${not empty sessionScope.user}">
                <div class="nav-user">
                    <span class="user-badge" title="<c:out value='${sessionScope.user.email}'/>">
                        <c:out value="${sessionScope.user.email}"/>
                    </span>
                    <form action="<%= request.getContextPath() %>/logout" method="post" style="display:inline; margin:0;">
                        <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}'/>" />
                        <button type="submit" class="nav-logout-btn">Sign Out</button>
                    </form>
                </div>
            </c:if>
        </div>
    </div>
</header>
