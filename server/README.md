# 汐序服务端

基于 Java 17 + Spring Boot 4.1 的 REST API，为 Android 与 Web 两端提供统一的鉴权、业务与数据接口。数据使用 MySQL 8.4，通过 `JdbcTemplate` 访问。

## 运行

在本地启动（需先准备好 `xixu_dev` 数据库）：

```powershell
.\start-local.ps1
```

脚本会提示输入 MySQL 用户名与密码，也可预先设置 `$env:XIXU_DB_USER`、`$env:XIXU_DB_PASSWORD` 免交互启动。默认监听 `127.0.0.1:18080`。首次安装且用户表为空时才使用 `-CreateAdmin` 创建初始管理员。

更完整的步骤见 [docs/登录功能本地运行.md](../docs/登录功能本地运行.md)。

## 模块

| 包 | 说明 |
|---|---|
| `auth` | 登录、登出、身份查询；Bearer 令牌与会话 |
| `ledger` | 记账：新增、查询、汇总、作废 |
| `journal` | 日志与训练：新增、查询、组次 |
| `wellness` | 身体与训练：体重/体成分、睡眠 |
| `assistant` | 智能助理只读工具（月度快照） |

接口前缀：`/api/auth`、`/api/ledger`、`/api/logs`、`/api/wellness`、`/api/assistant`；健康检查 `/api/health` 免鉴权。

## 结构

| 路径 | 说明 |
|---|---|
| `src/main/java/wiki/yuhang/xixu/` | 业务代码（按模块分包） |
| `src/main/resources/application.properties` | 端口、数据源与引导管理员配置 |
| `src/test/java/` | 单元测试（Mockito，无数据库依赖） |
| `start-local.ps1` | 本地启动脚本 |
| `test-login-local.ps1`、`test-ledger-local.ps1` | 真实接口联调脚本 |
| `import-dsh-logs-local.ps1`、`tools/` | DSH 样本导入与辅助工具 |

## 构建与测试

```powershell
.\mvnw.cmd test
```

当前单元测试覆盖 auth / ledger / journal / wellness / assistant 五个模块。

## 约定

- 时间字段统一 UTC；日期字段为用户本地日期。
- 会话令牌只保存 SHA-256 摘要，不存原文。
- 业务查询按当前登录用户 ID 限定，不信任客户端传入的 `user_id`。
- 数据库结构以 [database/](../database/README.md) 为准；接口说明见 [docs/](../docs/)。
