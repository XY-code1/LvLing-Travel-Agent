# 旅灵 Production Readiness Audit

审计日期：2026-09-19。范围为当前 `E:\Guido-main` 代码与配置。本轮只审计和准备部署文档，不购买云服务、不部署、不写入 Secret、不修改 Agent/Harness、数字人、TTS 或 Geolocation/CityContext 业务。

## 结论摘要

当前项目具备“前端 H5 静态文件 + Spring Boot 可执行 JAR + MySQL + Nginx 同源反向代理”的基础形态，但尚未达到可直接交给评委访问的生产状态。

已验证：

- `tourist-app` 生产构建 PASS。
- 生产构建产物未发现 `localhost:5175`、`localhost:5176`、`localhost:8080` 或 `127.0.0.1`。
- 业务 API 使用 `/api/**` 相对路径，适合 Nginx 同源代理。
- Spring Boot 使用 JDK 17 成功打包 fat JAR。
- DB、AMap、JWT、AI 加密配置均通过环境变量占位符读取，没有写入代码。
- `.gitignore` 已忽略生产配置、环境变量、证书和构建产物。

未验证：当前没有可用生产 MySQL 凭据、云服务器、域名、DNS、TLS 证书、Nginx 或常驻进程，因此 `/api/health` 的生产 200 尚未验收。

## 1. 当前与目标架构

开发环境：

```text
浏览器 -> tourist-app Vite :5175
       /api/tourist/**、/files/** -> Vite proxy -> Spring Boot :8080
Spring Boot -> MySQL scenic_ai_guide :3306
            -> Redis（按需）
            -> AMap Web Service（服务端 Key）
```

生产环境：

```text
Internet -> HTTPS 域名 -> Nginx :443
                         ├─ /       -> H5 静态文件
                         ├─ /api/** -> Spring Boot :8080
                         └─ /files/** -> Spring Boot :8080
Spring Boot -> 云 MySQL / Redis / AMap
```

评委只访问 HTTPS 域名，不安装 Java、Maven、Node.js、MySQL 或 PowerShell。

## 2. tourist-app 审计

构建：

```bash
cd tourist-app
npm run build
```

输出：`tourist-app/unpackage/dist/build/h5`。本轮构建 PASS，可由 Nginx 直接托管。

`utils/request.ts` 默认 `VITE_API_BASE_URL` 为空，业务请求使用 `/api/tourist/**`；`VITE_DEV_PROXY_TARGET=http://localhost:8080` 只用于开发 Vite proxy。生产产物扫描未发现 localhost/127.0.0.1。

自动定位保留为：

```text
navigator.geolocation
 -> latitude/longitude
 -> /api/tourist/context/resolve-location
 -> Spring Boot -> AMap reverse geocode -> CityContext
```

失败顺序：已有城市缓存 → 杭州 Demo fallback → 用户手动切换。生产定位必须使用 HTTPS 安全上下文；HTTP 页面可能被浏览器拒绝定位权限。Nginx 应将 80 重定向到 443，并转发 `X-Forwarded-Proto: https`。

## 3. backend 审计

`backend/pom.xml` 使用 Spring Boot 3.2.5、Java release 17 和 `spring-boot-maven-plugin` repackage。本轮使用 JDK 17 与项目本地仓库成功执行：

```bash
mvn -Dmaven.repo.local=E:\Guido-main\.m2\repository -DskipTests clean package
```

产物：`backend/target/scenic-ai-backend-0.0.1-SNAPSHOT.jar`。可执行启动：

