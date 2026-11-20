CREATE TABLE IF NOT EXISTS job_definition (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_name VARCHAR(200),
    system_code VARCHAR(100),
    class_name VARCHAR(500),
    method_name VARCHAR(200),
    long_running TINYINT,
    description VARCHAR(500),
    status VARCHAR(20),
    enabled TINYINT DEFAULT 1,
    schedule_type VARCHAR(20),
    schedule_expr VARCHAR(100),
    next_fire_time TIMESTAMP,
    retry_count INT DEFAULT 0,
    retry_interval_ms BIGINT DEFAULT 1000,
    timeout_ms BIGINT,
    shard_index INT DEFAULT 0,
    shard_total INT DEFAULT 1,
    is_baseline_locked TINYINT DEFAULT 0,
    depends_on VARCHAR(200),
    namespace VARCHAR(100),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS executor_instance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    system_code VARCHAR(100),
    namespace VARCHAR(100),
    host VARCHAR(100),
    port INT,
    status VARCHAR(20),
    last_heartbeat TIMESTAMP,
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS config_change_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_name VARCHAR(200),
    change_type VARCHAR(50),
    old_value TEXT,
    new_value TEXT,
    operator VARCHAR(100),
    approved TINYINT,
    approved_by VARCHAR(100),
    approved_at TIMESTAMP,
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS job_execution_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_name VARCHAR(200),
    system_code VARCHAR(100),
    executor_host VARCHAR(100),
    status VARCHAR(20),
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    duration_ms BIGINT,
    error_message TEXT,
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator VARCHAR(100),
    operation VARCHAR(200),
    uri VARCHAR(300),
    params TEXT,
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS job_scheduler_lock (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lock_name VARCHAR(200),
    locked_until TIMESTAMP,
    created_at TIMESTAMP
);
