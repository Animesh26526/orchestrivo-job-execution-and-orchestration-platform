-- V7__final_readiness_pass.sql
-- Additive migration: Actual batch wall-clock duration, valid run count, quantum, and persistent system settings

ALTER TABLE benchmark_runs
    ADD COLUMN batch_elapsed_nanos BIGINT NULL AFTER max_nanos,
    ADD COLUMN valid_run_count INT NULL AFTER batch_elapsed_nanos,
    ADD COLUMN quantum INT NULL AFTER process_count;

CREATE TABLE IF NOT EXISTS system_settings (
    setting_key VARCHAR(64) NOT NULL PRIMARY KEY,
    setting_value VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
