package com.orchestrivo.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Workload {
    private final List<ProcessSpec> processes;

    public Workload(List<ProcessSpec> processes) {
        if (processes == null) {
            throw new IllegalArgumentException("Processes list cannot be null");
        }
        
        Set<String> ids = new HashSet<>();
        for (ProcessSpec p : processes) {
            if (p == null) {
                throw new IllegalArgumentException("Process specification cannot be null");
            }
            if (!ids.add(p.getProcessId())) {
                throw new IllegalArgumentException("Duplicate process ID found in workload: " + p.getProcessId());
            }
        }
        this.processes = Collections.unmodifiableList(new ArrayList<>(processes));
    }

    public List<ProcessSpec> getProcesses() {
        return processes;
    }
    
    public boolean isEmpty() {
        return processes.isEmpty();
    }
}
