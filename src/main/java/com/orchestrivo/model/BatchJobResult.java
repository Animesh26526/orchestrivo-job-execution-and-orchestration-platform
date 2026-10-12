package com.orchestrivo.model;

import java.util.Collections;
import java.util.List;

public class BatchJobResult {
    private String batchId;
    private long workloadId;
    private List<ExecutionJobRecord> createdJobs;

    public BatchJobResult() {}

    public BatchJobResult(String batchId, long workloadId, List<ExecutionJobRecord> createdJobs) {
        this.batchId = batchId;
        this.workloadId = workloadId;
        this.createdJobs = createdJobs != null ? createdJobs : Collections.emptyList();
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public long getWorkloadId() {
        return workloadId;
    }

    public void setWorkloadId(long workloadId) {
        this.workloadId = workloadId;
    }

    public List<ExecutionJobRecord> getCreatedJobs() {
        return createdJobs;
    }

    public void setCreatedJobs(List<ExecutionJobRecord> createdJobs) {
        this.createdJobs = createdJobs;
    }

    public int getJobCount() {
        return createdJobs != null ? createdJobs.size() : 0;
    }
}
