-- V3__execution_jobs.sql
-- Additive schema migration for Stage 4: Persistent Background Execution Queue

CREATE TABLE IF NOT EXISTS execution_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_user_id BIGINT NOT NULL,
    workload_id BIGINT NOT NULL,
    algorithm_name VARCHAR(100) NOT NULL,
    algorithm_parameters JSON,
    input_snapshot JSON NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'QUEUED',
    attempt_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 3,
    claimed_by VARCHAR(100),
    claimed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    queue_wait_ms BIGINT,
    execution_duration_ms BIGINT,
    error_message TEXT,
    experiment_run_id BIGINT,
    idempotency_key VARCHAR(255),
    CONSTRAINT fk_job_owner FOREIGN KEY (owner_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_job_workload FOREIGN KEY (workload_id) REFERENCES workloads(id) ON DELETE CASCADE,
    CONSTRAINT fk_job_experiment FOREIGN KEY (experiment_run_id) REFERENCES experiment_runs(id) ON DELETE SET NULL
);

-- Performance & Queue Polling Indexes
CREATE INDEX idx_jobs_status_created ON execution_jobs(status, created_at);
CREATE INDEX idx_jobs_owner_status ON execution_jobs(owner_user_id, status, created_at);
CREATE INDEX idx_jobs_claimed_status ON execution_jobs(status, claimed_at);
CREATE INDEX idx_jobs_experiment_run ON execution_jobs(experiment_run_id);
CREATE UNIQUE INDEX uq_jobs_idempotency ON execution_jobs(owner_user_id, idempotency_key);