```bash
java -jar scenic-ai-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

生产 Secret 必须通过服务器 Secret 管理、Docker secrets 或受限 `EnvironmentFile` 注入，不能写入 Git、镜像或浏览器：

| 变量 | 用途 | 要求 |
|---|---|---|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | MySQL | 必填 |
| `AMAP_WEB_SERVICE_KEY` | 服务端地理编码、POI、天气、路线 | 必填，仅后端 |
| `JWT_SECRET` | 登录令牌 | 必填，建议 ≥32 字符 |
| `AI_CONFIG_AES_KEY` | AI Provider 凭据解密 | 必填，≥32 字符 |
| `CORS_ALLOWED_ORIGINS` | 正式 HTTPS 来源 | 同源部署填写正式域名 |
| `SERVER_PORT` | 服务端口 | 可选，默认 8080 |
| `REDIS_HOST/PORT/PASSWORD/DATABASE` | Redis | 按部署需要 |
| `AMAP_*_TTL` | 高德缓存 TTL | 可选 |

`application-prod.yml.example` 和 `application.yml` 均使用 `${...}` placeholder；未发现生产 Secret 硬编码。

## 4. 数据库审计

代码使用 MySQL Connector/J 和 MySQL 8 风格 JDBC 参数。生产建议 MySQL 8.0/8.4 LTS；当前机器版本未能通过可用连接确认，不能伪称已验证。

初始化文件：

- 基础表：`database/schema.sql`
- 基础 seed：`database/data.sql`
- 城市上下文迁移：`database/migration/phase3_city_context.sql`
- SOS 迁移：`database/migration/sos_request.sql`
- 数字人迁移：`database/avatar_canvas_migration.sql`、`database/avatar_selection_migration.sql`

生产顺序：创建独立数据库和最小权限应用用户 → 执行 schema → data → 按目标版本执行 migration → 检查 `city/scenic/spot/route/route_spot/avatar_config/admin_feature_item/ai_service_config/sos_request` → 启动后端并验证 `/api/health`。

不要让应用长期使用 MySQL root；迁移前做数据库备份并记录版本。

## 5. Nginx 配置模板

以下是待部署模板，需替换域名和证书路径：

```nginx
server {
    listen 80;
    server_name travel.example.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name travel.example.com;
    ssl_certificate /etc/letsencrypt/live/travel.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/travel.example.com/privkey.pem;
    root /var/www/lvling;
    index index.html;

    location / { try_files $uri $uri/ /index.html; }
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_read_timeout 120s;
    }
    location /files/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto https;
    }
}
```

`try_files ... /index.html` 保证 SPA history 路由刷新不 404。Nginx 中的 `127.0.0.1:8080` 是服务器内部反代地址，不会暴露给浏览器。

## 6. 服务管理

推荐 Docker Compose：Spring Boot、MySQL、Nginx（Redis 按需）服务边界清晰，`restart: unless-stopped` 覆盖服务器重启和异常退出，Secret 可用外部注入。当前仓库尚无生产 Dockerfile/Compose，需要下一阶段新增。

备选 JAR + systemd：专用 `lvling` 用户运行 `/opt/lvling/backend/*.jar`，使用 `/etc/lvling/lvling.env`（0600），systemd 设置 `Restart=on-failure`、`RestartSec=5` 和 `After=network-online.target`。当前也尚无现成 unit 文件。

## 7. 当前 blocker

1. 无云服务器、域名、DNS 和 TLS 证书。
2. 无生产 MySQL 实例、最小权限账号和迁移执行记录。
3. 无生产 Secret 注入配置。
4. 无 Dockerfile/Compose 或 systemd unit。
5. 本地 DB 凭据缺失，`/api/health` 尚未完成真实 200 验收。
6. Redis 是否启用、生产 AI Provider/TTS 是否已写入 `ai_service_config` 尚待确认。
7. 尚未进行公网健康检查、定位、POI、路线、服务和 Agent 联调。

## 8. 后续部署步骤（本轮不执行）

1. 选择云主机/容器平台和 MySQL 8.0/8.4 LTS；防火墙只开放 80/443。
2. 构建 H5，上传 `unpackage/dist/build/h5` 到 Nginx 根目录。
3. 使用 JDK 17 打包并制作后端镜像或上传 fat JAR。
4. 初始化 MySQL：schema → data → migration，创建最小权限账号。
5. 注入 DB、AMap、JWT、AI 加密和 CORS Secret。
6. 配置 Compose 或 systemd 自动启动/恢复。
7. 配置 Nginx、SPA fallback、HTTP→HTTPS 和证书。
8. 验收 `GET https://<domain>/api/health` = 200 且 database=UP。
9. 验收 `/api/tourist/cities`、`/api/tourist/context/resolve-location`、景点、服务、路线、登录、Agent。
10. 验收定位允许/拒绝/超时、任意前端路由刷新、服务器重启恢复，并检查日志不含密码/Key/Token。

## 最终回答

### A. 当前代码是否已经可以部署？

可以进入部署实施阶段，但不能宣称已生产就绪：前端构建和后端 JAR 已通过，生产基础设施和真实接口尚未验收。

### B. 还缺什么？

云服务器/容器平台、MySQL、DNS、HTTPS、Secret 注入、Nginx、Compose 或 systemd、数据库初始化和公网联调。

### C. 推荐云服务器规格

比赛 Demo 起步：2 vCPU、4 GB RAM、40–80 GB SSD、Ubuntu 22.04/24.04 LTS、5 Mbps 以上带宽。若启用本地向量/RAG、较重 Agent 并发或 TTS，建议 4 vCPU/8 GB RAM，并将 MySQL/Redis 拆为托管服务。

### D. 推荐 Docker Compose 还是 JAR + systemd？

优先 Docker Compose；若团队只维护标准 Linux VM 且不采用容器，选择 JAR + systemd。

### E. 到“评委只打开 URL”还需哪些步骤？

云资源 → MySQL 初始化 → Secret 注入 → 后端常驻/健康检查 → H5 上传 → Nginx 反代 → DNS/HTTPS → 定位和核心 API 公网联调 → 重启/故障恢复演练。完成后评委只需打开 HTTPS URL。
