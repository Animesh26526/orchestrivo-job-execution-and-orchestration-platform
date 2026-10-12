package com.orchestrivo.web;

import com.orchestrivo.model.User;
import com.orchestrivo.model.WorkloadRecord;
import com.orchestrivo.service.AuthenticationService;
import com.orchestrivo.service.WorkloadService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class SecurityIntegrationTest {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private FilterChain filterChain;
    private RequestDispatcher requestDispatcher;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        filterChain = mock(FilterChain.class);
        requestDispatcher = mock(RequestDispatcher.class);

        when(request.getSession()).thenReturn(session);
        when(request.getSession(anyBoolean())).thenReturn(session);
        when(request.getRequestDispatcher(anyString())).thenReturn(requestDispatcher);
        when(request.getContextPath()).thenReturn("");
    }

    @Test
    void testSessionIdRotationOnLogin() throws ServletException, IOException {
        AuthenticationService authService = mock(AuthenticationService.class);
        ServiceRegistry.authService = authService;

        LoginServlet loginServlet = new LoginServlet();
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test");
        when(authService.authenticate("email", "password")).thenReturn(mockUser);
        
        when(request.getParameter("email")).thenReturn("email");
        when(request.getParameter("password")).thenReturn("password");

        loginServlet.doPost(request, response);

        // Verify session rotation method was called
        verify(request).changeSessionId();
    }
    
    @Test
    void testSessionIdRotationOnRegister() throws ServletException, IOException {
        AuthenticationService authService = mock(AuthenticationService.class);
        ServiceRegistry.authService = authService;

        RegisterServlet registerServlet = new RegisterServlet();
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test");
        when(authService.register("email", "password", "USER")).thenReturn(mockUser);
        
        when(request.getParameter("email")).thenReturn("email");
        when(request.getParameter("password")).thenReturn("password");

        registerServlet.doPost(request, response);

        // Verify session rotation method was called
        verify(request).changeSessionId();
    }

    @Test
    void testCsrfRejectionForMissingToken() throws ServletException, IOException {
        AuthFilter filter = new AuthFilter();
        
        when(request.getRequestURI()).thenReturn("/workloads/new");
        when(request.getContextPath()).thenReturn("");
        when(request.getMethod()).thenReturn("POST");
        User sessionUser = new User();
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(session.getAttribute("csrfToken")).thenReturn("valid-token");
        when(request.getParameter("csrfToken")).thenReturn(null); // Missing

        filter.doFilter(request, response, filterChain);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing CSRF Token");
        verify(filterChain, never()).doFilter(request, response);
    }
    
    @Test
    void testCsrfRejectionForInvalidToken() throws ServletException, IOException {
        AuthFilter filter = new AuthFilter();
        
        when(request.getRequestURI()).thenReturn("/login");
        when(request.getContextPath()).thenReturn("");
        when(request.getMethod()).thenReturn("POST");
        when(session.getAttribute("user")).thenReturn(null);
        when(session.getAttribute("csrfToken")).thenReturn("valid-token");
        when(request.getParameter("csrfToken")).thenReturn("invalid-token");

        filter.doFilter(request, response, filterChain);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing CSRF Token");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testOwnershipEnforcementForWorkloadRetrieval() throws ServletException, IOException {
        WorkloadService workloadService = mock(WorkloadService.class);
        ServiceRegistry.workloadService = workloadService;
        
        WorkloadServlet servlet = new WorkloadServlet();
        User sessionUser = new User();
        sessionUser.setId(1L);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        
        when(request.getServletPath()).thenReturn("/workloads/view");
        when(request.getParameter("id")).thenReturn("10");
        
        // Simulating the case where the user asks for workload 10, but the service returns empty
        // because it doesn't belong to them or doesn't exist.
        when(workloadService.getWorkloadById(10L, 1L)).thenReturn(Optional.empty());

        servlet.doGet(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND, "Workload not found");
    }

    @Test
    void testSafeHandlingOfMalformedIds() throws ServletException, IOException {
        WorkloadServlet servlet = new WorkloadServlet();
        User sessionUser = new User();
        sessionUser.setId(1L);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        
        when(request.getServletPath()).thenReturn("/workloads/view");
        when(request.getParameter("id")).thenReturn("not-a-number");

        servlet.doGet(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid workload ID");
    }

    @Test
    void testOwnershipEnforcementForExperimentRetrieval() throws ServletException, IOException {
        com.orchestrivo.service.ExperimentService experimentService = mock(com.orchestrivo.service.ExperimentService.class);
        ServiceRegistry.experimentService = experimentService;
        
        ExperimentServlet servlet = new ExperimentServlet();
        User sessionUser = new User();
        sessionUser.setId(1L);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        
        when(request.getServletPath()).thenReturn("/experiments/view");
        when(request.getParameter("id")).thenReturn("5");
        
        when(experimentService.getExperimentById(5L, 1L)).thenReturn(Optional.empty());

        servlet.doGet(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND, "Experiment not found");
    }

    @Test
    void testOwnershipEnforcementForExperimentMutation() throws ServletException, IOException {
        WorkloadService workloadService = mock(WorkloadService.class);
        ServiceRegistry.workloadService = workloadService;
        
        ExperimentServlet servlet = new ExperimentServlet();
        User sessionUser = new User();
        sessionUser.setId(1L);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        
        when(request.getServletPath()).thenReturn("/experiments/run");
        when(request.getParameter("workloadId")).thenReturn("10");
        when(request.getParameter("algorithmName")).thenReturn("FCFS");
        
        // Simulating the workload not belonging to the user
        when(workloadService.getWorkloadById(10L, 1L)).thenReturn(Optional.empty());

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Workload not found or not owned");
    }

    @Test
    void testOwnershipEnforcementForWorkloadMutation() throws ServletException, IOException {
        WorkloadService workloadService = mock(WorkloadService.class);
        ServiceRegistry.workloadService = workloadService;
        
        WorkloadServlet servlet = new WorkloadServlet();
        User sessionUser = new User();
        sessionUser.setId(1L);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        
        when(request.getServletPath()).thenReturn("/workloads/delete");
        when(request.getParameter("id")).thenReturn("10");
        
        // Simulating the case where delete throws SecurityException because the workload is owned by another user
        doThrow(new SecurityException("Unauthorized to delete workload.")).when(workloadService).deleteWorkload(10L, 1L);

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Unauthorized to delete workload.");
    }

    @Test
    void testWorkloadDeletionForNonexistentResource() throws ServletException, IOException {
        WorkloadService workloadService = mock(WorkloadService.class);
        ServiceRegistry.workloadService = workloadService;
        
        WorkloadServlet servlet = new WorkloadServlet();
        User sessionUser = new User();
        sessionUser.setId(1L);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        
        when(request.getServletPath()).thenReturn("/workloads/delete");
        when(request.getParameter("id")).thenReturn("99");
        
        // Simulating the case where delete throws IllegalArgumentException because the workload does not exist
        doThrow(new IllegalArgumentException("Workload not found.")).when(workloadService).deleteWorkload(99L, 1L);

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND, "Workload not found.");
    }

    @Test
    void testMalformedIdOnWorkloadDelete() throws ServletException, IOException {
        WorkloadServlet servlet = new WorkloadServlet();
        User sessionUser = new User();
        sessionUser.setId(1L);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        
        when(request.getServletPath()).thenReturn("/workloads/delete");
        when(request.getParameter("id")).thenReturn("not-a-number");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid workload ID");
    }

    @Test
    void testMalformedIdOnExperimentView() throws ServletException, IOException {
        ExperimentServlet servlet = new ExperimentServlet();
        User sessionUser = new User();
        sessionUser.setId(1L);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        
        when(request.getServletPath()).thenReturn("/experiments/view");
        when(request.getParameter("id")).thenReturn("invalid-id");

        servlet.doGet(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid experiment ID");
    }

    @Test
    void testUnauthenticatedAccessRedirectsToLogin() throws ServletException, IOException {
        AuthFilter filter = new AuthFilter();
        
        when(request.getRequestURI()).thenReturn("/workloads");
        when(request.getContextPath()).thenReturn("");
        when(request.getMethod()).thenReturn("GET");
        when(session.getAttribute("user")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        verify(response).sendRedirect("/login");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testCsrfPermitsValidToken() throws ServletException, IOException {
        AuthFilter filter = new AuthFilter();
        
        when(request.getRequestURI()).thenReturn("/workloads/new");
        when(request.getContextPath()).thenReturn("");
        when(request.getMethod()).thenReturn("POST");
        User sessionUser = new User();
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(session.getAttribute("csrfToken")).thenReturn("valid-token");
        when(request.getParameter("csrfToken")).thenReturn("valid-token");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    void testCsrfRejectionOnLogout() throws ServletException, IOException {
        AuthFilter filter = new AuthFilter();
        
        when(request.getRequestURI()).thenReturn("/logout");
        when(request.getContextPath()).thenReturn("");
        when(request.getMethod()).thenReturn("POST");
        User sessionUser = new User();
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(session.getAttribute("csrfToken")).thenReturn("valid-token");
        when(request.getParameter("csrfToken")).thenReturn("wrong-token");

        filter.doFilter(request, response, filterChain);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing CSRF Token");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testCsrfRejectionOnRegistration() throws ServletException, IOException {
        AuthFilter filter = new AuthFilter();
        
        when(request.getRequestURI()).thenReturn("/register");
        when(request.getContextPath()).thenReturn("");
        when(request.getMethod()).thenReturn("POST");
        when(session.getAttribute("user")).thenReturn(null);
        when(session.getAttribute("csrfToken")).thenReturn("valid-token");
        when(request.getParameter("csrfToken")).thenReturn("invalid-token");

        filter.doFilter(request, response, filterChain);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing CSRF Token");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testSafeHandlingOfUnexpectedExceptions() throws ServletException, IOException {
        AuthenticationService authService = mock(AuthenticationService.class);
        ServiceRegistry.authService = authService;
        
        LoginServlet servlet = new LoginServlet();
        
        when(request.getParameter("email")).thenReturn("email");
        when(request.getParameter("password")).thenReturn("password");
        
        // Mocking an unexpected runtime exception (e.g. Database down)
        when(authService.authenticate("email", "password")).thenThrow(new RuntimeException("Database connection failed"));

        assertThrows(RuntimeException.class, () -> servlet.doPost(request, response));
        // The servlet container handles 500 mapping for uncaught RuntimeExceptions.
        // We verify that the raw exception is not caught and swallowed as a 200 OK.
    }

    @Test
    void testHtmlEscapingInRenderedViews() throws IOException {
        Path viewsPath = Paths.get("src/main/webapp/WEB-INF/views");
        assertTrue(Files.exists(viewsPath), "Views directory should exist");

        // We check workload list and view jsp for c:out usage wrapping specific user-controlled fields.
        Path listJsp = viewsPath.resolve("workloads/list.jsp");
        Path viewJsp = viewsPath.resolve("workloads/view.jsp");
        Path expListJsp = viewsPath.resolve("experiments/list.jsp");
        Path expViewJsp = viewsPath.resolve("experiments/view.jsp");

        if (Files.exists(listJsp)) {
            String listContent = Files.readString(listJsp);
            assertTrue(listContent.contains("<c:out value=\"${w.name}\"/>"), "List JSP should escape workload name");
        }
        
        if (Files.exists(viewJsp)) {
            String viewContent = Files.readString(viewJsp);
            assertTrue(viewContent.contains("<c:out value=\"${workload.name}\"/>"), "View JSP should escape workload name");
            assertTrue(viewContent.contains("<c:out value=\"${workload.description}\"/>"), "View JSP should escape workload description");
        }

        if (Files.exists(expListJsp)) {
            String expListContent = Files.readString(expListJsp);
            assertTrue(expListContent.contains("<c:out value=\"${e.algorithmName}\"/>"), "Experiments list should escape algorithm name");
            assertTrue(expListContent.contains("<c:out value=\"${e.runStatus}\"/>"), "Experiments list should escape run status");
        }

        if (Files.exists(expViewJsp)) {
            String expViewContent = Files.readString(expViewJsp);
            assertTrue(expViewContent.contains("<c:out value=\"${experiment.algorithmName}\"/>"), "Experiments view should escape algorithm name");
            assertTrue(expViewContent.contains("<c:out value=\"${experiment.runStatus}\"/>"), "Experiments view should escape run status");
            assertTrue(expViewContent.contains("<c:out value=\"${seg.processId}\"/>"), "Experiments view should escape process ID");
        }
    }

    @Test
    void testCsrfEnforcementOnStage3Endpoints() throws ServletException, IOException {
        AuthFilter filter = new AuthFilter();
        User sessionUser = new User();
        sessionUser.setId(1L);

        when(request.getContextPath()).thenReturn("");
        when(request.getMethod()).thenReturn("POST");
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(session.getAttribute("csrfToken")).thenReturn("correct-token");
        when(request.getParameter("csrfToken")).thenReturn("wrong-token");

        String[] mutatingPaths = {
            "/workloads/edit",
            "/workloads/duplicate",
            "/workloads/import",
            "/workloads/generate",
            "/benchmarks/run",
            "/benchmarks/delete"
        };

        for (String p : mutatingPaths) {
            reset(response);
            when(request.getRequestURI()).thenReturn(p);
            filter.doFilter(request, response, filterChain);
            verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing CSRF Token");
        }
    }

    @Test
    void testOwnershipEnforcementForBenchmarkRetrieval() throws ServletException, IOException {
        BenchmarkServlet servlet = new BenchmarkServlet();
        User sessionUser = new User();
        sessionUser.setId(1L);
        when(session.getAttribute("user")).thenReturn(sessionUser);

        when(request.getServletPath()).thenReturn("/benchmarks/view");
        when(request.getParameter("id")).thenReturn("77");

        com.orchestrivo.service.BenchmarkService benchmarkService = mock(com.orchestrivo.service.BenchmarkService.class);
        ServiceRegistry.benchmarkService = benchmarkService;
        when(benchmarkService.getBenchmarkById(77L, 1L)).thenReturn(Optional.empty());

        servlet.doGet(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND, "Benchmark not found");
    }

    @Test
    void testOwnershipEnforcementForWorkloadEdit() throws ServletException, IOException {
        WorkloadServlet servlet = new WorkloadServlet();
        User sessionUser = new User();
        sessionUser.setId(1L);
        when(session.getAttribute("user")).thenReturn(sessionUser);

        when(request.getServletPath()).thenReturn("/workloads/edit");
        when(request.getParameter("id")).thenReturn("55");

        WorkloadService workloadService = mock(WorkloadService.class);
        ServiceRegistry.workloadService = workloadService;
        when(workloadService.getWorkloadById(55L, 1L)).thenReturn(Optional.empty());

        servlet.doGet(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND, "Workload not found");
    }

    @Test
    void testValidLoginWithKnownAccount() throws ServletException, IOException {
        AuthenticationService authService = mock(AuthenticationService.class);
        ServiceRegistry.authService = authService;
        ServiceRegistry.auditService = mock(com.orchestrivo.service.AuditService.class);

        User knownUser = new User();
        knownUser.setId(42L);
        knownUser.setEmail("valid@example.com");
        knownUser.setRole("USER");
        when(authService.authenticate("valid@example.com", "CorrectPassword123!")).thenReturn(knownUser);

        when(request.getParameter("email")).thenReturn("valid@example.com");
        when(request.getParameter("password")).thenReturn("CorrectPassword123!");
        when(request.getContextPath()).thenReturn("/orchestrivo");

        LoginServlet loginServlet = new LoginServlet();
        loginServlet.doPost(request, response);

        verify(request).changeSessionId();
        verify(session).setAttribute("user", knownUser);
        verify(session).setAttribute(eq("csrfToken"), anyString());
        verify(response).sendRedirect("/orchestrivo/dashboard");
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    void testLoginWithIncorrectPassword() throws ServletException, IOException {
        AuthenticationService authService = mock(AuthenticationService.class);
        ServiceRegistry.authService = authService;

        when(authService.authenticate("valid@example.com", "WrongPassword")).thenReturn(null);

        when(request.getParameter("email")).thenReturn("valid@example.com");
        when(request.getParameter("password")).thenReturn("WrongPassword");
        when(request.getRequestDispatcher("/WEB-INF/views/auth/login.jsp")).thenReturn(requestDispatcher);

        LoginServlet loginServlet = new LoginServlet();
        loginServlet.doPost(request, response);

        verify(request).setAttribute("error", "Invalid email or password");
        verify(requestDispatcher).forward(request, response);
        verify(session, never()).setAttribute(eq("user"), any());
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    void testLoginWithUnknownAccount() throws ServletException, IOException {
        AuthenticationService authService = mock(AuthenticationService.class);
        ServiceRegistry.authService = authService;

        when(authService.authenticate("unknown@example.com", "SomePassword")).thenReturn(null);

        when(request.getParameter("email")).thenReturn("unknown@example.com");
        when(request.getParameter("password")).thenReturn("SomePassword");
        when(request.getRequestDispatcher("/WEB-INF/views/auth/login.jsp")).thenReturn(requestDispatcher);

        LoginServlet loginServlet = new LoginServlet();
        loginServlet.doPost(request, response);

        verify(request).setAttribute("error", "Invalid email or password");
        verify(requestDispatcher).forward(request, response);
        verify(session, never()).setAttribute(eq("user"), any());
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    void testLoginRejectionOnMissingCsrfToken() throws ServletException, IOException {
        AuthFilter filter = new AuthFilter();

        when(request.getServletPath()).thenReturn("/login");
        when(request.getPathInfo()).thenReturn(null);
        when(request.getMethod()).thenReturn("POST");
        when(session.getAttribute("csrfToken")).thenReturn("expected-csrf-token");
        when(request.getParameter("csrfToken")).thenReturn(null);
        when(request.getHeader("X-CSRF-TOKEN")).thenReturn(null);
        when(request.getHeader("X-XSRF-TOKEN")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing CSRF Token");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testLoginRejectionOnInvalidCsrfToken() throws ServletException, IOException {
        AuthFilter filter = new AuthFilter();

        when(request.getServletPath()).thenReturn("/login");
        when(request.getPathInfo()).thenReturn(null);
        when(request.getMethod()).thenReturn("POST");
        when(session.getAttribute("csrfToken")).thenReturn("expected-csrf-token");
        when(request.getParameter("csrfToken")).thenReturn("mismatched-csrf-token");

        filter.doFilter(request, response, filterChain);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing CSRF Token");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testPublicLoginRouteNotBlockedByAuthMiddleware() throws ServletException, IOException {
        AuthFilter filter = new AuthFilter();

        when(request.getServletPath()).thenReturn("/login");
        when(request.getPathInfo()).thenReturn(null);
        when(request.getMethod()).thenReturn("GET");
        when(session.getAttribute("user")).thenReturn(null);
        when(session.getAttribute("csrfToken")).thenReturn("active-token");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).sendRedirect(anyString());
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    void testPublicRegisterRouteNotBlockedByAuthMiddleware() throws ServletException, IOException {
        AuthFilter filter = new AuthFilter();

        when(request.getServletPath()).thenReturn("/register");
        when(request.getPathInfo()).thenReturn(null);
        when(request.getMethod()).thenReturn("GET");
        when(session.getAttribute("user")).thenReturn(null);
        when(session.getAttribute("csrfToken")).thenReturn("active-token");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).sendRedirect(anyString());
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    void testProtectedPagesRejectUnauthenticatedUsers() throws ServletException, IOException {
        AuthFilter filter = new AuthFilter();

        when(request.getContextPath()).thenReturn("/orchestrivo");
        when(request.getMethod()).thenReturn("GET");
        when(session.getAttribute("user")).thenReturn(null);

        String[] protectedPaths = {"/dashboard", "/workloads", "/jobs", "/benchmarks", "/experiments"};
        for (String p : protectedPaths) {
            reset(response);
            when(request.getServletPath()).thenReturn(p);
            when(request.getPathInfo()).thenReturn(null);

            filter.doFilter(request, response, filterChain);

            verify(response).sendRedirect("/orchestrivo/login");
        }
    }

    @Test
    void testSessionFixationProtectionOnLogin() throws ServletException, IOException {
        AuthenticationService authService = mock(AuthenticationService.class);
        ServiceRegistry.authService = authService;

        User user = new User();
        user.setId(10L);
        user.setEmail("user10@example.com");
        when(authService.authenticate("user10@example.com", "pass")).thenReturn(user);

        when(request.getParameter("email")).thenReturn("user10@example.com");
        when(request.getParameter("password")).thenReturn("pass");
        when(request.getContextPath()).thenReturn("");

        LoginServlet loginServlet = new LoginServlet();
        loginServlet.doPost(request, response);

        verify(request).changeSessionId();
    }

    @Test
    void testLogoutSessionInvalidation() throws ServletException, IOException {
        LogoutServlet logoutServlet = new LogoutServlet();
        when(request.getSession(false)).thenReturn(session);
        when(request.getContextPath()).thenReturn("/orchestrivo");

        logoutServlet.doPost(request, response);

        verify(session).invalidate();
        verify(response).sendRedirect("/orchestrivo/login");
    }

    @Test
    void testDemoLoginAuthentication() {
        com.orchestrivo.persistence.UserRepository userRepo = mock(com.orchestrivo.persistence.UserRepository.class);
        AuthenticationService authService = new AuthenticationService(userRepo);

        User demoUser = new User();
        demoUser.setId(99L);
        demoUser.setEmail("demo@orchestrivo.io");
        demoUser.setRole("USER");
        demoUser.setActive(true);
        // BCrypt hash for "Demo@12345"
        String hash = at.favre.lib.crypto.bcrypt.BCrypt.withDefaults().hashToString(12, "Demo@12345".toCharArray());
        demoUser.setPasswordHash(hash);

        when(userRepo.findByEmail("demo@orchestrivo.io")).thenReturn(Optional.of(demoUser));

        User authenticated = authService.authenticate("demo@orchestrivo.io", "Demo@12345");
        assertNotNull(authenticated, "Demo user must authenticate with documented credentials");
        assertEquals("demo@orchestrivo.io", authenticated.getEmail());

        User wrongPass = authService.authenticate("demo@orchestrivo.io", "WrongPassword");
        assertNull(wrongPass, "Demo user must not authenticate with wrong password");
    }
}
