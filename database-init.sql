-- 创建数据库
CREATE DATABASE IF NOT EXISTS interview_simulator CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE interview_simulator;

-- 删除旧表（如果存在）
DROP TABLE IF EXISTS interview_reports;
DROP TABLE IF EXISTS interview_records;
DROP TABLE IF EXISTS interview_record;

-- 创建面试记录表
CREATE TABLE interview_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    interview_type VARCHAR(64) NOT NULL,
    position VARCHAR(128) NOT NULL,
    ai_model VARCHAR(64),
    actual_duration INT,
    video_file_path VARCHAR(255),
    audio_file_path VARCHAR(255),
    overall_score DOUBLE,
    overall_feedback TEXT,
    skill_assessment TEXT,
    question_answers TEXT,
    improvement_suggestions TEXT,
    report_file_path VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 如果表已存在，删除冗余字段并添加 actual_duration 字段
ALTER TABLE interview_records DROP COLUMN IF EXISTS start_time;
ALTER TABLE interview_records DROP COLUMN IF EXISTS end_time;
ALTER TABLE interview_records DROP COLUMN IF EXISTS updated_at;
ALTER TABLE interview_records ADD COLUMN IF NOT EXISTS actual_duration INT AFTER ai_model;

-- 创建面试报告表
CREATE TABLE interview_reports (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    interview_record_id BIGINT NOT NULL,
    overall_score DOUBLE,
    overall_feedback TEXT,
    skill_radar_data TEXT,
    key_issues TEXT,
    improvement_suggestions TEXT,
    performance_analysis TEXT,
    technical_assessment TEXT,
    soft_skill_assessment TEXT,
    generated_at DATETIME,
    report_file_path VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (interview_record_id) REFERENCES interview_records(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建索引
CREATE INDEX idx_interview_records_user_id ON interview_records(user_id);
CREATE INDEX idx_interview_records_type ON interview_records(interview_type);
CREATE INDEX idx_interview_records_created_at ON interview_records(created_at);
CREATE INDEX idx_interview_reports_record_id ON interview_reports(interview_record_id);

-- 创建用户（可选，如果使用root用户则不需要）
-- CREATE USER 'interview_user'@'localhost' IDENTIFIED BY 'interview_password';
-- GRANT ALL PRIVILEGES ON interview_simulator.* TO 'interview_user'@'localhost';
-- FLUSH PRIVILEGES;

-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建简历表
CREATE TABLE IF NOT EXISTS resume (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    filename VARCHAR(255) NOT NULL,
    upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    original_name VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 表结构将由Hibernate自动创建，这里不需要手动创建表 