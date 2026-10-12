<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Create Account - Orchestrivo</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/app.css">
    <style>
        .password-field-wrapper {
            position: relative;
            display: flex;
            align-items: center;
        }
        .password-field-wrapper input {
            padding-right: 42px;
        }
        .toggle-password-btn {
            position: absolute;
            right: 8px;
            background: none;
            border: none;
            cursor: pointer;
            color: var(--color-slate-500);
            font-size: 13px;
            padding: 4px 6px;
            border-radius: 4px;
        }
        .toggle-password-btn:hover {
            color: var(--color-slate-800);
            background-color: var(--color-slate-100);
        }
        .field-error-text {
            color: var(--color-danger);
            font-size: 12px;
            margin-top: 4px;
            display: none;
        }
    </style>
</head>
<body class="auth-wrapper">

<div class="auth-card" style="max-width: 460px;">
    <div class="auth-header">
        <div class="nav-brand-logo" style="width: 48px; height: 48px; font-size: 24px; margin: 0 auto;">O</div>
        <h1>Create Orchestrivo Account</h1>
        <p>Register to run CPU scheduling simulations, asynchronous execution jobs, and benchmarks</p>
    </div>

    <c:if test="${not empty error}">
        <div class="alert alert-danger" id="serverAlert">
            <c:out value="${error}"/>
        </div>
    </c:if>

    <form id="registerForm" action="<%= request.getContextPath() %>/register" method="post" autocomplete="on">
        <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}'/>" />

        <div class="form-group">
            <label class="form-label" for="regFullName">Full Name</label>
            <input type="text" id="regFullName" name="fullName" class="form-input" 
                   placeholder="e.g. Jane Doe" value="<c:out value='${fullName}'/>" required autofocus>
            <div id="nameError" class="field-error-text">Please enter your full name.</div>
        </div>

        <div class="form-group">
            <label class="form-label" for="regEmail">Email Address</label>
            <input type="email" id="regEmail" name="email" class="form-input" 
                   placeholder="name@domain.com" value="<c:out value='${email}'/>" required autocomplete="email">
            <div id="emailError" class="field-error-text">Please enter a valid email address.</div>
        </div>

        <div class="form-group">
            <label class="form-label" for="regPassword">Password</label>
            <div class="password-field-wrapper">
                <input type="password" id="regPassword" name="password" class="form-input" 
                       placeholder="Create a secure password" required minlength="6" autocomplete="new-password">
                <button type="button" class="toggle-password-btn" onclick="togglePasswordVisibility('regPassword', this)" aria-label="Toggle password visibility">Show</button>
            </div>
            <div class="form-hint" id="passwordHint">Minimum 6 characters</div>
            <div id="passwordError" class="field-error-text">Password must be at least 6 characters.</div>
        </div>

        <div class="form-group">
            <label class="form-label" for="regConfirmPassword">Confirm Password</label>
            <div class="password-field-wrapper">
                <input type="password" id="regConfirmPassword" name="confirmPassword" class="form-input" 
                       placeholder="Re-enter your password" required minlength="6" autocomplete="new-password">
                <button type="button" class="toggle-password-btn" onclick="togglePasswordVisibility('regConfirmPassword', this)" aria-label="Toggle confirm password visibility">Show</button>
            </div>
            <div id="confirmPasswordError" class="field-error-text">Passwords do not match.</div>
        </div>

        <div style="margin-top: 26px;">
            <button type="submit" id="submitBtn" class="btn btn-primary" style="width: 100%; padding: 11px; font-weight: 600;">
                Create Account
            </button>
        </div>
    </form>

    <div class="auth-footer">
        Already have an account? <a href="<%= request.getContextPath() %>/login">Sign In</a>
    </div>
</div>

<script>
function togglePasswordVisibility(fieldId, btn) {
    const input = document.getElementById(fieldId);
    if (!input) return;
    if (input.type === 'password') {
        input.type = 'text';
        btn.textContent = 'Hide';
    } else {
        input.type = 'password';
        btn.textContent = 'Show';
    }
}

document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('registerForm');
    const pwd = document.getElementById('regPassword');
    const confirmPwd = document.getElementById('regConfirmPassword');
    const confirmErr = document.getElementById('confirmPasswordError');
    const submitBtn = document.getElementById('submitBtn');

    function checkPasswordMatch() {
        if (confirmPwd.value.length > 0 && pwd.value !== confirmPwd.value) {
            confirmErr.style.display = 'block';
            return false;
        } else {
            confirmErr.style.display = 'none';
            return true;
        }
    }

    confirmPwd.addEventListener('input', checkPasswordMatch);
    pwd.addEventListener('input', checkPasswordMatch);

    form.addEventListener('submit', function(e) {
        if (!checkPasswordMatch()) {
            e.preventDefault();
            confirmPwd.focus();
            return false;
        }
        submitBtn.disabled = true;
        submitBtn.textContent = 'Creating Account...';
    });
});
</script>
</body>
</html>