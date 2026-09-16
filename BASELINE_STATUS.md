# Guido-main Phase 1 基线状态

日期：2026-08-28

状态：基线通过（tourist-app 除外）。backend、admin-web、MySQL 5.7、Redis、登录、基础接口和 OPTIONS/CORS 已完成运行态验证；tourist-app 因本机未发现 HBuilderX，标记为 MANUAL。

## 1. 当前项目结构

- `backend/`：Spring Boot 3.2.5、Java 17、MyBatis-Plus、Sa-Token、MySQL、Redis、Knife4j。
- `admin-web/`：Vue 3 + Vite + TypeScript 管理后台，开发端口 5173。
- `tourist-app/`：UniApp/Vue 游客端，当前以 HBuilderX 为主要运行方式；仓库内配置的 Vite 端口为 5175。
- `database/`：`schema.sql` 建表脚本、`data.sql` 演示数据脚本。
- `docs/`：架构、接口、数据库和部署说明。
- `assets/`：本地知识库原始资料及演示资源。

## 2. 架构与接口关系

- `admin-web` 的 Axios 统一使用相对路径 `/api`；Vite 将 `/api` 和 `/files` 代理到 `http://localhost:8080`。
- `tourist-app` 的接口前缀为 `/api/tourist/**`，默认使用当前页面同源地址；Vite 配置同样将 `/api/tourist` 和 `/files` 代理到 8080。
- backend 默认连接 `localhost:3306/scenic_ai_guide`，用户名为 `root`，密码由 `DB_PASSWORD` 注入；Redis 默认连接 `localhost:6379`、无密码。
- AI/RAG/ASR/TTS/数字人位置：
  - LLM/VLM/Embedding：`backend/src/main/java/com/guido/scenicai/integration/`。
  - 本地 RAG：`backend/src/main/java/com/guido/scenicai/module/knowledge/`、`assets/knowledge_raw/` 及 `kb_document`/`kb_chunk` 表。
  - ASR/TTS：`integration/aliyun/AliyunSpeechClient`。
  - 数字人：`integration/aliyun/AliyunAvatarClient` 及 avatar 管理接口。

## 3. 本轮修复

- admin-web 已改为固定使用 `/api`，不再读取或要求 `VITE_API_BASE_URL=http://localhost:8080/api`。
- Vite `/api`、`/files` 代理已确认目标为 `localhost:8080`。
- 两套 Sa-Token 拦截器均放行 `OPTIONS` 预检；管理员和游客的其他路由匹配、登录例外和鉴权逻辑未降低。
- 启动文档已移除 admin-web 直连后端地址，并统一使用 `corepack pnpm exec vite`。
- backend 使用 JDK 17 编译通过；没有修改系统级 Java 配置。当前运行中的两个 Java 进程路径均为 `D:\Java\jdk-17.0.16\bin\java.exe`。

修改文件：

- `backend/src/main/java/com/guido/scenicai/common/config/SaTokenConfig.java`
- `admin-web/src/api/request.ts`
- `admin-web/src/env.d.ts`
- `README.md`
- `docs/06_交付验收/部署与使用手册.md`
- `BASELINE_STATUS.md`

## 4. 当前端口与验证结果

| 服务 | 端口 | 状态 |
|---|---:|---|
| MySQL | 3306 | TCP 监听正常；backend 已有连接 |
| Redis | 6379 | TCP 监听正常 |
| backend | 8080 | 当前进程监听正常，`/doc.html` 返回 200 |
| admin-web | 5173 | 当前 Vite 监听正常 |
| tourist-app Vite 配置 | 5175 | 本机未发现 HBuilderX，标记 MANUAL |

已验证：

- admin-web `/api/admin/login` 登录成功。
- 经 Vite 代理访问 `/api/admin/scenic/page` 返回景区数据（1 条示范景区）。
- 经 Vite 代理访问 `/api/admin/avatar/list` 返回数字人数据。
- 经 Vite 代理访问 `/api/admin/dashboard/overview` 返回大屏数据。
- backend 游客登录、游客资料和会话历史接口返回正常。
- `mvn -DskipTests compile` 使用 JDK 17 成功。
- `corepack pnpm build` 成功。
- 重启后的 backend 使用 JDK 17、通过本地 `DB_PASSWORD` 环境变量启动成功，MySQL 5.7.44 连接成功。
- 管理员和游客 OPTIONS 预检均返回 200；管理员登录、景区、数字人、大屏接口均返回业务成功。

