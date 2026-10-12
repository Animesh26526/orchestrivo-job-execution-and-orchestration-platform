<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sign In - Orchestrivo</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/app.css">
</head>
<body class="auth-wrapper">

<div class="auth-card">
    <div class="auth-header">
        <div class="nav-brand-logo" style="width: 44px; height: 44px; font-size: 22px; margin: 0 auto;">O</div>
        <h1>Sign In to Orchestrivo</h1>
        <p>CPU Scheduling Simulation & Benchmarking Platform</p>
    </div>

    <c:if test="${not empty error}">
        <div class="alert alert-danger">
            <c:out value="${error}"/>
        </div>
    </c:if>

    <form action="<%= request.getContextPath() %>/login" method="post">
        <input type="hidden" name="csrfToken" value="<c:out value='${not empty sessionScope.csrfToken ? sessionScope.csrfToken : csrfToken}'/>" />

        <div class="form-group">
            <label class="form-label" for="emailInput">Email Address</label>
            <input type="email" id="emailInput" name="email" class="form-input" placeholder="name@domain.com" value="<c:out value='${email}'/>" required autofocus>
        </div>

        <div class="form-group">
            <label class="form-label" for="passwordInput">Password</label>
            <div style="position: relative; display: flex; align-items: center;">
                <input type="password" id="passwordInput" name="password" class="form-input" style="padding-right: 42px;" placeholder="Enter your password" required autocomplete="current-password">
                <button type="button" class="btn btn-outline" style="position: absolute; right: 6px; padding: 3px 8px; font-size: 11px;" onclick="toggleLoginPassword(this)" aria-label="Toggle password visibility">Show</button>
            </div>
        </div>

        <div style="margin-top: 24px;">
            <button type="submit" class="btn btn-primary" style="width: 100%; padding: 10px;">Sign In</button>
        </div>
    </form>

    <script>
    function toggleLoginPassword(btn) {
        const inp = document.getElementById('passwordInput');
        if (inp.type === 'password') {
            inp.type = 'text';
            btn.textContent = 'Hide';
        } else {
            inp.type = 'password';
            btn.textContent = 'Show';
        }
    }
    function fillDemoCredentials() {
        document.getElementById('emailInput').value = 'demo@orchestrivo.io';
        document.getElementById('passwordInput').value = 'Demo@12345';
    }
    </script>

    <div style="margin-top: 18px; padding: 12px 14px; background: rgba(56, 189, 248, 0.05); border: 1px dashed rgba(56, 189, 248, 0.3); border-radius: var(--radius-sm); font-size: 12px; text-align: left;">
        <div style="font-weight: 600; color: var(--color-accent); margin-bottom: 4px;">🚀 Quick Demo Access</div>
        <div style="color: var(--color-text-secondary); line-height: 1.5; margin-bottom: 8px;">
            Email: <code style="color: var(--color-text-primary); background: rgba(255,255,255,0.06); padding: 1px 4px; border-radius: 3px;">demo@orchestrivo.io</code><br/>
            Password: <code style="color: var(--color-text-primary); background: rgba(255,255,255,0.06); padding: 1px 4px; border-radius: 3px;">Demo@12345</code>
        </div>
        <div style="display: flex; gap: 8px;">
            <button type="button" class="btn btn-outline btn-sm" onclick="fillDemoCredentials()">Autofill Demo</button>
            <a href="<%= request.getContextPath() %>/demo" class="btn btn-secondary btn-sm">Seeder Control</a>
        </div>
    </div>

    <div class="auth-footer" style="margin-top: 16px;">
        Don't have an account? <a href="<%= request.getContextPath() %>/register">Create Account</a>
    </div>
</div>

<script src="<%= request.getContextPath() %>/assets/js/app.js"></script>
</body>
</html>