package com.orchestrivo.service;

import com.google.gson.Gson;
import com.orchestrivo.domain.AlgorithmType;
import com.orchestrivo.domain.ScheduleResult;
import com.orchestrivo.domain.SchedulingConfig;
import com.orchestrivo.domain.Workload;
import com.orchestrivo.model.ExperimentRunRecord;
import com.orchestrivo.model.WorkloadRecord;
import com.orchestrivo.persistence.ExperimentRunRepository;
import com.orchestrivo.scheduling.SchedulingStrategy;
import com.orchestrivo.scheduling.factory.SchedulingStrategyFactory;
import com.orchestrivo.validation.ScheduleValidator;
import com.orchestrivo.domain.ValidationResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ExperimentService {
    private final ExperimentRunRepository runRepository;
    private final ScheduleValidator validator;

    public ExperimentService(ExperimentRunRepository runRepository) {
        this.runRepository = runRepository;
        this.validator = new ScheduleValidator();
    }

    public ExperimentRunRecord runExperiment(WorkloadRecord workloadRecord, String algorithmName, int quantum, long userId) {
        AlgorithmType type;
        try {
            type = AlgorithmType.valueOf(algorithmName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown algorithm: " + algorithmName);
        }

        SchedulingConfig config = new SchedulingConfig(type, quantum);
        SchedulingStrategy strategy = new SchedulingStrategyFactory().getStrategy(type);

        Workload workload = workloadRecord.getWorkload();
        
        ExperimentRunRecord record = new ExperimentRunRecord();
        record.setOwnerUserId(userId);
        record.setWorkloadId(workloadRecord.getId());
        record.setAlgorithmName(type.name());
        
        Map<String, Object> params = new HashMap<>();
        if (type == AlgorithmType.ROUND_ROBIN) {
            params.put("quantum", quantum);
        }
        record.setAlgorithmParameters(new Gson().toJson(params));

        // Create immutable process snapshot
        String snapshotJson = "[]";
        if (workload != null && workload.getProcesses() != null) {
            snapshotJson = new Gson().toJson(workload.getProcesses());
        }
        record.setInputSnapshot(snapshotJson);

        record.setStartedAt(LocalDateTime.now());
        record.setRunStatus("RUNNING");

        try {
            ScheduleResult result = strategy.schedule(workload, config);
            ValidationResult validation = validator.validate(workload, result);

            record.setResult(result);
            if (validation.isValid()) {
                record.setValidationStatus("VALID");
                record.setRunStatus("SUCCEEDED");
            } else {
                record.setValidationStatus("INVALID");
                record.setRunStatus("FAILED");
                record.setErrorMessage(String.join("; ", validation.getErrors()));
            }
        } catch (Exception e) {
            record.setRunStatus("FAILED");
            record.setValidationStatus("ERROR");
            record.setErrorMessage(e.getMessage());
        }

        record.setCompletedAt(LocalDateTime.now());
        
        return runRepository.save(record);
    }

    public com.orchestrivo.model.PagedResult<ExperimentRunRecord> getExperimentsPaged(long userId, Long workloadId, String algorithm, String sortBy, String sortDir, int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, Math.min(50, pageSize));
        int total = runRepository.countWithFilter(userId, workloadId, algorithm);
        int offset = (safePage - 1) * safePageSize;
        List<ExperimentRunRecord> items = runRepository.findWithFilter(userId, workloadId, algorithm, sortBy, sortDir, offset, safePageSize);
        return new com.orchestrivo.model.PagedResult<>(items, safePage, safePageSize, total);
    }

    public List<ExperimentRunRecord> getExperimentsByUser(long userId) {
        return runRepository.findByOwnerId(userId);
    }
    
    public Optional<ExperimentRunRecord> getExperimentById(long runId, long userId) {
        Optional<ExperimentRunRecord> opt = runRepository.findById(runId);
        if (opt.isPresent() && opt.get().getOwnerUserId() == userId) {
            return opt;
        }
        return Optional.empty();
    }

    public void deleteExperiment(long runId, long userId) {
        Optional<ExperimentRunRecord> opt = runRepository.findById(runId);
        if (opt.isPresent()) {
            if (opt.get().getOwnerUserId() != userId) {
                throw new SecurityException("Unauthorized to delete experiment run.");
            }
            runRepository.deleteById(runId);
        } else {
            throw new IllegalArgumentException("Experiment run not found.");
        }
    }

    public com.orchestrivo.model.ComparisonResult compareExperiments(List<Long> runIds, long userId) {
        if (runIds == null || runIds.isEmpty()) {
            throw new IllegalArgumentException("At least one experiment run must be selected for comparison.");
        }
        if (runIds.size() > 10) {
            throw new IllegalArgumentException("A maximum of 10 experiment runs can be compared at one time.");
        }

        List<ExperimentRunRecord> runs = new ArrayList<>();
        String firstSnapshot = null;
        boolean compatible = true;
        String compatibilityMessage = "All compared runs were executed on identical workload inputs.";

        for (Long id : runIds) {
            if (id == null) continue;
            Optional<ExperimentRunRecord> opt = runRepository.findById(id);
            if (opt.isEmpty()) {
                throw new IllegalArgumentException("Experiment run " + id + " not found.");
            }
            ExperimentRunRecord run = opt.get();
            if (run.getOwnerUserId() != userId) {
                throw new SecurityException("Unauthorized access to experiment run " + id);
            }
            runs.add(run);

            String snap = run.getInputSnapshot();
            if (snap == null || snap.trim().isEmpty()) {
                // If old run before migration had no snapshot, fall back to checking workload ID
                snap = "workload_id:" + run.getWorkloadId();
            }

            if (firstSnapshot == null) {
                firstSnapshot = snap;
            } else if (!firstSnapshot.equals(snap)) {
                compatible = false;
                compatibilityMessage = "Incompatible workloads: The selected runs were executed on different workloads or different process specifications. Controlled comparison requires identical workload inputs.";
            }
        }

        String tradeOffNotes = "Scheduling Trade-Off Observations:\n"
                + "- SJF (Shortest Job First) minimizes average waiting time when burst times vary, but may starve longer processes under continuous arrivals.\n"
                + "- Round Robin ensures fair CPU sharing and bounded response times, but introduces context switching overhead depending on the chosen quantum.\n"
                + "- SRTF (Shortest Remaining Time First) can improve turnaround time via preemption at the expense of frequent context switches.\n"
                + "- FCFS (First-Come, First-Served) is non-preemptive and simple, but suffers from the convoy effect when long processes arrive early.\n"
                + "Note: These represent general theoretical trade-offs; actual results depend entirely on the specific process arrival and burst distribution.";

        return new com.orchestrivo.model.ComparisonResult(runs, compatible, compatibilityMessage, tradeOffNotes);
    }
}
