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
}
