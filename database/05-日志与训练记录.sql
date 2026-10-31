-- 在 xixu_dev 中执行一次。先确认 app_user 已存在。
-- 一条 life_log_entry 是一天中的一则文字日志或一次训练；训练组次另存，便于统计。

CREATE TABLE life_log_entry (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    entry_type VARCHAR(10) NOT NULL COMMENT 'NOTE 或 WORKOUT',
    category_name VARCHAR(30) NOT NULL DEFAULT '随记' COMMENT '原始记录类别，如睡眠、运动、饮食',
    occurred_on DATE NOT NULL COMMENT '实际发生日期',
    time_label VARCHAR(50) NULL COMMENT '原始时间口径，允许区间、全天或跨夜描述',
    title VARCHAR(100) NOT NULL,
    content VARCHAR(2000) NULL COMMENT '原始描述；未知信息不补造',
    remark VARCHAR(2000) NULL COMMENT '来源、估算、更正等原始备注',
    source_type VARCHAR(20) NOT NULL DEFAULT 'MANUAL' COMMENT 'MANUAL、AGENT 或 IMPORT',
    source_ref VARCHAR(150) NULL COMMENT '导入来源定位；手动记录留空',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_life_log_user_date (user_id, occurred_on, id),
    UNIQUE KEY uk_life_log_user_source (user_id, source_ref),
    CONSTRAINT fk_life_log_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE RESTRICT,
    CONSTRAINT chk_life_log_type CHECK (entry_type IN ('NOTE', 'WORKOUT')),
    CONSTRAINT chk_life_log_source CHECK (source_type IN ('MANUAL', 'AGENT', 'IMPORT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='生活日志条目';

CREATE TABLE workout_set (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    log_entry_id BIGINT UNSIGNED NOT NULL,
    sequence_no SMALLINT UNSIGNED NOT NULL COMMENT '本次训练中的顺序',
    exercise_name VARCHAR(80) NOT NULL,
    repetitions SMALLINT UNSIGNED NULL COMMENT '次数；计时训练可为空',
    weight_kg DECIMAL(7,2) NULL COMMENT '负重，千克；未提供时为空',
    duration_seconds INT UNSIGNED NULL COMMENT '持续秒数；次数训练可为空',
    PRIMARY KEY (id),
    UNIQUE KEY uk_workout_set_order (log_entry_id, sequence_no),
    CONSTRAINT fk_workout_set_log FOREIGN KEY (log_entry_id) REFERENCES life_log_entry (id) ON DELETE RESTRICT,
    CONSTRAINT chk_workout_set_repetitions CHECK (repetitions IS NULL OR repetitions > 0),
    CONSTRAINT chk_workout_set_weight CHECK (weight_kg IS NULL OR weight_kg >= 0),
    CONSTRAINT chk_workout_set_duration CHECK (duration_seconds IS NULL OR duration_seconds > 0),
    CONSTRAINT chk_workout_set_measure CHECK (repetitions IS NOT NULL OR duration_seconds IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='训练组次';
