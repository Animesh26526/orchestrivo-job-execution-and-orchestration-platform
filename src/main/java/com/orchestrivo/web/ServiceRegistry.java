package com.orchestrivo.web;

import com.orchestrivo.persistence.*;
import com.orchestrivo.service.*;

public class ServiceRegistry {
    // In a real app without DI framework, we do simple singletons
    public static UserRepository userRepository;
    public static WorkloadRepository workloadRepository;
    public static ExperimentRunRepository experimentRunRepository;

    public static AuthenticationService authService;
    public static WorkloadService workloadService;
    public static ExperimentService experimentService;

    public static void initialize(UserRepository ur, WorkloadRepository wr, ExperimentRunRepository er) {
        userRepository = ur;
        workloadRepository = wr;
        experimentRunRepository = er;

        authService = new AuthenticationService(ur);
        workloadService = new WorkloadService(wr);
        experimentService = new ExperimentService(er);
    }
}
