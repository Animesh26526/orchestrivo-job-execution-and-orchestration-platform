package com.orchestrivo.web;

import com.orchestrivo.persistence.*;
import com.orchestrivo.service.*;

public class ServiceRegistry {
    public static UserRepository userRepository;
    public static WorkloadRepository workloadRepository;
    public static ExperimentRunRepository experimentRunRepository;
    public static BenchmarkRepository benchmarkRepository;
    public static AuditLogRepository auditLogRepository;
    public static ExecutionJobRepository executionJobRepository;
    public static SystemSettingsRepository systemSettingsRepository;

    public static AuthenticationService authService;
    public static WorkloadService workloadService;
    public static ExperimentService experimentService;
    public static BenchmarkService benchmarkService;
    public static AuditService auditService;
    public static ExecutionJobService executionJobService;
    public static ExecutionWorkerManager workerManager;

    public static void initialize(UserRepository ur, WorkloadRepository wr, ExperimentRunRepository er) {
        initialize(ur, wr, er, new BenchmarkRepositoryJdbcImpl(), new AuditLogRepositoryJdbcImpl(), new ExecutionJobRepositoryJdbcImpl(), new SystemSettingsRepositoryJdbcImpl());
    }

    public static void initialize(UserRepository ur, WorkloadRepository wr, ExperimentRunRepository er,
                                  BenchmarkRepository br, AuditLogRepository ar) {
        initialize(ur, wr, er, br, ar, new ExecutionJobRepositoryJdbcImpl(), new SystemSettingsRepositoryJdbcImpl());
    }

    public static void initialize(UserRepository ur, WorkloadRepository wr, ExperimentRunRepository er,
                                  BenchmarkRepository br, AuditLogRepository ar, ExecutionJobRepository jr) {
        initialize(ur, wr, er, br, ar, jr, new SystemSettingsRepositoryJdbcImpl());
    }

    public static void initialize(UserRepository ur, WorkloadRepository wr, ExperimentRunRepository er,
                                  BenchmarkRepository br, AuditLogRepository ar, ExecutionJobRepository jr,
                                  SystemSettingsRepository sr) {
        userRepository = ur;
        workloadRepository = wr;
        experimentRunRepository = er;
        benchmarkRepository = br;
        auditLogRepository = ar;
        executionJobRepository = jr;
        systemSettingsRepository = sr;

        authService = new AuthenticationService(ur);
        workloadService = new WorkloadService(wr);
        experimentService = new ExperimentService(er);
        benchmarkService = new BenchmarkService(br, sr);
        auditService = new AuditService(ar);
        executionJobService = new ExecutionJobService(jr);

        // Configure worker manager
        workerManager = new ExecutionWorkerManager(jr, er, auditService, 2);
        executionJobService.setJobSignalListener(() -> workerManager.triggerImmediatePoll());
    }
}
