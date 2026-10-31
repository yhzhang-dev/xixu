-- 仅用于已经按旧版 05 脚本建好 life_log_entry 的本地开发库，执行一次。
-- 新建库直接运行最新版 05 脚本，不需要本迁移。
-- 保留原始类别、时间文字、备注和导入来源，已有记录使用默认类别「随记」。

ALTER TABLE life_log_entry
    ADD COLUMN category_name VARCHAR(30) NOT NULL DEFAULT '随记' AFTER entry_type,
    ADD COLUMN time_label VARCHAR(50) NULL AFTER occurred_on,
    ADD COLUMN remark VARCHAR(2000) NULL AFTER content,
    ADD COLUMN source_ref VARCHAR(150) NULL AFTER source_type,
    ADD UNIQUE KEY uk_life_log_user_source (user_id, source_ref);

ALTER TABLE life_log_entry DROP CHECK chk_life_log_source;
ALTER TABLE life_log_entry
    ADD CONSTRAINT chk_life_log_source
    CHECK (source_type IN ('MANUAL', 'AGENT', 'IMPORT'));
