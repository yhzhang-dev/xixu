-- 汐序：记账表 ledger_entry（一笔支出或收入）。
-- 在已选中的 xixu_dev 数据库执行一次；独立脚本，不重复执行 schema.sql。
-- 前置：app_user 表已存在（由 schema.sql 创建）。

CREATE TABLE ledger_entry (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '记账记录 ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '所属用户，关联 app_user.id',
    entry_type VARCHAR(8) NOT NULL COMMENT 'EXPENSE 支出 / INCOME 收入',
    amount DECIMAL(12,2) NOT NULL COMMENT '金额，恒为正数，人民币',
    occurred_on DATE NOT NULL COMMENT '实际发生日期',
    occurred_time TIME NULL DEFAULT NULL COMMENT '实际发生时间，未知留空',
    category_name VARCHAR(50) NOT NULL COMMENT '分类：吃饭、交通等',
    channel_name VARCHAR(30) NULL DEFAULT NULL COMMENT '渠道：微信、支付宝、现金等',
    merchant VARCHAR(100) NULL DEFAULT NULL COMMENT '商家或收款方',
    note VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
    source_type VARCHAR(20) NULL DEFAULT NULL COMMENT '来源：手动、自述、截图、账单等',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1 有效，0 作废',
    void_reason VARCHAR(200) NULL DEFAULT NULL COMMENT '作废原因',
    voided_at DATETIME(3) NULL DEFAULT NULL COMMENT '作废时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '录入时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后修改时间',
    PRIMARY KEY (id),
    KEY idx_ledger_entry_user_occurred (user_id, occurred_on),
    CONSTRAINT fk_ledger_entry_user FOREIGN KEY (user_id)
        REFERENCES app_user (id) ON DELETE RESTRICT,
    CONSTRAINT chk_ledger_entry_type CHECK (entry_type IN ('EXPENSE', 'INCOME')),
    CONSTRAINT chk_ledger_entry_amount CHECK (amount > 0),
    CONSTRAINT chk_ledger_entry_status CHECK (status IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='记账记录';
