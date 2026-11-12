# 生产部署（Docker Compose）

面向评审/演示的最小生产拓扑：MySQL + Spring Boot 后端 + Nginx（同源托管 H5 并反代 `/api`、`/files`）。

```text
Browser ──HTTPS──> Nginx ──/api、/files──> backend:8080 ──> MySQL
                        └─/──> tourist-app H5 静态文件
```

## 1. 准备

1. 构建游客端 H5（输出到 `tourist-app/unpackage/dist/build/h5`）：

   ```bash
   cd tourist-app
   npm run build
   ```

2. 准备 TLS 证书，放到 `deploy/certs/`：

   ```text
   deploy/certs/fullchain.pem
   deploy/certs/privkey.pem
   ```

3. 复制 `.env.example` 为 `.env`，填入真实值（`.env` 已被忽略，不要提交）：

   ```bash
   cp .env.example .env
   ```

   `DB_ROOT_PASSWORD` 仅用于初始化/运维；应用使用 `DB_USERNAME`/`DB_PASSWORD` 这个最小权限账号（compose 会自动创建并只授权 `scenic_ai_guide` 库）。

## 2. 启动

```bash
docker compose up -d --build
```

首次启动会执行 `database/schema.sql`、`data.sql` 和迁移脚本（按文件名顺序）。后端等待 MySQL 健康检查通过后启动。

## 3. 验证

```bash
curl -sk https://localhost/api/health         # 期望 backend=UP、database=UP
curl -sk https://localhost/api/tourist/cities
```

浏览器打开 `https://<域名>/`，检查：

- 任意前端路由刷新不 404；
- 业务请求只访问当前 HTTPS 域名，无 Mixed Content / CORS / localhost 请求；
- 日志不包含 Key、密码、Token。

## 4. 维护

```bash
docker compose logs -f backend
docker compose exec mysql mysql -uroot -p"$DB_ROOT_PASSWORD" scenic_ai_guide
docker compose down          # 停止（数据保留在 named volume）
docker compose down -v       # 停止并删除数据卷（谨慎）
```

## 5. 说明与后续

- `backend/Dockerfile` 使用多阶段构建：Maven 构建 fat JAR，运行镜像只用 JRE 17，并以非 root 用户 `lvling` 运行。
- 知识库原始文档已打进镜像（`KNOWLEDGE_RAW_PATH=/app/assets/knowledge_raw`），启动时幂等导入；也可以在管理后台手动上传。
- 上传文件和日志分别挂载到 `uploads`、`logs` 卷，容器重建不丢数据。
- 若使用云平台托管 MySQL/Redis，可删除 compose 中的 `mysql` 服务，把 `DB_URL`/`REDIS_*` 指向托管实例。
- 审计文档 `docs/PRODUCTION_READINESS_AUDIT.md` 中其余 blocker（云服务器、域名、DNS、TLS、公网联调）仍需运维侧完成。
