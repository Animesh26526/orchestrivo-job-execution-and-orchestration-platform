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
        <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}'/>" />

        <div class="form-group">
            <label class="form-label" for="emailInput">Email Address</label>
            <input type="email" id="emailInput" name="email" class="form-input" placeholder="name@domain.com" required autofocus>
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
    </script>

    <div class="auth-footer">
        Don't have an account? <a href="<%= request.getContextPath() %>/register">Create Account</a>
    </div>
</div>

<script src="<%= request.getContextPath() %>/assets/js/app.js"></script>
</body>
</html>