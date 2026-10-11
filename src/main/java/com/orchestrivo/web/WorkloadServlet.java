package com.orchestrivo.web;

import com.orchestrivo.domain.ProcessSpec;
import com.orchestrivo.domain.Workload;
import com.orchestrivo.model.PagedResult;
import com.orchestrivo.model.User;
import com.orchestrivo.model.WorkloadRecord;
import com.orchestrivo.util.CsvExporter;
import com.orchestrivo.util.CsvWorkloadParser;
import com.orchestrivo.util.WorkloadGenerator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@WebServlet({"/workloads", "/workloads/new", "/workloads/edit", "/workloads/view", "/workloads/duplicate", "/workloads/import", "/workloads/generate", "/workloads/export", "/workloads/delete"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 64,       // 64 KB
        maxFileSize = 1024 * 1024,           // 1 MB
        maxRequestSize = 1024 * 1024 * 2     // 2 MB
)
public class WorkloadServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        User user = (User) req.getSession().getAttribute("user");

        if ("/workloads/new".equals(path)) {
            req.getRequestDispatcher("/WEB-INF/views/workloads/new.jsp").forward(req, resp);
        } else if ("/workloads/edit".equals(path)) {
            handleEditGet(req, resp, user);
        } else if ("/workloads/view".equals(path)) {
            handleViewGet(req, resp, user);
        } else if ("/workloads/import".equals(path)) {
            req.getRequestDispatcher("/WEB-INF/views/workloads/import.jsp").forward(req, resp);
        } else if ("/workloads/generate".equals(path)) {
            req.getRequestDispatcher("/WEB-INF/views/workloads/generate.jsp").forward(req, resp);
        } else if ("/workloads/export".equals(path)) {
            handleExportGet(req, resp, user);
        } else {
            handleListGet(req, resp, user);
        }
    }

    private void handleListGet(HttpServletRequest req, HttpServletResponse resp, User user) throws ServletException, IOException {
        String search = req.getParameter("search");
        String sortBy = req.getParameter("sortBy");
        String sortDir = req.getParameter("sortDir");
        int page = 1;
        int pageSize = 10;

        try {
            String p = req.getParameter("page");
            if (p != null && !p.trim().isEmpty()) {
                page = Integer.parseInt(p.trim());
            }
        } catch (NumberFormatException ignored) {}

        try {
            String ps = req.getParameter("pageSize");
            if (ps != null && !ps.trim().isEmpty()) {
                pageSize = Integer.parseInt(ps.trim());
            }
        } catch (NumberFormatException ignored) {}

        PagedResult<WorkloadRecord> pagedResult = ServiceRegistry.workloadService.getWorkloadsPaged(user.getId(), search, sortBy, sortDir, page, pageSize);
        req.setAttribute("pagedResult", pagedResult);
        req.setAttribute("workloads", pagedResult.getItems());
        req.setAttribute("search", search);
        req.setAttribute("sortBy", sortBy != null ? sortBy : "created_at");
        req.setAttribute("sortDir", sortDir != null ? sortDir : "DESC");
        req.getRequestDispatcher("/WEB-INF/views/workloads/list.jsp").forward(req, resp);
    }

    private void handleViewGet(HttpServletRequest req, HttpServletResponse resp, User user) throws ServletException, IOException {
        try {
            long id = Long.parseLong(req.getParameter("id"));
            Optional<WorkloadRecord> wr = ServiceRegistry.workloadService.getWorkloadById(id, user.getId());
            if (wr.isPresent()) {
                req.setAttribute("workload", wr.get());
                req.getRequestDispatcher("/WEB-INF/views/workloads/view.jsp").forward(req, resp);
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Workload not found");
            }
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid workload ID");
        }
    }

    private void handleEditGet(HttpServletRequest req, HttpServletResponse resp, User user) throws ServletException, IOException {
        try {
            long id = Long.parseLong(req.getParameter("id"));
            Optional<WorkloadRecord> wr = ServiceRegistry.workloadService.getWorkloadById(id, user.getId());
            if (wr.isPresent()) {
                req.setAttribute("workload", wr.get());
                req.getRequestDispatcher("/WEB-INF/views/workloads/edit.jsp").forward(req, resp);
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Workload not found");
            }
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid workload ID");
        }
    }

    private void handleExportGet(HttpServletRequest req, HttpServletResponse resp, User user) throws IOException {
        try {
            long id = Long.parseLong(req.getParameter("id"));
            Optional<WorkloadRecord> wr = ServiceRegistry.workloadService.getWorkloadById(id, user.getId());
            if (wr.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Workload not found");
                return;
            }

            WorkloadRecord workload = wr.get();
            if (ServiceRegistry.auditService != null) {
                ServiceRegistry.auditService.log(user.getId(), "CSV_EXPORT", "WORKLOAD", String.valueOf(id), Map.of("workloadName", workload.getName()));
            }

            resp.setContentType("text/csv; charset=UTF-8");
            resp.setHeader("Content-Disposition", "attachment; filename=\"workload-" + id + ".csv\"");
            CsvExporter.exportWorkload(workload, resp.getWriter());
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid workload ID");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        User user = (User) req.getSession().getAttribute("user");

        if ("/workloads/new".equals(path)) {
            handleCreatePost(req, resp, user);
        } else if ("/workloads/edit".equals(path)) {
            handleEditPost(req, resp, user);
        } else if ("/workloads/duplicate".equals(path)) {
            handleDuplicatePost(req, resp, user);
        } else if ("/workloads/import".equals(path)) {
            handleImportPost(req, resp, user);
        } else if ("/workloads/generate".equals(path)) {
            handleGeneratePost(req, resp, user);
        } else if ("/workloads/delete".equals(path)) {
            handleDeletePost(req, resp, user);
        }
    }

    private void handleCreatePost(HttpServletRequest req, HttpServletResponse resp, User user) throws ServletException, IOException {
        try {
            String name = req.getParameter("name");
            String description = req.getParameter("description");
            List<ProcessSpec> processes = parseProcessesFromRequest(req);

            WorkloadRecord created = ServiceRegistry.workloadService.createManualWorkload(user.getId(), name, description, processes);
            if (ServiceRegistry.auditService != null) {
                ServiceRegistry.auditService.log(user.getId(), "WORKLOAD_CREATE", "WORKLOAD", String.valueOf(created.getId()), Map.of("name", created.getName()));
            }
            resp.sendRedirect(req.getContextPath() + "/workloads");
        } catch (Exception e) {
            req.setAttribute("error", e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/workloads/new.jsp").forward(req, resp);
        }
    }

    private void handleEditPost(HttpServletRequest req, HttpServletResponse resp, User user) throws ServletException, IOException {
        try {
            long id = Long.parseLong(req.getParameter("id"));
            String name = req.getParameter("name");
            String description = req.getParameter("description");
            List<ProcessSpec> processes = parseProcessesFromRequest(req);

            WorkloadRecord updated = ServiceRegistry.workloadService.updateWorkload(id, user.getId(), name, description, processes);
            if (ServiceRegistry.auditService != null) {
                ServiceRegistry.auditService.log(user.getId(), "WORKLOAD_UPDATE", "WORKLOAD", String.valueOf(updated.getId()), Map.of("name", updated.getName()));
            }
            resp.sendRedirect(req.getContextPath() + "/workloads/view?id=" + updated.getId());
        } catch (IllegalArgumentException e) {
            req.setAttribute("error", e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/workloads/edit.jsp").forward(req, resp);
        } catch (SecurityException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        } catch (Exception e) {
            req.setAttribute("error", e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/workloads/edit.jsp").forward(req, resp);
        }
    }

    private void handleDuplicatePost(HttpServletRequest req, HttpServletResponse resp, User user) throws IOException {
        try {
            long id = Long.parseLong(req.getParameter("id"));
            String newName = req.getParameter("name");
            WorkloadRecord duplicated = ServiceRegistry.workloadService.duplicateWorkload(id, user.getId(), newName);
            if (ServiceRegistry.auditService != null) {
                ServiceRegistry.auditService.log(user.getId(), "WORKLOAD_DUPLICATE", "WORKLOAD", String.valueOf(duplicated.getId()), Map.of("originalId", id, "newName", duplicated.getName()));
            }
            resp.sendRedirect(req.getContextPath() + "/workloads");
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid workload ID");
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (SecurityException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        }
    }

    private void handleImportPost(HttpServletRequest req, HttpServletResponse resp, User user) throws ServletException, IOException {
        String name = req.getParameter("name");
        String description = req.getParameter("description");
        String csvContent = req.getParameter("csvContent");

        // If file was attached via multipart, read from file part
        if (csvContent == null || csvContent.trim().isEmpty()) {
            try {
                Part filePart = req.getPart("csvFile");
                if (filePart != null && filePart.getSize() > 0) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(filePart.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line).append("\n");
                        }
                        csvContent = sb.toString();
                    }
                }
            } catch (Exception ignored) {}
        }

        if (name == null || name.trim().isEmpty()) {
            req.setAttribute("error", "Workload name is required.");
            req.getRequestDispatcher("/WEB-INF/views/workloads/import.jsp").forward(req, resp);
            return;
        }

        CsvWorkloadParser.ParseResult parseResult = CsvWorkloadParser.parse(csvContent);
        if (!parseResult.isSuccess()) {
            req.setAttribute("errors", parseResult.getErrors());
            req.setAttribute("name", name);
            req.setAttribute("description", description);
            req.getRequestDispatcher("/WEB-INF/views/workloads/import.jsp").forward(req, resp);
            return;
        }

        try {
            WorkloadRecord record = new WorkloadRecord();
            record.setOwnerUserId(user.getId());
            record.setName(name.trim());
            record.setDescription(description);
            record.setSourceType("IMPORT");
            record.setWorkload(new Workload(parseResult.getProcesses()));

            WorkloadRecord saved = ServiceRegistry.workloadRepository.save(record);
            if (ServiceRegistry.auditService != null) {
                ServiceRegistry.auditService.log(user.getId(), "WORKLOAD_IMPORT", "WORKLOAD", String.valueOf(saved.getId()), Map.of("processCount", parseResult.getProcesses().size()));
            }
            resp.sendRedirect(req.getContextPath() + "/workloads/view?id=" + saved.getId());
        } catch (Exception e) {
            req.setAttribute("error", "Failed to save imported workload: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/workloads/import.jsp").forward(req, resp);
        }
    }

    private void handleGeneratePost(HttpServletRequest req, HttpServletResponse resp, User user) throws ServletException, IOException {
        try {
            String name = req.getParameter("name");
            int processCount = Integer.parseInt(req.getParameter("processCount"));
            int minArrival = Integer.parseInt(req.getParameter("minArrival"));
            int maxArrival = Integer.parseInt(req.getParameter("maxArrival"));
            int minBurst = Integer.parseInt(req.getParameter("minBurst"));
            int maxBurst = Integer.parseInt(req.getParameter("maxBurst"));
            long seed = Long.parseLong(req.getParameter("seed"));

            WorkloadGenerator.GeneratorConfig config = new WorkloadGenerator.GeneratorConfig(
                    name, processCount, minArrival, maxArrival, minBurst, maxBurst, seed
            );

            WorkloadRecord generated = WorkloadGenerator.generateWorkloadRecord(user.getId(), config);
            WorkloadRecord saved = ServiceRegistry.workloadRepository.save(generated);

            if (ServiceRegistry.auditService != null) {
                ServiceRegistry.auditService.log(user.getId(), "WORKLOAD_GENERATE", "WORKLOAD", String.valueOf(saved.getId()), Map.of("seed", seed, "processCount", processCount));
            }
            resp.sendRedirect(req.getContextPath() + "/workloads/view?id=" + saved.getId());
        } catch (Exception e) {
            req.setAttribute("error", e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/workloads/generate.jsp").forward(req, resp);
        }
    }

    private void handleDeletePost(HttpServletRequest req, HttpServletResponse resp, User user) throws IOException {
        try {
            long id = Long.parseLong(req.getParameter("id"));
            ServiceRegistry.workloadService.deleteWorkload(id, user.getId());
            if (ServiceRegistry.auditService != null) {
                ServiceRegistry.auditService.log(user.getId(), "WORKLOAD_DELETE", "WORKLOAD", String.valueOf(id), Collections.emptyMap());
            }
            resp.sendRedirect(req.getContextPath() + "/workloads");
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid workload ID");
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (SecurityException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        }
    }

    private List<ProcessSpec> parseProcessesFromRequest(HttpServletRequest req) {
        String[] pids = req.getParameterValues("processId");
        String[] arrivals = req.getParameterValues("arrivalTime");
        String[] bursts = req.getParameterValues("burstTime");

        List<ProcessSpec> processes = new ArrayList<>();
        if (pids != null) {
            for (int i = 0; i < pids.length; i++) {
                String pid = pids[i] != null ? pids[i].trim() : "";
                if (pid.isEmpty()) continue;
                int arr = arrivals != null && arrivals.length > i ? Integer.parseInt(arrivals[i].trim()) : 0;
                int brst = bursts != null && bursts.length > i ? Integer.parseInt(bursts[i].trim()) : 1;
                processes.add(new ProcessSpec(pid, arr, brst));
            }
        }
        return processes;
    }
}
