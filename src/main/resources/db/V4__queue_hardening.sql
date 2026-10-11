-- V4__queue_hardening.sql
-- Additive schema migration for Stage 4.1: Queue Hardening, Attempt Fencing & Lease Renewal

ALTER TABLE execution_jobs
    ADD COLUMN claim_token VARCHAR(64) NULL AFTER claimed_by,
    ADD COLUMN lease_expires_at TIMESTAMP NULL AFTER claimed_at;

CREATE INDEX idx_jobs_lease_expires ON execution_jobs(status, lease_expires_at);
CREATE INDEX idx_jobs_claim_token ON execution_jobs(claim_token);
