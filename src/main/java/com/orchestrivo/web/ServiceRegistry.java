package com.orchestrivo.web;

import com.orchestrivo.persistence.*;
import com.orchestrivo.service.*;

public class ServiceRegistry {
    // In a real app without DI framework, we do simple singletons
    public static UserRepository userRepository;
    public static WorkloadRepository workloadRepository;
    public static ExperimentRunRepository experimentRunRepository;
    public static BenchmarkRepository benchmarkRepository;
    public static AuditLogRepository auditLogRepository;

    public static AuthenticationService authService;
    public static WorkloadService workloadService;
    public static ExperimentService experimentService;
    public static BenchmarkService benchmarkService;
    public static AuditService auditService;

    public static void initialize(UserRepository ur, WorkloadRepository wr, ExperimentRunRepository er) {
        initialize(ur, wr, er, new BenchmarkRepositoryJdbcImpl(), new AuditLogRepositoryJdbcImpl());
    }

    public static void initialize(UserRepository ur, WorkloadRepository wr, ExperimentRunRepository er,
                                  BenchmarkRepository br, AuditLogRepository ar) {
        userRepository = ur;
        workloadRepository = wr;
        experimentRunRepository = er;
        benchmarkRepository = br;
        auditLogRepository = ar;

        authService = new AuthenticationService(ur);
        workloadService = new WorkloadService(wr);
        experimentService = new ExperimentService(er);
        benchmarkService = new BenchmarkService(br);
        auditService = new AuditService(ar);
    }
}
