# 数据库设计

汐序（Xixu）的数据库结构与演进记录。开发库默认 `xixu_dev`，使用 MySQL 8.4 与 `utf8mb4` 字符集；精确时刻统一以 UTC 存储，日期字段保留用户所在时区的本地日期。

## 数据库概览

- 数据库：`xixu_dev`（MySQL 8.4，`utf8mb4_0900_ai_ci`）。
- 结构来源：以本目录下的 SQL 与设计文档为准；若与实际库不一致，以实际库为准。
- 演进方式：新表使用独立建表脚本，已有表的结构变更使用带编号的迁移脚本，不重复执行 `schema.sql`。
- 时间约定：`DATETIME(3)` 字段统一为 UTC；`DATE` 字段为用户本地日期。
- 删除约定：业务数据以状态位软删除（如记账的 `status`），不物理删除。

## 表结构总览

| 表 | 说明 | 设计 / 脚本 | 状态 |
|---|---|---|---|
| `app_user` | 系统用户 | [01](01-用户表设计.md) / [schema.sql](schema.sql) | 已建表 |
| `ledger_entry` | 记账记录 | [02](02-记账表设计.md) / [04](04-记账表建表.sql) | 已建表，已联调 |
| `auth_session` | 登录会话 | [03](03-登录会话表.sql) / [07](07-登录会话增强迁移.sql) | 已建表，已使用 |
| `life_log_entry` | 原始生活记录 | [05](05-日志与训练记录.sql) | 已建表，已导入样本 |
| `workout_set` | 日志中的训练组次 | [05](05-日志与训练记录.sql) | 已建表 |
| 身体与训练、日志分类（10 张表） | 体重与体成分、睡眠、训练会话、原始记录主题分类 | [08 设计](08-身体健康与日志分类设计.md) / [08 SQL](08-身体健康与日志分类建表.sql) | 未执行 |

> 「已建表」来自用户报告与本地验证；本工作区未直接连接数据库核验全部结构。

## 设计文档

| 文档 | 内容 |
|---|---|
| [01-用户表设计](01-用户表设计.md) | 用户表字段、角色与登录规则 |
| [02-记账表设计](02-记账表设计.md) | 记账表字段、索引与约束 |
| [08-身体健康与日志分类设计](08-身体健康与日志分类设计.md) | 身体、训练、睡眠与原记录分类的目标结构与关系 |
| [建库建表步骤](建库建表步骤.md) | 本地建库、建表与核对步骤 |

## 建表与迁移

### 1. 建库

```sql
CREATE DATABASE xixu_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE xixu_dev;
```

### 2. 建表

在已选中 `xixu_dev` 的 MySQL 客户端中，按依赖顺序执行：

```sql
SOURCE E:/graduation-project/database/schema.sql;
SOURCE E:/graduation-project/database/03-登录会话表.sql;
SOURCE E:/graduation-project/database/04-记账表建表.sql;
SOURCE E:/graduation-project/database/05-日志与训练记录.sql;
```

### 3. 迁移

已有旧结构时执行对应迁移，不要重复执行建表脚本：

- [06-日志真实样本字段迁移](06-日志真实样本字段迁移.sql)：为已按旧版建表的日志库补充原始类别、时间、备注与来源字段。
- [07-登录会话增强迁移](07-登录会话增强迁移.sql)：为旧 `auth_session` 增加设备与最近使用字段；执行会注销全部旧会话，需重新登录。
- [08-身体健康与日志分类建表](08-身体健康与日志分类建表.sql)：新增身体、睡眠、训练与分类结构，尚未执行与接入。

## 核对

在已连接 `xixu_dev` 的 MySQL 客户端执行以下**只读**语句：

```sql
SHOW TABLES;
DESCRIBE ledger_entry;
SHOW CREATE TABLE ledger_entry\G
SELECT COUNT(*) FROM ledger_entry;
```

正常应看到 `ledger_entry`，含 `entry_type`、`amount`、`occurred_on`、`status`、`void_reason` 等字段。若表不存在，再按[建表脚本](04-记账表建表.sql)处理；不要在已建表时重复执行。遇错保留错误文字即可，不要发送密码。

## 说明

- 表数量随模块扩展而调整，不按固定数量凑表。
- 「184 条样本」来自用户报告，本工作区未直接查询核验。
- 建议产出顺序：`用例与数据对象.md` → `ER图.md` → `数据字典.md` → `schema.sql` → 最小演示数据与迁移校验。