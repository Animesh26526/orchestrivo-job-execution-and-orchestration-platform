package com.orchestrivo.service;

import com.google.gson.Gson;
import com.orchestrivo.domain.AlgorithmType;
import com.orchestrivo.model.AlgorithmConfigSpec;
import com.orchestrivo.model.BatchJobResult;
import com.orchestrivo.model.ExecutionJobRecord;
import com.orchestrivo.model.ExecutionJobStatus;
import com.orchestrivo.model.PagedResult;
import com.orchestrivo.model.QueueMetrics;
import com.orchestrivo.model.WorkloadRecord;
import com.orchestrivo.persistence.ExecutionJobRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ExecutionJobService {

    private final ExecutionJobRepository jobRepository;
    private Runnable jobSignalListener;

    public ExecutionJobService(ExecutionJobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public void setJobSignalListener(Runnable listener) {
        this.jobSignalListener = listener;
    }

    private void notifyWorker() {
        if (jobSignalListener != null) {
            try {
                jobSignalListener.run();
            } catch (Exception ignored) {}
        }
    }

    public ExecutionJobRecord enqueueExperiment(WorkloadRecord workloadRecord, String algorithmName, int quantum, long userId, String idempotencyKey) {
        if (workloadRecord == null) {
            throw new IllegalArgumentException("Workload cannot be null.");
        }
        if (workloadRecord.getOwnerUserId() != userId) {
            throw new SecurityException("Unauthorized: Workload is not owned by user.");
        }

        // Idempotency check: if key is present, check for existing job
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            Optional<ExecutionJobRecord> existing = jobRepository.findByIdempotencyKey(userId, idempotencyKey.trim());
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        AlgorithmType type;
        try {
            type = AlgorithmType.valueOf(algorithmName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown algorithm: " + algorithmName);
        }

        Map<String, Object> params = new HashMap<>();
        if (type == AlgorithmType.ROUND_ROBIN) {
            if (quantum < 1) {
                throw new IllegalArgumentException("Time quantum must be at least 1.");
            }
            params.put("quantum", quantum);
        }

        String snapshotJson = com.orchestrivo.model.WorkloadSnapshot.serialize(workloadRecord.getWorkload(), workloadRecord.getGenerationSeed());

        ExecutionJobRecord job = new ExecutionJobRecord();
        job.setOwnerUserId(userId);
        job.setWorkloadId(workloadRecord.getId());
        job.setAlgorithmName(type.name());
        job.setAlgorithmParameters(new Gson().toJson(params));
        job.setInputSnapshot(snapshotJson);
        job.setStatus(ExecutionJobStatus.QUEUED);
        job.setAttemptCount(0);
        job.setMaxAttempts(3);
        job.setIdempotencyKey(idempotencyKey != null && !idempotencyKey.trim().isEmpty() ? idempotencyKey.trim() : null);

        ExecutionJobRecord savedJob;
        try {
            savedJob = jobRepository.save(job);
        } catch (Exception e) {
            if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
                Optional<ExecutionJobRecord> existing = jobRepository.findByIdempotencyKey(userId, idempotencyKey.trim());
                if (existing.isPresent()) {
                    return existing.get();
                }
            }
            throw e;
        }
        notifyWorker();
        return savedJob;
    }

    public Optional<ExecutionJobRecord> getJobById(long jobId, long userId) {
        return jobRepository.findByIdAndOwner(jobId, userId);
    }

    public PagedResult<ExecutionJobRecord> getJobsPaged(long userId, String statusFilter, String sortBy, String sortDir, int page, int pageSize) {
        return jobRepository.findByOwnerPaged(userId, statusFilter, sortBy, sortDir, page, pageSize);
    }

    public List<ExecutionJobRecord> getRecentJobs(long userId, int limit) {
        return jobRepository.findRecentByOwner(userId, limit);
    }

    public boolean retryJob(long jobId, long userId) {
        Optional<ExecutionJobRecord> opt = jobRepository.findByIdAndOwner(jobId, userId);
        if (opt.isEmpty()) {
            throw new SecurityException("Job not found or not owned.");
        }
        ExecutionJobRecord job = opt.get();
        if (!job.isEligibleForRetry()) {
            throw new IllegalStateException("Job is not eligible for retry.");
        }
        boolean ok = jobRepository.requestRetry(jobId, userId);
        if (ok) {
            notifyWorker();
        }
        return ok;
    }

    public boolean cancelJob(long jobId, long userId) {
        Optional<ExecutionJobRecord> opt = jobRepository.findByIdAndOwner(jobId, userId);
        if (opt.isEmpty()) {
            throw new SecurityException("Job not found or not owned.");
        }
        ExecutionJobRecord job = opt.get();
        if (!job.isEligibleForCancellation()) {
            throw new IllegalStateException("Only queued jobs can be cancelled.");
        }
        return jobRepository.cancelJob(jobId, userId);
    }

    public QueueMetrics getQueueMetrics(long userId) {
        return jobRepository.getQueueMetrics(userId);
    }

    public QueueMetrics getGlobalQueueMetrics() {
        return jobRepository.getGlobalQueueMetrics();
    }

    public BatchJobResult enqueueBatch(WorkloadRecord workloadRecord, List<AlgorithmConfigSpec> configs, long userId) {
        if (workloadRecord == null) {
            throw new IllegalArgumentException("Workload cannot be null.");
        }
        if (workloadRecord.getOwnerUserId() != userId) {
            throw new SecurityException("Unauthorized: Workload is not owned by user.");
        }
        if (configs == null || configs.isEmpty()) {
            throw new IllegalArgumentException("At least one algorithm configuration must be provided.");
        }
        if (configs.size() > 20) {
            throw new IllegalArgumentException("Batch size limit exceeded. Maximum 20 jobs allowed per batch (requested: " + configs.size() + ").");
        }

        // 1. Atomic pre-validation of all configurations
        for (AlgorithmConfigSpec spec : configs) {
            if (spec.getAlgorithmName() == null || spec.getAlgorithmName().trim().isEmpty()) {
                throw new IllegalArgumentException("Algorithm name cannot be empty.");
            }
            AlgorithmType type;
            try {
                type = AlgorithmType.valueOf(spec.getAlgorithmName().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown or unsupported algorithm: " + spec.getAlgorithmName());
            }
            if (type == AlgorithmType.ROUND_ROBIN && spec.getQuantum() < 1) {
                throw new IllegalArgumentException("Time quantum must be at least 1 for Round Robin.");
            }
        }

        String batchId = "batch-" + UUID.randomUUID().toString();
        List<ExecutionJobRecord> createdJobs = new ArrayList<>();

        // 2. Enqueue all validated jobs
        for (AlgorithmConfigSpec spec : configs) {
            AlgorithmType type = AlgorithmType.valueOf(spec.getAlgorithmName().trim().toUpperCase());
            Map<String, Object> params = new HashMap<>();
            if (type == AlgorithmType.ROUND_ROBIN) {
                params.put("quantum", spec.getQuantum());
            }

            String snapshotJson = com.orchestrivo.model.WorkloadSnapshot.serialize(workloadRecord.getWorkload(), workloadRecord.getGenerationSeed());

            ExecutionJobRecord job = new ExecutionJobRecord();
            job.setOwnerUserId(userId);
            job.setWorkloadId(workloadRecord.getId());
            job.setAlgorithmName(type.name());
            job.setAlgorithmParameters(new Gson().toJson(params));
            job.setInputSnapshot(snapshotJson);
            job.setStatus(ExecutionJobStatus.QUEUED);
            job.setAttemptCount(0);
            job.setMaxAttempts(3);
            job.setBatchId(batchId);

            ExecutionJobRecord saved = jobRepository.save(job);
            createdJobs.add(saved);
        }

        notifyWorker();
        return new BatchJobResult(batchId, workloadRecord.getId(), createdJobs);
    }

    public List<ExecutionJobRecord> getJobsByBatchId(String batchId, long userId) {
        if (batchId == null || batchId.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return jobRepository.findByBatchId(batchId.trim(), userId);
    }
}
