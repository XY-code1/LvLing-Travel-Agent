# 旅灵生产部署

目标拓扑：浏览器通过同一个 HTTPS 域名访问 H5；Nginx 提供静态文件并把 `/api`、`/files` 转发到 Spring Boot。高德 Web Service、数据库、LLM 与 TTS 凭据只存在于服务端。

## 1. 构建

### tourist-app H5

HBuilderX 安装在默认项目约定位置时：

```powershell
cd E:\Guido-main\tourist-app
npm run build
```

输出目录：`tourist-app/unpackage/dist/build/h5`。

生产推荐同源 `/api`，因此 `VITE_API_BASE_URL` 留空。若前后端必须使用不同域名，在构建前设置 `VITE_API_BASE_URL=https://api.example.com`。前端高德 JS API 使用 `VITE_AMAP_JS_KEY` 与 `VITE_AMAP_SECURITY_CODE`；应在高德控制台限制允许的生产域名。

### Spring Boot

```powershell
$env:JAVA_HOME="D:\Java\jdk-17.0.16"
cd E:\Guido-main\backend
mvn -Dmaven.repo.local=E:\Guido-main.m2\repository clean package
```

产物位于 `backend/target/scenic-ai-backend-0.0.1-SNAPSHOT.jar`。

## 2. 生产环境变量

必须在部署平台的 Secret/Environment 设置中注入，不写入 Git、前端或镜像：

| 变量 | 用途 | 必填 |
| --- | --- | --- |
| `DB_URL` | MySQL JDBC URL | 是 |
| `DB_USERNAME` | MySQL 用户 | 是 |
| `DB_PASSWORD` | MySQL 密码 | 是 |
| `AMAP_WEB_SERVICE_KEY` | 服务端地理编码、POI、天气、路线 | 是 |
| `AI_CONFIG_AES_KEY` | 解密数据库中的 AI Provider 凭据，至少 32 位 | 是 |
| `JWT_SECRET` | 登录令牌密钥，至少 32 位 | 是 |
| `CORS_ALLOWED_ORIGINS` | 允许跨域的域名；同源部署也建议填正式域名 | 是 |
| `SERVER_PORT` | Spring Boot 端口，默认 8080 | 否 |
| `REDIS_HOST/PORT/PASSWORD/DATABASE` | Redis | 按部署环境 |

LLM、RAG、阿里云 TTS 的 Provider 凭据沿用 `ai_service_config` 数据模型，通过管理后台保存为加密配置；不得放入前端环境变量。若部署平台不保留数据库，需先导入 `database/schema.sql` 及必要 migration。

可选缓存 TTL：`AMAP_GEOCODE_CACHE_TTL`（默认 24h）、`AMAP_POI_CACHE_TTL`（30m）、`AMAP_WEATHER_CACHE_TTL`（10m）、`AMAP_ROUTE_CACHE_TTL`（5m）。

## 3. 启动后端

将 `application-prod.yml.example` 复制为部署机外部配置文件 `application-prod.yml`，不要填写凭据；凭据继续由环境变量提供：

```bash
java -jar scenic-ai-backend-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --spring.config.additional-location=file:/etc/lvling/
```

先验证 `GET http://127.0.0.1:8080/api/health`。正常响应包含 `backend=UP`、`database=UP`。高德临时异常不会让此健康检查判定后端死亡。

## 4. Nginx 同源反向代理

```nginx
server {
    listen 80;
    server_name travel.example.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name travel.example.com;

    ssl_certificate     /etc/letsencrypt/live/travel.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/travel.example.com/privkey.pem;

    root /var/www/lvling;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

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

使用云平台托管时采用同等规则：静态站点根目录指向 H5 构建产物，`/api/**` 和 `/files/**` 转发到后端。TLS 证书可由平台托管或 Let's Encrypt 提供。

## 5. 自动定位与调用治理

首次访问时浏览器请求定位权限；得到 WGS84 坐标后只把坐标发送到同源 `/api/tourist/context/resolve-location`，后端调用高德逆地理编码并返回 CityContext。成功结果保存在浏览器 24 小时；手动选择城市优先，不会被刷新后的自动定位覆盖。拒绝、超时或浏览器不支持时页面保留手动搜索入口，不崩溃。

高德服务端缓存按“接口 + 已排序参数”去重。相同地理编码、POI、天气、路线请求在 TTL 内不再次消耗高德额度。`DAILY_QUERY_OVER_LIMIT` 会返回明确错误，不会更换 Key 或伪造数据。

## 6. 上线验证

1. `https://travel.example.com/` 能打开并刷新任意 UniApp 页面。
2. `https://travel.example.com/api/health` 返回 backend/database 均为 UP。
3. 首次允许定位后 Header 城市、景点和服务同步变化；拒绝定位后可以手动搜索。
4. 城市搜索、景点 POI、服务 POI、天气、路线各调用一次，随后刷新验证缓存命中。
5. 浏览器 Network 中业务请求只访问当前 HTTPS 域名，无 Mixed Content、CORS 或 localhost 请求。
6. 检查服务日志不包含 Key、密码、Token；检查仓库不含 `.env.local`、`application-prod.yml` 与证书私钥。
