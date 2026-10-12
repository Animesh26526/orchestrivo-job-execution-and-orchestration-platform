-- V6__stage4_2_features.sql
-- Additive schema migration for Stage 4.2: High-Resolution Timing, Batch Execution & Benchmark Comparison

ALTER TABLE execution_jobs
    ADD COLUMN execution_duration_nanos BIGINT NULL AFTER execution_duration_ms,
    ADD COLUMN batch_id VARCHAR(64) NULL AFTER idempotency_key;

CREATE INDEX idx_jobs_batch ON execution_jobs(batch_id);

ALTER TABLE benchmark_runs
    ADD COLUMN run_type VARCHAR(50) NOT NULL DEFAULT 'CANDIDATE' AFTER validation_passed,
    ADD COLUMN benchmark_group VARCHAR(100) NULL AFTER run_type;

CREATE INDEX idx_benchmarks_group ON benchmark_runs(benchmark_group, run_type);
