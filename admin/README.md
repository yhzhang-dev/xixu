# 汐序 Web 端

Vue 3 + Vite。当前已实现与 Android 端共用账号的登录、身份查询、退出登录及工作台首页；四个生活模块与账号管理目前只展示清晰的待接入状态，**没有伪造业务数据**。

## 本地运行

先按 [`server/start-local.ps1`](../server/start-local.ps1) 启动 Spring Boot，再在本目录运行：

```powershell
npm install
npm run dev
```

打开 <http://127.0.0.1:5173/>，用手机端同一账号登录。2026-10-31 用户已验证进入工作台并看到用户名。Vite 将 `/api` 转发至 `http://127.0.0.1:18080`，浏览器无需直接访问 MySQL。账号令牌暂存在当前浏览器标签的 `sessionStorage`；关闭标签后需要重新登录。

验证生产构建：

```powershell
npm run build
```

产物在 `dist/`。部署到云端时，Web 静态站点和 `/api` 应由同一 HTTPS 域名提供，由反向代理将 `/api` 转发给 Spring Boot；目前尚未部署。
