-- 汐序：第一阶段数据库结构。
-- 先在 MySQL 中创建并选中数据库，再执行本文件。

CREATE TABLE app_user (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
    username VARCHAR(50) NOT NULL COMMENT '登录名',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
    display_name VARCHAR(50) NOT NULL COMMENT '展示名',
    role_code VARCHAR(16) NOT NULL DEFAULT 'USER' COMMENT 'ADMIN 或 USER',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0 禁用，1 正常',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    last_login_at DATETIME(3) NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_user_username (username),
    CONSTRAINT chk_app_user_role CHECK (role_code IN ('ADMIN', 'USER')),
    CONSTRAINT chk_app_user_status CHECK (status IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统用户';
