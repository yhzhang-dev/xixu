# 汐序 Android 客户端

uni-app（Vue 3）实现的 Android 客户端，与 Web 端共用 Spring Boot 服务端与数据库。当前已接入登录、记账、日志与训练、身体与训练。

## 运行

用 HBuilderX 打开本目录，运行到 Android 真机或模拟器。

后端默认地址为 `http://127.0.0.1:18080`；USB 真机调试需先做反向端口映射：

```powershell
adb reverse tcp:18080 tcp:18080
```

后端启动方式见 [server/README.md](../server/README.md) 与 [docs/登录功能本地运行.md](../docs/登录功能本地运行.md)。

## 页面

| 页面 | 路径 | 说明 |
|---|---|---|
| 登录 | `pages/login/index` | 用户名/密码登录，令牌存本地 |
| 首页 | `pages/index/index` | 今日账目与日志概览、快捷入口 |
| 记账 | `pages/ledger/record`、`pages/ledger/list` | 记一笔、账本列表 |
| 日志与训练 | `pages/journal/record`、`pages/journal/list` | 写记录、按月回看 |
| 身体与训练 | `pages/health/index`、`pages/health/record` | 体重/体成分、睡眠记录 |

## 目录结构

| 路径 | 说明 |
|---|---|
| `pages.json` | 页面路由与全局样式配置 |
| `App.vue`、`main.js` | 应用入口 |
| `common/api.js` | 后端地址 `API_BASE_URL` 与令牌 key |
| `common/wellness.js` | 身体与训练接口封装 |
| `static/` | Logo 与多密度应用图标（`app-icons/v2`） |
| `manifest.json` | uni-app 应用配置 |

## 约定

- 接口地址集中在 `common/api.js`，正式部署前改为 HTTPS 域名。
- 所有请求携带 `Authorization: Bearer <token>`；401 时清除令牌并回登录页。
- 字段与接口以 [docs/记账接口说明.md](../docs/记账接口说明.md)、[docs/日志与训练接口说明.md](../docs/日志与训练接口说明.md) 为准。

## 备注

- 构建产物 `unpackage/` 与 `node_modules` 不入库（见 `.gitignore`）。
- 真机与 HBuilderX 的详细步骤参考 [docs/登录功能本地运行.md](../docs/登录功能本地运行.md)。
