package com.orchestrivo.service;

import com.orchestrivo.domain.ProcessSpec;
import com.orchestrivo.domain.Workload;
import com.orchestrivo.model.WorkloadRecord;
import com.orchestrivo.persistence.WorkloadRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WorkloadService {
    private final WorkloadRepository workloadRepository;

    public WorkloadService(WorkloadRepository workloadRepository) {
        this.workloadRepository = workloadRepository;
    }

    public WorkloadRecord createManualWorkload(long userId, String name, String description, List<ProcessSpec> processes) {
        Workload workload = new Workload(processes);
        WorkloadRecord record = new WorkloadRecord();
        record.setOwnerUserId(userId);
        record.setName(name);
        record.setDescription(description);
        record.setSourceType("MANUAL");
        record.setWorkload(workload);
        return workloadRepository.save(record);
    }

    public List<WorkloadRecord> getWorkloadsByUser(long userId) {
        return workloadRepository.findByOwnerId(userId);
    }

    public Optional<WorkloadRecord> getWorkloadById(long workloadId, long userId) {
        Optional<WorkloadRecord> opt = workloadRepository.findById(workloadId);
        if (opt.isPresent() && opt.get().getOwnerUserId() == userId) {
            return opt;
        }
        return Optional.empty();
    }
    
    public void deleteWorkload(long workloadId, long userId) {
        Optional<WorkloadRecord> opt = workloadRepository.findById(workloadId);
        if (opt.isPresent()) {
            if (opt.get().getOwnerUserId() != userId) {
                throw new SecurityException("Unauthorized to delete workload.");
            }
            workloadRepository.deleteById(workloadId);
        } else {
            throw new IllegalArgumentException("Workload not found.");
        }
    }
}
