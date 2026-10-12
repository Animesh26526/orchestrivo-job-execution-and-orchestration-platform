package com.orchestrivo.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.orchestrivo.domain.*;
import com.orchestrivo.model.ExecutionJobRecord;
import com.orchestrivo.model.ExperimentRunRecord;
import com.orchestrivo.persistence.ExecutionJobRepository;
import com.orchestrivo.persistence.ExperimentRunRepository;
import com.orchestrivo.scheduling.SchedulingStrategy;
import com.orchestrivo.scheduling.factory.SchedulingStrategyFactory;
import com.orchestrivo.validation.ScheduleValidator;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ExecutionWorkerManager {

    private final ExecutionJobRepository jobRepository;
    private final ExperimentRunRepository experimentRunRepository;
    private final AuditService auditService;
    private final ScheduleValidator validator;
    private final int workerCount;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledExecutorService pollerExecutor;
    private ScheduledExecutorService heartbeatScheduler;
    private ThreadPoolExecutor workerPool;
    private final Semaphore taskLimiter;
    private final String instanceId = "worker-host-" + UUID.randomUUID().toString().substring(0, 8);

    public ExecutionWorkerManager(ExecutionJobRepository jobRepository,
                                  ExperimentRunRepository experimentRunRepository,
                                  AuditService auditService) {
        this(jobRepository, experimentRunRepository, auditService, 2);
    }

    public ExecutionWorkerManager(ExecutionJobRepository jobRepository,
                                  ExperimentRunRepository experimentRunRepository,
                                  AuditService auditService,
                                  int workerCount) {
        this.jobRepository = jobRepository;
        this.experimentRunRepository = experimentRunRepository;
        this.auditService = auditService;
        this.validator = new ScheduleValidator();
        this.workerCount = Math.max(1, Math.min(8, workerCount));
        this.taskLimiter = new Semaphore(this.workerCount);
    }

    public synchronized void start() {
        if (running.compareAndSet(false, true)) {
            // 1. Crash recovery: recover any stale running jobs from prior crashes
            try {
                int recovered = jobRepository.recoverStaleJobs(60);
                if (recovered > 0) {
                    System.out.println("[ExecutionWorkerManager] Recovered " + recovered + " stale jobs from previous run.");
                }
            } catch (Exception e) {
                System.err.println("[ExecutionWorkerManager] Stale job recovery warning: " + e.getMessage());
            }

            // 2. Initialize bounded worker thread pool
            workerPool = new ThreadPoolExecutor(
                    workerCount, workerCount,
                    60L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(workerCount * 2),
                    new ThreadFactory() {
                        private int counter = 1;
                        @Override
                        public Thread newThread(Runnable r) {
                            Thread t = new Thread(r, "orchestrivo-worker-" + (counter++));
                            t.setDaemon(true);
                            return t;
                        }
                    },
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );

            // 3. Initialize poller loop and heartbeat scheduler
            pollerExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "orchestrivo-queue-poller");
                t.setDaemon(true);
                return t;
            });

            heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "orchestrivo-worker-heartbeat");
                t.setDaemon(true);
                return t;
            });

            pollerExecutor.scheduleWithFixedDelay(this::pollAndDispatch, 500, 1000, TimeUnit.MILLISECONDS);
            System.out.println("[ExecutionWorkerManager] Started with " + workerCount + " bounded workers (Instance: " + instanceId + ")");
        }
    }

    public synchronized void shutdown() {
        if (running.compareAndSet(true, false)) {
            System.out.println("[ExecutionWorkerManager] Shutting down background execution engine...");
            if (pollerExecutor != null) {
                pollerExecutor.shutdownNow();
            }
            if (heartbeatScheduler != null) {
                heartbeatScheduler.shutdownNow();
            }
            if (workerPool != null) {
                workerPool.shutdown();
                try {
                    if (!workerPool.awaitTermination(3, TimeUnit.SECONDS)) {
                        workerPool.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    workerPool.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
            System.out.println("[ExecutionWorkerManager] Background engine stopped cleanly.");
        }
    }

    public void triggerImmediatePoll() {
        if (running.get() && pollerExecutor != null && !pollerExecutor.isShutdown()) {
            pollerExecutor.execute(this::pollAndDispatch);
        }
    }

    public void registerJobService(ExecutionJobService jobService) {
        if (jobService != null) {
            jobService.setJobSignalListener(this::triggerImmediatePoll);
        }
    }

    private void pollAndDispatch() {
        if (!running.get()) return;

        while (running.get() && taskLimiter.tryAcquire()) {
            try {
                Optional<ExecutionJobRecord> claimed = jobRepository.claimNextQueuedJob(instanceId);
                if (claimed.isPresent()) {
                    ExecutionJobRecord job = claimed.get();
                    workerPool.execute(() -> {
                        try {
                            executeJob(job);
                        } finally {
                            taskLimiter.release();
                            triggerImmediatePoll();
                        }
                    });
                } else {
                    taskLimiter.release();
                    break; // No queued jobs available right now
                }
            } catch (Exception e) {
                taskLimiter.release();
                System.err.println("[ExecutionWorkerManager] Polling loop error: " + e.getMessage());
                break;
            }
        }
    }

    public void executeJob(ExecutionJobRecord job) {
        long startNanos = System.nanoTime();
        ScheduledFuture<?> heartbeatTask = null;
        AtomicBoolean leaseLost = new AtomicBoolean(false);

        if (heartbeatScheduler != null && !heartbeatScheduler.isShutdown() && job.getClaimToken() != null) {
            heartbeatTask = heartbeatScheduler.scheduleWithFixedDelay(() -> {
                try {
                    boolean renewed = jobRepository.renewLease(job.getId(), job.getClaimToken(), 30);
                    if (!renewed) {
                        leaseLost.set(true);
                    }
                } catch (Exception ignored) {}
            }, 5, 5, TimeUnit.SECONDS);
        }

        try {
            // 1. Parse algorithm and parameters
            AlgorithmType algorithmType = AlgorithmType.valueOf(job.getAlgorithmName().toUpperCase());
            int quantum = 3;
            if (job.getAlgorithmParameters() != null && !job.getAlgorithmParameters().trim().isEmpty()) {
                try {
                    Map<String, Object> pMap = new Gson().fromJson(job.getAlgorithmParameters(), new TypeToken<Map<String, Object>>(){}.getType());
                    if (pMap != null && pMap.containsKey("quantum")) {
                        quantum = ((Number) pMap.get("quantum")).intValue();
                    }
                } catch (Exception ignored) {}
            }

            // 2. Deserialize immutable snapshot (supporting versioned object and legacy raw array)
            List<ProcessSpec> processes = com.orchestrivo.model.WorkloadSnapshot.extractProcesses(job.getInputSnapshot());
            if (processes == null || processes.isEmpty()) {
                throw new IllegalArgumentException("Job input snapshot contains zero process specifications.");
            }

            Workload workload = new Workload(processes);
            SchedulingConfig config = new SchedulingConfig(algorithmType, quantum);
            SchedulingStrategy strategy = new SchedulingStrategyFactory().getStrategy(algorithmType);

            // 3. Execute scheduling computation
            ScheduleResult result = strategy.schedule(workload, config);

            // Check lease status before persisting
            if (leaseLost.get()) {
                System.err.println("[ExecutionWorkerManager] Job #" + job.getId() + " lease was lost during computation; discarding results.");
                return;
            }

            // 4. Validate output schedule
            ValidationResult validation = validator.validate(workload, result);

            long executionDurationNanos = Math.max(1L, System.nanoTime() - startNanos);
            long executionDurationMs = executionDurationNanos / 1_000_000;

            // 5. Prepare experiment run record
            ExperimentRunRecord runRecord = new ExperimentRunRecord();
            runRecord.setOwnerUserId(job.getOwnerUserId());
            runRecord.setWorkloadId(job.getWorkloadId());
            runRecord.setAlgorithmName(algorithmType.name());
            runRecord.setAlgorithmParameters(job.getAlgorithmParameters());
            runRecord.setInputSnapshot(job.getInputSnapshot());
            runRecord.setResult(result);
            runRecord.setStartedAt(job.getStartedAt() != null ? job.getStartedAt() : LocalDateTime.now());
            runRecord.setCompletedAt(LocalDateTime.now());

            if (validation.isValid()) {
                runRecord.setValidationStatus("VALID");
                runRecord.setRunStatus("SUCCEEDED");

                // 6. Atomically complete job with experiment run record
                boolean completed = jobRepository.completeJobWithExperimentRun(job.getId(), job.getClaimToken(), runRecord, executionDurationMs, executionDurationNanos);
                if (completed) {
                    if (auditService != null) {
                        auditService.log(job.getOwnerUserId(), "JOB_COMPLETED", "EXECUTION_JOB",
                                String.valueOf(job.getId()),
                                Map.of("algorithm", job.getAlgorithmName(), "durationMs", executionDurationMs, "durationNanos", executionDurationNanos, "experimentRunId", runRecord.getId()));
                    }
                } else {
                    System.err.println("[ExecutionWorkerManager] Job #" + job.getId() + " claim token expired or was reclaimed before completion; transaction aborted.");
                }
            } else {
                runRecord.setValidationStatus("INVALID");
                runRecord.setRunStatus("FAILED");
                runRecord.setErrorMessage(String.join("; ", validation.getErrors()));

                // Validation error is non-transient, mark FAILED without retry
                jobRepository.markFailed(job.getId(), job.getClaimToken(), "Schedule validation failure: " + String.join("; ", validation.getErrors()), false);
                if (auditService != null) {
                    auditService.log(job.getOwnerUserId(), "JOB_FAILED", "EXECUTION_JOB",
                            String.valueOf(job.getId()),
                            Map.of("reason", "Validation failure"));
                }
            }

        } catch (Exception e) {
            long executionDurationMs = (System.nanoTime() - startNanos) / 1_000_000;
            System.err.println("[ExecutionWorkerManager] Job #" + job.getId() + " execution failure: " + e.getMessage());

            boolean canRetry = job.getAttemptCount() < job.getMaxAttempts();
            jobRepository.markFailed(job.getId(), job.getClaimToken(), "Execution error: " + e.getMessage(), canRetry);

            if (auditService != null) {
                auditService.log(job.getOwnerUserId(), "JOB_FAILED", "EXECUTION_JOB",
                        String.valueOf(job.getId()),
                        Map.of("reason", e.getMessage() != null ? e.getMessage() : "Unknown", "retryScheduled", canRetry));
            }
        } finally {
            if (heartbeatTask != null) {
                heartbeatTask.cancel(false);
            }
        }
    }

    public boolean isRunning() {
        return running.get();
    }
}
