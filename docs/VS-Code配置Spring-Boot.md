# 在 VS Code 中打开汐序服务端

本机检查于 2026-10-31：JDK 17、VS Code、Java 扩展包、Spring Boot Tools、Spring Boot Dashboard、Spring Initializr、Maven for Java 均已安装。`server/mvnw.cmd -v` 识别 Maven 3.10.0 和 JDK 17；`mvnw.cmd -DskipTests package` 构建成功。**目前服务端只有工程骨架，登录接口和数据库连接配置还在开发中，暂不要用能否启动服务判断 VS Code 配置是否成功。**

## 操作步骤

1. 在 VS Code 选择「文件 → 打开文件夹」，打开 `E:\graduation-project\server`。若弹出工作区信任提示，确认这是本机自己创建的项目后选择信任。
2. 等右下角 Java 导入进度结束。左侧资源管理器应看到 `pom.xml`、`src`、`mvnw.cmd`；侧栏可展开 **JAVA PROJECTS**、**MAVEN**、**SPRING BOOT DASHBOARD**。
3. 打开 `src/main/java/wiki/yuhang/xixu/XixuServerApplication.java`，类名上方应出现 Run / Debug。打开 `pom.xml` 时，Maven 依赖应能解析，不出现整页红色报错。
4. 在 VS Code 里按 `Ctrl+反引号` 打开终端，确认路径是 `E:\graduation-project\server`，执行：

   ```powershell
   .\mvnw.cmd -v
   .\mvnw.cmd -DskipTests package
   ```

   看到 `BUILD SUCCESS` 即表示 JDK、Maven Wrapper 和工程导入的基础链路正常。第一次构建会下载依赖，后续会快很多。

## 常见情况

- 看不到 **JAVA PROJECTS**：按 `Ctrl+Shift+P`，执行 `Java: Import Java projects in workspace`，然后等待导入。
- Java 文件持续显示红色但命令行构建成功：按 `Ctrl+Shift+P`，执行 `Java: Clean Java Language Server Workspace`，确认重启后再等导入完成。
- `mvn` 命令找不到：本项目使用 `mvnw.cmd`，无需全局安装 Maven。
- 当前直接点击 Run 若因数据源缺少配置而启动失败，属于服务端尚未接入数据库，不代表 VS Code 安装失败。登录功能完成后会提供本地运行配置。
