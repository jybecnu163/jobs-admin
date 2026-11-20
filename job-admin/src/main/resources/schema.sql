-- Job 管理系统建表语句
CREATE DATABASE IF NOT EXISTS job_admin DEFAULT CHARSET utf8mb4;
USE job_admin;

CREATE TABLE IF NOT EXISTS job_definition (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_name VARCHAR(200) NOT NULL UNIQUE,
    system_code VARCHAR(100) NOT NULL,
    class_name VARCHAR(500),
    method_name VARCHAR(200),
    long_running TINYINT DEFAULT 0 COMMENT '是否长任务',
    description VARCHAR(500),
    status VARCHAR(20) DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED/UNREACHABLE',
    enabled TINYINT DEFAULT 1 COMMENT '管理员启用意图，实例状态变化不改变该值',
    schedule_type VARCHAR(20) COMMENT 'CRON/FIXED_RATE/NONE',
    schedule_expr VARCHAR(100),
    is_baseline_locked TINYINT DEFAULT 0 COMMENT '是否被基线锁定',
    next_fire_time DATETIME COMMENT '下次触发时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS executor_instance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    system_code VARCHAR(100) NOT NULL,
    host VARCHAR(100),
    port INT,
    status VARCHAR(20) DEFAULT 'ONLINE' COMMENT 'ONLINE/OFFLINE',
    last_heartbeat DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS config_change_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_name VARCHAR(200),
    change_type VARCHAR(50),
    old_value TEXT,
    new_value TEXT,
    operator VARCHAR(100),
    approved TINYINT DEFAULT 0 COMMENT '0待审批,1已批准,2已拒绝',
    approved_by VARCHAR(100),
    approved_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS job_scheduler_lock (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lock_name VARCHAR(200) NOT NULL UNIQUE,
    locked_until DATETIME NOT NULL COMMENT '锁过期时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS job_execution_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_name VARCHAR(200),
    system_code VARCHAR(100),
    status VARCHAR(20) COMMENT 'SUCCESS/FAILED/STOPPED',
    start_time DATETIME,
    end_time DATETIME,
    duration_ms BIGINT,
    error_message TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
