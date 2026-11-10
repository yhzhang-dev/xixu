-- 汐序：为 ledger_entry 增加导入来源标识，用于重复导入时去重。
-- 在已选中 xixu_dev 且已存在 ledger_entry 表时执行一次；手动录入的行 source_ref 为 NULL，不受影响。
ALTER TABLE ledger_entry
    ADD COLUMN source_ref VARCHAR(100) NULL DEFAULT NULL COMMENT '导入来源定位；手动记录留空' AFTER source_type,
    ADD UNIQUE KEY uk_ledger_user_source (user_id, source_ref);
