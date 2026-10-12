package com.orchestrivo.service;

import com.orchestrivo.domain.ProcessSpec;
import com.orchestrivo.domain.Workload;
import com.orchestrivo.model.WorkloadRecord;
import com.orchestrivo.persistence.WorkloadRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.orchestrivo.model.PagedResult;

public class WorkloadService {
    private final WorkloadRepository workloadRepository;

    public WorkloadService(WorkloadRepository workloadRepository) {
        this.workloadRepository = workloadRepository;
    }

    public WorkloadRecord createManualWorkload(long userId, String name, String description, List<ProcessSpec> processes) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Workload name cannot be empty.");
        }
        Workload workload = new Workload(processes);
        if (workload.isEmpty()) {
            throw new IllegalArgumentException("Workload cannot be empty.");
        }
        WorkloadRecord record = new WorkloadRecord();
        record.setOwnerUserId(userId);
        record.setName(name.trim());
        record.setDescription(description);
        record.setSourceType("MANUAL");
        record.setWorkload(workload);
        return workloadRepository.save(record);
    }

    public WorkloadRecord updateWorkload(long workloadId, long userId, String name, String description, List<ProcessSpec> processes) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Workload name cannot be empty.");
        }
        Optional<WorkloadRecord> opt = workloadRepository.findById(workloadId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Workload not found.");
        }
        WorkloadRecord record = opt.get();
        if (record.getOwnerUserId() != userId) {
            throw new SecurityException("Unauthorized to edit workload.");
        }

        Workload validatedWorkload = new Workload(processes);
        if (validatedWorkload.isEmpty()) {
            throw new IllegalArgumentException("Workload cannot be empty.");
        }

        record.setName(name.trim());
        record.setDescription(description);
        record.setWorkload(validatedWorkload);

        return workloadRepository.update(record);
    }

    public WorkloadRecord duplicateWorkload(long workloadId, long userId, String newName) {
        Optional<WorkloadRecord> opt = workloadRepository.findById(workloadId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Workload not found.");
        }
        WorkloadRecord original = opt.get();
        if (original.getOwnerUserId() != userId) {
            throw new SecurityException("Unauthorized to duplicate workload.");
        }

        String targetName = (newName != null && !newName.trim().isEmpty())
                ? newName.trim()
                : original.getName() + " (Copy)";

        WorkloadRecord duplicate = new WorkloadRecord();
        duplicate.setOwnerUserId(userId);
        duplicate.setName(targetName);
        duplicate.setDescription(original.getDescription());
        duplicate.setSourceType("DUPLICATE");
        duplicate.setGenerationSeed(original.getGenerationSeed());
        duplicate.setGenerationConfig(original.getGenerationConfig());

        List<ProcessSpec> copiedProcesses = new ArrayList<>();
        if (original.getWorkload() != null) {
            for (ProcessSpec p : original.getWorkload().getProcesses()) {
                copiedProcesses.add(new ProcessSpec(p.getProcessId(), p.getArrivalTime(), p.getBurstTime()));
            }
        }
        duplicate.setWorkload(new Workload(copiedProcesses));

        return workloadRepository.save(duplicate);
    }

    public PagedResult<WorkloadRecord> getWorkloadsPaged(long userId, String search, String sortBy, String sortDir, int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, Math.min(50, pageSize));
        int total = workloadRepository.countWithFilter(userId, search);
        int offset = (safePage - 1) * safePageSize;
        List<WorkloadRecord> items = workloadRepository.findWithFilter(userId, search, sortBy, sortDir, offset, safePageSize);
        return new PagedResult<>(items, safePage, safePageSize, total);
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
