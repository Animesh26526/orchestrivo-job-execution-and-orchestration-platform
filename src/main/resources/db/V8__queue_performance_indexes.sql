-- V8__queue_performance_indexes.sql
-- Performance optimization: Composite index on (status, id) to eliminate filesort during queue claim

CREATE INDEX idx_jobs_status_id ON execution_jobs(status, id);
