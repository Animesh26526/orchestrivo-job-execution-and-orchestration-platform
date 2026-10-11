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
}