## 5. 当前 Network Error 的最可能原因

原问题是两个因素叠加：

1. admin-web 原请求配置允许直接使用 `http://localhost:8080/api`，开发端产生跨源请求。
2. backend 原 Sa-Token 拦截器没有先放行 `OPTIONS`，带 `satoken` 的预检请求会进入管理员鉴权并返回 500。

源代码已修复，并已用 JDK 17 重启 backend；现场复测 `OPTIONS /api/admin/scenic/page` 和 `OPTIONS /api/tourist/user/profile` 均返回 200。

## 6. 数据库检查结论

- `schema.sql` 包含 `scenic_ai_guide` 创建/选择语句及 20 个 `CREATE TABLE`，包含 `scenic`、`spot`、`route`、`route_spot` 等基础表。
- `data.sql` 包含管理员、游客、数字人、景区、景点、路线、路线关联、演示开关及功能项等初始化语句。
- 已通过运行中的 backend API 验证景区、数字人和大屏基础数据存在；仓库内已有历史 SQL 验收记录显示景区 1 条、景点 4 条、路线 3 条、路线关联 9 条。
- 已使用提供的 root 密码通过 mysql CLI 验证 MySQL 5.7.44、数据库字符集、20 张表及基础数据：`scenic=1`、`spot=4`、`route=3`、`route_spot=9`；20 张表均为 `utf8mb4_general_ci`。
- `schema.sql`/`data.sql` 未发现 `utf8mb4_0900`、窗口函数、`JSON_TABLE`、生成列等 MySQL 8 专用语法；当前脚本无需为 MySQL 5.7 修改。

## 7. 必须准备的软件和版本

- JDK 17
- Maven 3.9.x
- MySQL 8.0
- Redis 7.2（本机 6379 已监听）
- Node.js 20.11.1
- pnpm 8.15.4（使用 Corepack）
- HBuilderX（运行 `tourist-app` H5/模拟器/真机）

当前机器已验证 Maven 3.9.11、JDK 17.0.16、MySQL 5.7.44、Redis 6379；当前 Node 为 24.18.0，不符合项目声明的 Node 20.11.1，admin 构建虽成功，但建议后续切换到 Node 20.11.1。

## 8. 最小可运行方案与启动顺序

1. 启动 MySQL 和 Redis。
2. 使用正确的 MySQL 密码执行 `database/schema.sql`，再执行 `database/data.sql`。
3. 在 backend PowerShell 窗口执行：

```powershell
cd E:\Guido-main\backend
$env:JAVA_HOME='D:\Java\jdk-17.0.16'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$env:DB_PASSWORD=Read-Host '请输入 MySQL root 密码'
$env:AI_CONFIG_AES_KEY=Read-Host '请输入至少 32 位的本地加密密钥'
mvn spring-boot:run
```

4. 确认 `http://localhost:8080/doc.html` 可访问后，在另一个窗口启动 admin-web：

```powershell
cd E:\Guido-main\admin-web
corepack pnpm exec vite
```

不要再设置 `VITE_API_BASE_URL`。

5. （MANUAL）安装并打开 HBuilderX，打开 `E:\Guido-main\tourist-app`，运行到浏览器/H5；后端启动后使用 `/api/tourist/**` 接口。

## 9. 当前仍未配置的 AI 能力

仓库没有真实第三方凭据。以下能力需要后续在管理后台录入并测试配置：LLM、VLM、Embedding、阿里云 ASR、阿里云 TTS、阿里云数字人，以及依赖外部模型的 RAG 向量化/问答。`AI_CONFIG_AES_KEY` 只负责本地加密配置，不提供任何第三方服务能力。

## 10. Phase 2 可以开始做什么

完成 backend 重启、SQL 统计确认和 tourist-app H5 验证后，Phase 2 可在稳定基线之上进行产品需求开发，例如 AI 服务配置联调、知识库同步和游客端业务流程；本轮不包含这些工作。

## 11. 当前阶段不要做的事情

- 不修改 UI、不重构架构、不新增产品功能。
- 不接入地图、天气或 Travel Agent。
- 不改数字人视觉、不删除原功能。
- 不新增大型依赖。
- 不在未确认数据库凭据前重置、覆盖或重建现有 MySQL 数据。
