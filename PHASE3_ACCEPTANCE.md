# Phase 3 验收报告

验收时间：2026-08-28

| 验收项 | 结果 | 说明 | 是否需要修复 |
|---|---|---|---|
| backend 8080 | PASS | 端口监听，`/doc.html` 返回 200，JDK 17 | 否 |
| admin-web 5173 | PASS | Vite 已启动，端口监听，admin-web 构建 PASS | 否 |
| admin login | PASS | 管理员登录返回 200 | 否 |
| tourist login | PASS | 游客登录返回 200 | 否 |
| MySQL | PASS | MySQL 5.7 可连接，city 表可读写 | 否 |
| Redis | PASS | 6379 TCP 连接正常 | 否 |
| OPTIONS/CORS | PASS | 管理接口 OPTIONS 返回 200，允许 localhost:5173 | 否 |
| A. city 表 | PASS | 表存在；临时城市新增、编辑、停用均返回 200 | 否 |
| B. admin 城市管理 | PASS | `/api/admin/city/page` 返回 Seed 城市，前端路由和页面已构建 | 否 |
| C. 城市 CRUD/启停 | PASS | 通过临时验收城市完成全流程；停用为软停用 | 否 |
| D. city_id 关联 | PASS | scenic/spot/route/route_spot/设施/知识/会话/avatar 已增加字段和索引 | 否 |
| E. POI 隔离 | PASS | 无锡 Context 返回 4 个 POI，杭州 Context 返回 0 个 | 否 |
| F. 服务隔离 | PASS | 无锡返回 2 个设施，杭州返回 0 个 | 否 |
| G. 公告隔离 | PASS | 查询统一按 city_id 过滤；当前 Seed 城市无公告数据，未出现跨城数据 | 否 |
| H. Context 同步 | PASS | 首页从全局 Pinia CityContext 加载城市、Hero、POI 和问题 | 否 |
| I. 刷新保留城市 | PASS | 当前 cityId 写入 `guido_city_id`，刷新后按该 ID 恢复 | 否 |
| J. CityResolver | PASS | 支持 cityCode/cityName 精确解析 | 否 |
| K. Discovery/Provider fallback | PASS | “青岛”未预置查询返回 fallback context，无 500 | 否 |
| L. Phase 1 回归 | PASS | 原 admin scenic、tourist hot、登录、CORS、文档入口均正常 | 否 |

## 失败与处理

验收初始发现 8080 和 5173 的前台进程已退出，属于运行进程状态问题，已使用既有 JDK 17/Maven/Vite 命令重新启动。城市表首次查询发现缺少审计字段，已以增量方式补齐 `create_by`、`update_by`、`deleted`，未删除或重写数据。

## 当前结论

Phase 3 验收通过，当前具备进入 Phase 4 的条件。Phase 4 尚未开始。

仍未验收的真实外部能力不属于本阶段：地图、天气、Web Search、完整 Travel Agent、GPS、Journey Engine 和数字人 Lip Sync。
