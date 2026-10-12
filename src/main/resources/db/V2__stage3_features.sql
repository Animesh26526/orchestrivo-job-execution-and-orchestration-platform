-- V2__stage3_features.sql
-- Additive schema migration for Stage 3: Snapshots, Benchmarking, and Indexing

-- 1. Add immutable input snapshot to experiment_runs
ALTER TABLE experiment_runs
    ADD COLUMN input_snapshot JSON AFTER algorithm_parameters;

-- 2. Create benchmark_runs table
CREATE TABLE IF NOT EXISTS benchmark_runs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_user_id BIGINT NOT NULL,
    algorithm_name VARCHAR(100) NOT NULL,
    process_count INT NOT NULL,
    seed BIGINT NOT NULL,
    warmup_iterations INT NOT NULL,
    measured_iterations INT NOT NULL,
    median_nanos DOUBLE NOT NULL,
    min_nanos BIGINT NOT NULL,
    max_nanos BIGINT NOT NULL,
    std_dev_nanos DOUBLE NOT NULL,
    validation_passed BOOLEAN NOT NULL DEFAULT TRUE,
    environment_info JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_benchmark_owner FOREIGN KEY (owner_user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 3. Create benchmark_measurements table
CREATE TABLE IF NOT EXISTS benchmark_measurements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    benchmark_run_id BIGINT NOT NULL,
    iteration_number INT NOT NULL,
    duration_nanos BIGINT NOT NULL,
    CONSTRAINT fk_measurement_run FOREIGN KEY (benchmark_run_id) REFERENCES benchmark_runs(id) ON DELETE CASCADE
);

-- 4. Performance Indexes for Search, Filtering, and Pagination
CREATE INDEX idx_workloads_owner_name ON workloads(owner_user_id, name);
CREATE INDEX idx_runs_owner_workload ON experiment_runs(owner_user_id, workload_id);
CREATE INDEX idx_runs_owner_algo ON experiment_runs(owner_user_id, algorithm_name);
CREATE INDEX idx_audit_actor_timestamp ON audit_logs(actor_user_id, timestamp);
CREATE INDEX idx_benchmarks_owner ON benchmark_runs(owner_user_id, created_at);
