package com.orchestrivo.web;

import com.orchestrivo.model.ExecutionJobRecord;
import com.orchestrivo.model.ExecutionJobStatus;
import com.orchestrivo.model.User;
import com.orchestrivo.service.ExecutionJobService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class JobSecurityIntegrationTest {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private FilterChain filterChain;
    private RequestDispatcher requestDispatcher;
    private ExecutionJobService jobService;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        filterChain = mock(FilterChain.class);
        requestDispatcher = mock(RequestDispatcher.class);
        jobService = mock(ExecutionJobService.class);
        ServiceRegistry.executionJobService = jobService;

        when(request.getSession()).thenReturn(session);
        when(request.getSession(anyBoolean())).thenReturn(session);
        when(request.getRequestDispatcher(anyString())).thenReturn(requestDispatcher);
        when(request.getContextPath()).thenReturn("");
    }

    @Test
    void testUnauthenticatedAccessToJobsRedirectsToLogin() throws ServletException, IOException {
        AuthFilter filter = new AuthFilter();

        when(request.getRequestURI()).thenReturn("/jobs");
        when(request.getContextPath()).thenReturn("");
        when(request.getMethod()).thenReturn("GET");
        when(session.getAttribute("user")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        verify(response).sendRedirect("/login");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testCsrfRejectionOnJobRetry() throws ServletException, IOException {
        AuthFilter filter = new AuthFilter();
        User sessionUser = new User();
        sessionUser.setId(1L);

        when(request.getRequestURI()).thenReturn("/jobs/retry");
        when(request.getContextPath()).thenReturn("");
        when(request.getMethod()).thenReturn("POST");
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(session.getAttribute("csrfToken")).thenReturn("valid-token");
        when(request.getParameter("csrfToken")).thenReturn("wrong-token");

        filter.doFilter(request, response, filterChain);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing CSRF Token");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testCsrfRejectionOnJobCancel() throws ServletException, IOException {
        AuthFilter filter = new AuthFilter();
        User sessionUser = new User();
        sessionUser.setId(1L);

        when(request.getRequestURI()).thenReturn("/jobs/cancel");
        when(request.getContextPath()).thenReturn("");
        when(request.getMethod()).thenReturn("POST");
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(session.getAttribute("csrfToken")).thenReturn("valid-token");
        when(request.getParameter("csrfToken")).thenReturn(null); // Missing token

        filter.doFilter(request, response, filterChain);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing CSRF Token");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testOwnershipEnforcementOnJobView() throws ServletException, IOException {
        JobServlet servlet = new JobServlet();
        User sessionUser = new User();
        sessionUser.setId(1L);
        when(session.getAttribute("user")).thenReturn(sessionUser);

        when(request.getServletPath()).thenReturn("/jobs/view");
        when(request.getParameter("id")).thenReturn("99");
        when(jobService.getJobById(99L, 1L)).thenReturn(Optional.empty());

        servlet.doGet(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND, "Job not found");
    }

    @Test
    void testMalformedIdHandlingOnJobView() throws ServletException, IOException {
        JobServlet servlet = new JobServlet();
        User sessionUser = new User();
        sessionUser.setId(1L);
        when(session.getAttribute("user")).thenReturn(sessionUser);

        when(request.getServletPath()).thenReturn("/jobs/view");
        when(request.getParameter("id")).thenReturn("invalid_id_format");

        servlet.doGet(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid job ID");
    }

    @Test
    void testStatusApiReturnsJsonForOwner() throws ServletException, IOException {
        JobServlet servlet = new JobServlet();
        User sessionUser = new User();
        sessionUser.setId(1L);
        when(session.getAttribute("user")).thenReturn(sessionUser);

        when(request.getServletPath()).thenReturn("/jobs/status");
        when(request.getParameter("id")).thenReturn("10");

        ExecutionJobRecord job = new ExecutionJobRecord();
        job.setId(10L);
        job.setStatus(ExecutionJobStatus.COMPLETED);
        job.setAttemptCount(1);
        job.setMaxAttempts(3);
        job.setExperimentRunId(55L);
        when(jobService.getJobById(10L, 1L)).thenReturn(Optional.of(job));

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);

        servlet.doGet(request, response);

        verify(response).setContentType("application/json; charset=UTF-8");
        String output = sw.toString();
        assertTrue(output.contains("\"status\":\"COMPLETED\""));
        assertTrue(output.contains("\"experimentRunId\":55"));
    }
}
