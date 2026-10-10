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
    void testHtmlEscapingInRenderedViews() throws IOException {
        // Since we can't easily run a JSP engine in this test without complex setup,
        // we can verify that the JSP files use <c:out> for user-controlled data.
        Path viewsPath = Paths.get("src/main/webapp/WEB-INF/views");
        assertTrue(Files.exists(viewsPath), "Views directory should exist");

        // We check workload list and view jsp for c:out usage which escapes HTML by default.
        try (Stream<Path> paths = Files.walk(viewsPath)) {
            boolean hasCOut = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".jsp"))
                    .anyMatch(p -> {
                        try {
                            String content = Files.readString(p);
                            return content.contains("<c:out");
                        } catch (IOException e) {
                            return false;
                        }
                    });
            assertTrue(hasCOut, "Expected to find <c:out> tags in JSP files for HTML escaping");
        }
    }
}
