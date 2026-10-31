<p align="center">
  <img src="design/readme-banner.png" alt="汐序 Xixu" width="100%">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-orange" alt="Java 17">
  <img src="https://img.shields.io/badge/Spring_Boot-4.1-6DB33F" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Vue-3-42B883" alt="Vue 3">
  <img src="https://img.shields.io/badge/MySQL-8.4-4479A1" alt="MySQL 8.4">
  <img src="https://img.shields.io/badge/uni--app-Android-2B9939" alt="uni-app">
</p>

# 汐序 · 个人生活数据管理与智能助理系统

> 把分散在记账、日志、训练与身体状况里的日常记录，收拢到一个可以回看、统计并由智能助理辅助的空间。

汐序（Xixu）是一款面向个人的生活数据管理应用，由 Android 客户端、Web 管理端与统一服务端组成。两端共用同一套账号体系与业务数据，旨在把日常记录从分散的聊天与本地文件沉淀为结构化数据，并在此基础上提供查询、统计与智能助理能力。项目当前处于本科毕业设计开发阶段。

## 核心功能

| 模块 | 能力 | 状态 |
|---|---|---|
| 记账 | 收支录入、按日期查询、带原因作废、跨端查看 | 已联调 |
| 日志与训练 | 每日记录、结构化训练组次、按月回看、历史样本导入 | 开发中 |
| 身体与训练 | 体重与体成分、睡眠记录，预留训练会话结构 | 开发中 |
| 智能助理 | 受控工具生成草稿、跨模块只读统计 | 规划中 |

## 技术栈

| 层次 | 选型 |
|---|---|
| Android | uni-app（Vue 3）；若校方要求原生开发，可调整为 Kotlin |
| Web | Vue 3 + Vite |
| 服务端 | Java 17 + Spring Boot 4.1，对外提供 REST API |
| 数据库 | MySQL 8.4 |
| 认证 | Bearer Token，服务端保存会话摘要与有效期 |

## 系统架构

客户端与服务端分离：Android 与 Web 两端通过 HTTP 调用同一套 Spring Boot 接口，服务端统一完成鉴权、业务校验与数据持久化，数据集中存储于 MySQL。

```
Android (uni-app) ─┐
                   ├─→ Spring Boot REST API ─→ MySQL 8.4
Web (Vue 3 + Vite) ┘
```

## 项目结构

| 目录 | 说明 |
|---|---|
| `server/` | Spring Boot 服务端：登录鉴权、记账、日志与训练、身体记录、只读统计接口 |
| `admin/` | Vue 3 Web 管理端：工作台、记账、日志与训练、健康记录 |
| `android/` | uni-app Android 客户端：登录、记一笔、账本、日志与训练、健康记录 |
| `database/` | 数据字典、建表与迁移 SQL，见[数据库说明](database/README.md) |
| `docs/` | 选题、需求、开发环境与接口说明 |
| `design/` | 原型、图标、界面设计系统与横幅素材 |
| `thesis/` | 论文大纲、写作素材与初稿章节，见[论文工作区](thesis/README.md) |
| `deploy/` | 云端部署说明与配置（后续补充） |

## 快速开始

### 环境要求

- JDK 17（仓库自带 Maven Wrapper）
- MySQL 8.4（开发库默认 `xixu_dev`；Windows 下服务名通常为 `MySQL84`）
- Node.js（Web 端）
- HBuilderX（Android 端预览）

### 1. 启动服务端

```powershell
cd server
.\start-local.ps1
```

脚本会提示输入 MySQL 用户名与密码；也可先设置 `$env:XIXU_DB_USER`、`$env:XIXU_DB_PASSWORD` 免交互启动。已有管理员账号时不要加 `-CreateAdmin`。看到 `Started XixuServerApplication` 即表示启动成功（端口 18080）。

### 2. 启动 Web 端

```powershell
cd admin
npm install
npm run dev
```

浏览器打开 <http://127.0.0.1:5173/>，使用手机端同一账号登录。

### 3. Android 客户端

用 HBuilderX 打开 `android/` 运行到真机。USB 调试需先执行 `adb reverse tcp:18080 tcp:18080`，详见[本地运行说明](docs/登录功能本地运行.md)。

## 文档

| 文档 | 内容 |
|---|---|
| [选题意向](docs/选题意向.md) | 题目、范围与工作量估算 |
| [开发环境](docs/开发环境.md) | 本机与云端环境清单 |
| [登录功能本地运行](docs/登录功能本地运行.md) | 后端启动、端口与真机联调步骤 |
| [记账接口说明](docs/记账接口说明.md) | 记账 REST 接口与字段约束 |
| [日志与训练接口说明](docs/日志与训练接口说明.md) | 日志/训练数据结构与接口 |
| [记账模块审查](docs/记账模块审查.md) | 记账模块实现与审查记录 |
| [VS Code 配置 Spring Boot](docs/VS-Code配置Spring-Boot.md) | 后端开发环境配置 |

## 开发进度

- **已联调**：登录鉴权；记账的「手机新增 → Web 查看 → Web 作废」跨端流程。
- **开发中**：日志与训练、身体与训练。代码与页面已就绪，真实数据库联调与手动录入尚待完成。
- **规划中**：智能助理的草稿生成与受控查询；日程与附件模块。

## 参与与致谢

- 开发：[yhzhang-dev](https://github.com/yhzhang-dev)
- AI 编程助手：[DeepSea（小鲸）](https://github.com/deepsea-whale)

详见 [CONTRIBUTORS.md](CONTRIBUTORS.md)。

## 贡献者

<a href="https://github.com/yhzhang-dev"><img src="https://github.com/yhzhang-dev.png?size=96" width="56" height="56" alt="yhzhang-dev" title="yhzhang-dev"></a>
&nbsp;&nbsp;
<a href="https://github.com/deepsea-whale"><img src="https://github.com/deepsea-whale.png?size=96" width="56" height="56" alt="deepsea-whale" title="deepsea-whale"></a>

## 许可

本项目为 2027 届本科毕业设计作品，代码与文档版权归作者所有。