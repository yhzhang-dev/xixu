-- 汐序身体与训练模块第一版。用于已存在 app_user、life_log_entry 的 xixu_dev。
-- 只新建表并分类原始日志；不删除、覆盖或自动转换原始日志/旧 workout_set。
-- 可重复执行：新表使用 IF NOT EXISTS，类别种子和分类只补缺失行。
-- 精确时刻存 UTC；日期字段保留用户实际记录的当地日期。未知值留 NULL。

CREATE TABLE IF NOT EXISTS life_log_category (
    id SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    domain_code VARCHAR(12) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    name VARCHAR(40) NOT NULL,
    sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_life_log_category_code (code),
    KEY idx_life_log_category_domain (domain_code, sort_order),
    CONSTRAINT chk_life_log_category_domain CHECK (domain_code IN ('HEALTH', 'JOURNAL', 'LEDGER', 'SCHEDULE', 'OTHER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='原始生活记录受控主题';

CREATE TABLE IF NOT EXISTS life_log_classification (
    log_entry_id BIGINT UNSIGNED NOT NULL,
    category_id SMALLINT UNSIGNED NOT NULL,
    method VARCHAR(10) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    review_status VARCHAR(10) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    classified_at_utc DATETIME(3) NOT NULL,
    reviewed_at_utc DATETIME(3) NULL,
    PRIMARY KEY (log_entry_id),
    KEY idx_life_log_class_category (category_id, review_status),
    CONSTRAINT fk_life_log_class_entry FOREIGN KEY (log_entry_id) REFERENCES life_log_entry (id) ON DELETE RESTRICT,
    CONSTRAINT fk_life_log_class_category FOREIGN KEY (category_id) REFERENCES life_log_category (id) ON DELETE RESTRICT,
    CONSTRAINT chk_life_log_class_method CHECK (method IN ('RULE', 'MANUAL', 'AGENT')),
    CONSTRAINT chk_life_log_class_review CHECK (review_status IN ('PENDING', 'CONFIRMED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='原始记录当前主主题及审核状态';

CREATE TABLE IF NOT EXISTS health_body_measurement (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    observed_on DATE NOT NULL,
    observed_at_utc DATETIME(3) NULL,
    height_cm DECIMAL(5,2) NULL,
    weight_kg DECIMAL(6,2) NULL,
    body_fat_pct DECIMAL(5,2) NULL,
    muscle_mass_kg DECIMAL(6,2) NULL,
    source_type VARCHAR(10) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    source_ref VARCHAR(180) NULL,
    source_log_id BIGINT UNSIGNED NULL,
    created_at_utc DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_body_user_date (user_id, observed_on, id),
    KEY idx_body_source_log (source_log_id),
    UNIQUE KEY uk_body_user_source (user_id, source_type, source_ref),
    CONSTRAINT fk_body_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE RESTRICT,
    CONSTRAINT fk_body_source_log FOREIGN KEY (source_log_id) REFERENCES life_log_entry (id) ON DELETE RESTRICT,
    CONSTRAINT chk_body_source CHECK (source_type IN ('MANUAL', 'DEVICE', 'IMPORT')),
    CONSTRAINT chk_body_weight CHECK (weight_kg IS NULL OR weight_kg > 0),
    CONSTRAINT chk_body_height CHECK (height_cm IS NULL OR height_cm > 0),
    CONSTRAINT chk_body_fat CHECK (body_fat_pct IS NULL OR body_fat_pct BETWEEN 0 AND 100),
    CONSTRAINT chk_body_muscle CHECK (muscle_mass_kg IS NULL OR muscle_mass_kg >= 0),
    CONSTRAINT chk_body_has_measure CHECK (weight_kg IS NOT NULL OR body_fat_pct IS NOT NULL OR muscle_mass_kg IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='一次体重或体成分测量';

CREATE TABLE IF NOT EXISTS health_body_metric (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    measurement_id BIGINT UNSIGNED NOT NULL,
    metric_code VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    value_decimal DECIMAL(12,4) NOT NULL,
    unit_code VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_body_metric_measurement_code (measurement_id, metric_code),
    CONSTRAINT fk_body_metric_measurement FOREIGN KEY (measurement_id) REFERENCES health_body_measurement (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='体成分设备附加数值；代码和单位由应用白名单校验';

CREATE TABLE IF NOT EXISTS health_sleep_session (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    sleep_on DATE NOT NULL COMMENT '醒来日期',
    session_kind VARCHAR(10) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'NIGHT',
    fell_asleep_at_utc DATETIME(3) NULL,
    woke_at_utc DATETIME(3) NULL,
    duration_minutes SMALLINT UNSIGNED NULL,
    deep_minutes SMALLINT UNSIGNED NULL,
    light_minutes SMALLINT UNSIGNED NULL,
    rem_minutes SMALLINT UNSIGNED NULL,
    awake_minutes SMALLINT UNSIGNED NULL,
    efficiency_pct DECIMAL(5,2) NULL,
    provider_score SMALLINT UNSIGNED NULL COMMENT '来源设备给出的原始评分，非医学结论',
    avg_sleep_heart_rate_bpm SMALLINT UNSIGNED NULL,
    avg_hrv_ms DECIMAL(7,2) NULL,
    source_type VARCHAR(10) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    source_ref VARCHAR(180) NULL,
    source_log_id BIGINT UNSIGNED NULL,
    created_at_utc DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_sleep_user_date (user_id, sleep_on, id),
    KEY idx_sleep_source_log (source_log_id),
    UNIQUE KEY uk_sleep_user_source (user_id, source_type, source_ref),
    CONSTRAINT fk_sleep_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE RESTRICT,
    CONSTRAINT fk_sleep_source_log FOREIGN KEY (source_log_id) REFERENCES life_log_entry (id) ON DELETE RESTRICT,
    CONSTRAINT chk_sleep_kind CHECK (session_kind IN ('NIGHT', 'NAP')),
    CONSTRAINT chk_sleep_source CHECK (source_type IN ('MANUAL', 'DEVICE', 'IMPORT')),
    CONSTRAINT chk_sleep_duration CHECK (duration_minutes IS NULL OR duration_minutes > 0),
    CONSTRAINT chk_sleep_efficiency CHECK (efficiency_pct IS NULL OR efficiency_pct BETWEEN 0 AND 100),
    CONSTRAINT chk_sleep_time CHECK (fell_asleep_at_utc IS NULL OR woke_at_utc IS NULL OR woke_at_utc > fell_asleep_at_utc)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='一段睡眠及汇总指标';

CREATE TABLE IF NOT EXISTS health_sleep_stage (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    session_id BIGINT UNSIGNED NOT NULL,
    sequence_no SMALLINT UNSIGNED NOT NULL,
    stage_code VARCHAR(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    started_at_utc DATETIME(3) NOT NULL,
    ended_at_utc DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sleep_stage_order (session_id, sequence_no),
    CONSTRAINT fk_sleep_stage_session FOREIGN KEY (session_id) REFERENCES health_sleep_session (id) ON DELETE RESTRICT,
    CONSTRAINT chk_sleep_stage_code CHECK (stage_code IN ('AWAKE', 'LIGHT', 'DEEP', 'REM')),
    CONSTRAINT chk_sleep_stage_time CHECK (ended_at_utc > started_at_utc)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='有真实设备时间线时存储的睡眠阶段';

CREATE TABLE IF NOT EXISTS health_workout_session (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    occurred_on DATE NOT NULL,
    started_at_utc DATETIME(3) NULL,
    ended_at_utc DATETIME(3) NULL,
    activity_name VARCHAR(80) NOT NULL,
    duration_seconds INT UNSIGNED NULL,
    active_kcal DECIMAL(8,2) NULL,
    total_kcal DECIMAL(8,2) NULL,
    avg_heart_rate_bpm SMALLINT UNSIGNED NULL,
    max_heart_rate_bpm SMALLINT UNSIGNED NULL,
    note VARCHAR(2000) NULL,
    source_type VARCHAR(10) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    source_ref VARCHAR(180) NULL,
    source_log_id BIGINT UNSIGNED NULL,
    created_at_utc DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_workout_user_date (user_id, occurred_on, id),
    KEY idx_workout_source_log (source_log_id),
    UNIQUE KEY uk_workout_user_source (user_id, source_type, source_ref),
    CONSTRAINT fk_workout_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE RESTRICT,
    CONSTRAINT fk_workout_source_log FOREIGN KEY (source_log_id) REFERENCES life_log_entry (id) ON DELETE RESTRICT,
    CONSTRAINT chk_workout_source CHECK (source_type IN ('MANUAL', 'DEVICE', 'IMPORT')),
    CONSTRAINT chk_workout_duration CHECK (duration_seconds IS NULL OR duration_seconds > 0),
    CONSTRAINT chk_workout_active_kcal CHECK (active_kcal IS NULL OR active_kcal >= 0),
    CONSTRAINT chk_workout_total_kcal CHECK (total_kcal IS NULL OR total_kcal >= 0),
    CONSTRAINT chk_workout_time CHECK (started_at_utc IS NULL OR ended_at_utc IS NULL OR ended_at_utc > started_at_utc)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='一次训练及设备汇总';

CREATE TABLE IF NOT EXISTS health_workout_set (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    session_id BIGINT UNSIGNED NOT NULL,
    sequence_no SMALLINT UNSIGNED NOT NULL,
    exercise_name VARCHAR(80) NOT NULL,
    repetitions SMALLINT UNSIGNED NULL,
    weight_kg DECIMAL(7,2) NULL,
    duration_seconds INT UNSIGNED NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_health_workout_set_order (session_id, sequence_no),
    CONSTRAINT fk_health_workout_set_session FOREIGN KEY (session_id) REFERENCES health_workout_session (id) ON DELETE RESTRICT,
    CONSTRAINT chk_health_workout_set_reps CHECK (repetitions IS NULL OR repetitions > 0),
    CONSTRAINT chk_health_workout_set_weight CHECK (weight_kg IS NULL OR weight_kg >= 0),
    CONSTRAINT chk_health_workout_set_duration CHECK (duration_seconds IS NULL OR duration_seconds > 0),
    CONSTRAINT chk_health_workout_set_measure CHECK (repetitions IS NOT NULL OR duration_seconds IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='结构化训练组次';

CREATE TABLE IF NOT EXISTS health_workout_metric (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    session_id BIGINT UNSIGNED NOT NULL,
    metric_code VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    value_decimal DECIMAL(12,4) NOT NULL,
    unit_code VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_workout_metric_session_code (session_id, metric_code),
    CONSTRAINT fk_workout_metric_session FOREIGN KEY (session_id) REFERENCES health_workout_session (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='可选：设备提供的训练区间、恢复和效果指标';

CREATE TABLE IF NOT EXISTS health_workout_heart_rate_sample (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    session_id BIGINT UNSIGNED NOT NULL,
    offset_seconds INT UNSIGNED NOT NULL COMMENT '从训练开始的秒数',
    heart_rate_bpm SMALLINT UNSIGNED NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_workout_hr_offset (session_id, offset_seconds),
    CONSTRAINT fk_workout_hr_session FOREIGN KEY (session_id) REFERENCES health_workout_session (id) ON DELETE RESTRICT,
    CONSTRAINT chk_workout_hr_positive CHECK (heart_rate_bpm > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='可选：设备提供的训练逐点心率';

INSERT INTO life_log_category (code, domain_code, name, sort_order) VALUES
('HEALTH_SLEEP', 'HEALTH', '睡眠', 10),
('HEALTH_ACTIVITY', 'HEALTH', '运动与训练', 20),
('HEALTH_NUTRITION', 'HEALTH', '饮食', 30),
('HEALTH_BODY', 'HEALTH', '身体状态', 40),
('HEALTH_VITAL', 'HEALTH', '体征', 50),
('HEALTH_SUPPLEMENT', 'HEALTH', '补剂', 60),
('HEALTH_HYDRATION', 'HEALTH', '饮水', 70),
('HEALTH_ENERGY', 'HEALTH', '精力', 80),
('JOURNAL_MOOD', 'JOURNAL', '情绪', 10),
('JOURNAL_REFLECTION', 'JOURNAL', '心得', 20),
('JOURNAL_STUDY', 'JOURNAL', '学习', 30),
('JOURNAL_LEISURE', 'JOURNAL', '娱乐', 40),
('JOURNAL_HABIT', 'JOURNAL', '习惯', 50),
('JOURNAL_ENVIRONMENT', 'JOURNAL', '环境', 60),
('JOURNAL_MISC', 'JOURNAL', '随记', 90),
('SCHEDULE_NOTE', 'SCHEDULE', '日程原文', 10),
('LEDGER_SPENDING_RAW', 'LEDGER', '消费原文', 10),
('OTHER_UNSORTED', 'OTHER', '待分类', 99)
ON DUPLICATE KEY UPDATE id = id;

-- 只按已有类别/类型做主题映射；不从正文推断数值或训练完成情况。
-- 新导入行可重复运行此段补分类，已有人工确认不会被覆盖。
INSERT INTO life_log_classification
    (log_entry_id, category_id, method, review_status, classified_at_utc)
SELECT le.id, c.id, 'RULE', 'PENDING', UTC_TIMESTAMP(3)
FROM life_log_entry AS le
JOIN life_log_category AS c ON c.code = CASE
    WHEN le.entry_type = 'WORKOUT' THEN 'HEALTH_ACTIVITY'
    WHEN le.category_name = '睡眠' THEN 'HEALTH_SLEEP'
    WHEN le.category_name = '运动' THEN 'HEALTH_ACTIVITY'
    WHEN le.category_name = '饮食' THEN 'HEALTH_NUTRITION'
    WHEN le.category_name = '身体' THEN 'HEALTH_BODY'
    WHEN le.category_name = '体征' THEN 'HEALTH_VITAL'
    WHEN le.category_name = '补剂' THEN 'HEALTH_SUPPLEMENT'
    WHEN le.category_name = '饮水' THEN 'HEALTH_HYDRATION'
    WHEN le.category_name = '精力' THEN 'HEALTH_ENERGY'
    WHEN le.category_name = '情绪' THEN 'JOURNAL_MOOD'
    WHEN le.category_name = '心得' THEN 'JOURNAL_REFLECTION'
    WHEN le.category_name = '学习' THEN 'JOURNAL_STUDY'
    WHEN le.category_name = '娱乐' THEN 'JOURNAL_LEISURE'
    WHEN le.category_name = '习惯' THEN 'JOURNAL_HABIT'
    WHEN le.category_name = '环境' THEN 'JOURNAL_ENVIRONMENT'
    WHEN le.category_name = '日程' THEN 'SCHEDULE_NOTE'
    WHEN le.category_name = '消费' THEN 'LEDGER_SPENDING_RAW'
    WHEN le.category_name = '随记' THEN 'JOURNAL_MISC'
    ELSE 'OTHER_UNSORTED'
END
LEFT JOIN life_log_classification AS existing ON existing.log_entry_id = le.id
WHERE existing.log_entry_id IS NULL;

-- 只读核验：旧原文数量不变；分类数量应等于当前 life_log_entry 数量。
SELECT (SELECT COUNT(*) FROM life_log_entry) AS original_log_count,
       (SELECT COUNT(*) FROM life_log_classification) AS classified_count,
       (SELECT COUNT(*) FROM health_body_measurement) AS body_measurement_count,
       (SELECT COUNT(*) FROM health_sleep_session) AS sleep_session_count,
       (SELECT COUNT(*) FROM health_workout_session) AS workout_session_count;
