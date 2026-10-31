-- 仅用于已按旧版 03 脚本建好的 xixu_dev.auth_session；执行一次，不要重复执行。
-- 先停止旧后端，执行本脚本，再启动新版后端。
-- 为消除历史时间口径不一致的风险，全部旧会话将注销；Web 和手机需重新登录。
-- 不修改历史 created_at / expires_at：无法可靠判断哪些旧值是本地时间。

USE xixu_dev;

ALTER TABLE auth_session
    ADD COLUMN last_used_at DATETIME(3) NULL COMMENT 'UTC；鉴权成功后最多每 5 分钟更新一次' AFTER revoked_at,
    ADD COLUMN user_agent VARCHAR(512) NULL COMMENT '登录时浏览器或 App 的 User-Agent' AFTER last_used_at,
    ADD COLUMN ip_address VARCHAR(45) NULL COMMENT '直接连接后端的客户端 IP' AFTER user_agent,
    ADD KEY idx_auth_session_expires_at (expires_at),
    ADD KEY idx_auth_session_revoked_at (revoked_at);

UPDATE auth_session
SET revoked_at = UTC_TIMESTAMP(3)
WHERE revoked_at IS NULL;

SELECT COUNT(*) AS active_sessions_after_migration
FROM auth_session
WHERE revoked_at IS NULL AND expires_at > UTC_TIMESTAMP(3);
