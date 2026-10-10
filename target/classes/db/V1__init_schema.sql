-- V1__init_schema.sql
-- Orchestrivo Database Schema Foundation

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE workloads (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    source_type VARCHAR(50) NOT NULL,
    generation_seed BIGINT,
    generation_config JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_workload_owner FOREIGN KEY (owner_user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE processes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workload_id BIGINT NOT NULL,
    process_key VARCHAR(100) NOT NULL,
    arrival_time INT NOT NULL,
    burst_time INT NOT NULL,
    priority INT,
    deadline INT,
    CONSTRAINT fk_process_workload FOREIGN KEY (workload_id) REFERENCES workloads(id) ON DELETE CASCADE,
    UNIQUE KEY uk_workload_process (workload_id, process_key)
);

CREATE TABLE experiment_runs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_user_id BIGINT NOT NULL,
    workload_id BIGINT NOT NULL,
    algorithm_name VARCHAR(100) NOT NULL,
    algorithm_parameters JSON,
    run_status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    validation_status VARCHAR(50),
    average_waiting_time DOUBLE,
    average_turnaround_time DOUBLE,
    average_response_time DOUBLE,
    simulated_throughput DOUBLE,
    context_switches INT,
    cpu_idle_time INT,
    total_duration INT,
    error_message TEXT,
    CONSTRAINT fk_run_owner FOREIGN KEY (owner_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_run_workload FOREIGN KEY (workload_id) REFERENCES workloads(id) ON DELETE CASCADE
);

CREATE TABLE process_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    experiment_run_id BIGINT NOT NULL,
    process_key VARCHAR(100) NOT NULL,
    first_start_time INT NOT NULL,
    completion_time INT NOT NULL,
    waiting_time INT NOT NULL,
    turnaround_time INT NOT NULL,
    response_time INT NOT NULL,
    CONSTRAINT fk_result_run FOREIGN KEY (experiment_run_id) REFERENCES experiment_runs(id) ON DELETE CASCADE
);

CREATE TABLE execution_segments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    experiment_run_id BIGINT NOT NULL,
    segment_sequence INT NOT NULL,
    process_key VARCHAR(100) NOT NULL, -- IDLE or process ID
    start_time INT NOT NULL,
    end_time INT NOT NULL,
    CONSTRAINT fk_segment_run FOREIGN KEY (experiment_run_id) REFERENCES experiment_runs(id) ON DELETE CASCADE
);

CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_user_id BIGINT,
    event_type VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100),
    resource_id VARCHAR(255),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata JSON,
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE SET NULL
);
