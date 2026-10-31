-- 在已选中的 xixu_dev 数据库执行一次；access token 原文不会入库。
CREATE TABLE auth_session (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    token_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    created_at DATETIME(3) NOT NULL COMMENT 'UTC；由数据库 UTC_TIMESTAMP(3) 写入',
    expires_at DATETIME(3) NOT NULL COMMENT 'UTC；创建后 24 小时',
    revoked_at DATETIME(3) NULL COMMENT 'UTC；注销或超出会话上限时填写',
    last_used_at DATETIME(3) NULL COMMENT 'UTC；鉴权成功后最多每 5 分钟更新一次',
    user_agent VARCHAR(512) NULL COMMENT '登录时浏览器或 App 的 User-Agent',
    ip_address VARCHAR(45) NULL COMMENT '直接连接后端的客户端 IP',
    PRIMARY KEY (id),
    UNIQUE KEY uk_auth_session_token_hash (token_hash),
    KEY idx_auth_session_user_id (user_id),
    KEY idx_auth_session_expires_at (expires_at),
    KEY idx_auth_session_revoked_at (revoked_at),
    CONSTRAINT fk_auth_session_user FOREIGN KEY (user_id)
        REFERENCES app_user (id) ON DELETE RESTRICT,
    CONSTRAINT chk_auth_session_expiry CHECK (expires_at > created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='登录会话';
