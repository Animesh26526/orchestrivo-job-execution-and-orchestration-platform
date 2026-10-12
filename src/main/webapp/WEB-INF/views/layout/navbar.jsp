<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<header class="app-navbar" role="banner">
    <div class="nav-inner">
        <a href="<%= request.getContextPath() %>/dashboard" class="nav-brand" aria-label="Orchestrivo Home">
            <span class="nav-brand-logo">O</span>
            <span>Orchestrivo</span>
            <span class="nav-brand-tagline">Execution & Scheduling Engine</span>
        </a>

        <!-- Mobile hamburger toggle button -->
        <button type="button" class="nav-mobile-toggle" id="navMobileToggle" aria-label="Toggle Navigation Menu" aria-expanded="false" aria-controls="navLinks">
            <span class="hamburger-bar"></span>
            <span class="hamburger-bar"></span>
            <span class="hamburger-bar"></span>
        </button>

        <nav class="nav-menu" id="navLinks" role="navigation" aria-label="Primary Navigation">
            <ul class="nav-links" role="menubar">
                <!-- 1. Dashboard Dropdown -->
                <li class="nav-item has-dropdown ${pageContext.request.servletPath.startsWith('/dashboard') ? 'active' : ''}" role="none">
                    <button class="nav-link dropdown-toggle" id="navDashboardDropdown" aria-haspopup="true" aria-expanded="false" type="button" role="menuitem">
                        Dashboard <span class="nav-chevron" aria-hidden="true">▾</span>
                    </button>
                    <div class="dropdown-menu" aria-labelledby="navDashboardDropdown" role="menu">
                        <a href="<%= request.getContextPath() %>/dashboard" class="dropdown-item" role="menuitem">Platform Overview</a>
                        <a href="<%= request.getContextPath() %>/dashboard#featuredComparison" class="dropdown-item" role="menuitem">Pinned Comparison</a>
                        <a href="<%= request.getContextPath() %>/dashboard#recentExecutionJobs" class="dropdown-item" role="menuitem">Recent Jobs</a>
                        <a href="<%= request.getContextPath() %>/dashboard#schedulingInsights" class="dropdown-item" role="menuitem">Scheduling Insights</a>
                    </div>
                </li>

                <!-- 2. Scheduling Dropdown -->
                <li class="nav-item has-dropdown ${pageContext.request.servletPath.startsWith('/workloads') || pageContext.request.servletPath.startsWith('/experiments') ? 'active' : ''}" role="none">
                    <button class="nav-link dropdown-toggle" id="navSchedulingDropdown" aria-haspopup="true" aria-expanded="false" type="button" role="menuitem">
                        Scheduling <span class="nav-chevron" aria-hidden="true">▾</span>
                    </button>
                    <div class="dropdown-menu" aria-labelledby="navSchedulingDropdown" role="menu">
                        <a href="<%= request.getContextPath() %>/workloads" class="dropdown-item" role="menuitem">Workload Library</a>
                        <a href="<%= request.getContextPath() %>/workloads/new" class="dropdown-item" role="menuitem">+ New Workload</a>
                        <div class="dropdown-divider"></div>
                        <a href="<%= request.getContextPath() %>/experiments/new" class="dropdown-item" role="menuitem">Single Simulation Run</a>
                        <a href="<%= request.getContextPath() %>/jobs/batch" class="dropdown-item" role="menuitem">Batch Simulation Run</a>
                        <div class="dropdown-divider"></div>
                        <a href="<%= request.getContextPath() %>/experiments" class="dropdown-item" role="menuitem">Experiment History</a>
                    </div>
                </li>

                <!-- 3. Execution Jobs Dropdown -->
                <li class="nav-item has-dropdown ${pageContext.request.servletPath.startsWith('/jobs') ? 'active' : ''}" role="none">
                    <button class="nav-link dropdown-toggle" id="navJobsDropdown" aria-haspopup="true" aria-expanded="false" type="button" role="menuitem">
                        Execution Jobs <span class="nav-chevron" aria-hidden="true">▾</span>
                    </button>
                    <div class="dropdown-menu" aria-labelledby="navJobsDropdown" role="menu">
                        <a href="<%= request.getContextPath() %>/jobs" class="dropdown-item" role="menuitem">Queue Dashboard</a>
                        <a href="<%= request.getContextPath() %>/jobs?status=RUNNING" class="dropdown-item" role="menuitem">Active Running Jobs</a>
                        <a href="<%= request.getContextPath() %>/jobs?status=QUEUED" class="dropdown-item" role="menuitem">Pending Queued Jobs</a>
                        <a href="<%= request.getContextPath() %>/jobs?status=FAILED" class="dropdown-item" role="menuitem">Failed & Dead-Letter</a>
                        <div class="dropdown-divider"></div>
                        <a href="<%= request.getContextPath() %>/jobs/batch" class="dropdown-item" role="menuitem">+ Enqueue Batch Workload</a>
                    </div>
                </li>

                <!-- 4. Benchmarks Dropdown -->
                <li class="nav-item has-dropdown ${pageContext.request.servletPath.startsWith('/benchmarks') ? 'active' : ''}" role="none">
                    <button class="nav-link dropdown-toggle" id="navBenchmarksDropdown" aria-haspopup="true" aria-expanded="false" type="button" role="menuitem">
                        Benchmarks <span class="nav-chevron" aria-hidden="true">▾</span>
                    </button>
                    <div class="dropdown-menu" aria-labelledby="navBenchmarksDropdown" role="menu">
                        <a href="<%= request.getContextPath() %>/benchmarks" class="dropdown-item" role="menuitem">Benchmark History</a>
                        <a href="<%= request.getContextPath() %>/benchmarks/run" class="dropdown-item" role="menuitem">+ Run Micro-Benchmark</a>
                        <a href="<%= request.getContextPath() %>/benchmarks/compare" class="dropdown-item" role="menuitem">Before / After Comparison</a>
                        <div class="dropdown-divider"></div>
                        <a href="<%= request.getContextPath() %>/benchmarks/guide" class="dropdown-item" role="menuitem">Methodology Guide</a>
                    </div>
                </li>
            </ul>
        </nav>

        <div class="nav-actions">
            <c:if test="${not empty sessionScope.user}">
                <!-- Account / System Dropdown -->
                <div class="nav-item has-dropdown nav-account-dropdown">
                    <button class="nav-link dropdown-toggle user-toggle-btn" id="navAccountDropdown" aria-haspopup="true" aria-expanded="false" type="button">
                        <span class="user-badge" title="<c:out value='${sessionScope.user.email}'/>">
                            <c:out value="${sessionScope.user.email}"/>
                        </span>
                        <span class="nav-chevron" aria-hidden="true">▾</span>
                    </button>
                    <div class="dropdown-menu dropdown-menu-end" aria-labelledby="navAccountDropdown" role="menu">
                        <div class="dropdown-header">Signed in as<br/><strong><c:out value="${sessionScope.user.email}"/></strong></div>
                        <div class="dropdown-divider"></div>
                        <a href="<%= request.getContextPath() %>/demo" class="dropdown-item" role="menuitem">Demo Data Pipeline</a>
                        <div class="dropdown-divider"></div>
                        <form action="<%= request.getContextPath() %>/logout" method="post" style="margin: 0;">
                            <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}'/>" />
                            <button type="submit" class="dropdown-item logout-item" role="menuitem">Sign Out</button>
                        </form>
                    </div>
                </div>
            </c:if>
            <c:if test="${empty sessionScope.user}">
                <a href="<%= request.getContextPath() %>/login" class="btn btn-sm btn-primary">Sign In</a>
            </c:if>
        </div>
    </div>
</header>
