<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Register Account - Orchestrivo</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/app.css">
</head>
<body class="auth-wrapper">

<div class="auth-card">
    <div class="auth-header">
        <div class="nav-brand-logo" style="width: 44px; height: 44px; font-size: 22px; margin: 0 auto;">O</div>
        <h1>Create Orchestrivo Account</h1>
        <p>Start running simulation experiments and benchmarks</p>
    </div>

    <c:if test="${not empty error}">
        <div class="alert alert-danger">
            <c:out value="${error}"/>
        </div>
    </c:if>

    <form action="<%= request.getContextPath() %>/register" method="post">
        <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}'/>" />

        <div class="form-group">
            <label class="form-label" for="regEmail">Email Address</label>
            <input type="email" id="regEmail" name="email" class="form-input" placeholder="name@domain.com" required autofocus>
        </div>

        <div class="form-group">
            <label class="form-label" for="regPassword">Password</label>
            <input type="password" id="regPassword" name="password" class="form-input" placeholder="Choose a strong password" required minlength="6">
            <div class="form-hint">Minimum 6 characters</div>
        </div>

        <div style="margin-top: 24px;">
            <button type="submit" class="btn btn-primary" style="width: 100%; padding: 10px;">Create Account</button>
        </div>
    </form>

    <div class="auth-footer">
        Already have an account? <a href="<%= request.getContextPath() %>/login">Sign In</a>
    </div>
</div>

<script src="<%= request.getContextPath() %>/assets/js/app.js"></script>
</body>
</html>