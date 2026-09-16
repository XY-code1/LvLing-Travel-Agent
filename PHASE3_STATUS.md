# Phase 3 状态：旅灵 City Context 基础架构

## 1. 实际修改文件

- `database/migration/phase3_city_context.sql`
- `backend/src/main/java/com/guido/scenicai/module/city/**`
- 现有景区、景点、路线、设施、知识库、会话、数字人实体增加 `cityId`
- `tourist-app/types/index.ts`、`api/scenic.ts`、`stores/index.ts`、`pages/index/index.vue`
- `admin-web/src/api/city.ts`、`src/types/index.ts`、`src/views/city/CityManage.vue`、`src/router/index.ts`

## 2. 数据库迁移

新增 `city` 表及 `city_id` 关联字段和索引；现有数据映射到通用 Seed 城市“无锡”，未删除原表或原数据。另保留一个“杭州”不完整 Seed 城市用于 Discovery/Fallback 验证。SQL 使用 MySQL 5.7 兼容语法，字符集为 utf8mb4。

## 3. CityContext / Resolver / Discovery

`CityResolver` 通过 cityCode/cityName 查询本地城市；`CityContextService` 聚合景区、POI、设施、公告和知识来源；未知或不完整城市返回可用的 fallback context，不使页面崩溃。

## 4. Provider 架构

已建立 `CityProvider`、`POIProvider`、`ServiceProvider`、`KnowledgeProvider` 接口及 Local Provider 实现。后续可增加 External/Search Provider，不改变业务层。

## 5. API

- 游客：`GET /api/tourist/cities`、`GET /api/tourist/cities/{id}/context`、`GET /api/tourist/context/resolve`
- 管理：`GET /api/admin/city/page`、`POST/PUT /api/admin/city`、`DELETE /api/admin/city/{id}`（停用，不物理删除）

## 6. 前端改动

游客端增加全局城市与 CityContext 状态，首页从 CityContext 加载当前城市景点和 Hero 图片，并保留旧接口 fallback。后台增加城市管理入口。未进行视觉重构。

## 7. 测试结果

- backend JDK 17 编译：PASS
- backend 8080：PASS，启动日志确认 Java 17.0.16
- MySQL 3306：PASS，MySQL 5.7 增量迁移已执行
- Redis 6379：PASS
- admin login：PASS
- tourist login：PASS
- OPTIONS/CORS：PASS，返回 200 且允许 localhost:5173
- 城市列表：2 个 Seed 城市
- 无锡 context：返回既有 POI 数据
- 青岛未预置查询：fallback PASS
- admin-web 构建：PASS
- tourist-app：源码已接入；本机无独立 HBuilderX/UniApp 构建链，需手动在 HBuilderX 运行 H5

## 8. 当前未实现能力

真实地图、天气、Web Search、完整 Travel Agent/Tool Calling、完整 Journey Engine、GPS、数字人 Lip Sync、全国 POI 批量导入及外部 Provider。

## 9. 已知问题

城市切换控件尚未做最终 UI 呈现；旧景区/路线接口仍保持兼容，部分旧页面仍以 scenicId 为上下文。首次部署需在基础 schema/data 执行后执行本迁移一次。

## 10. Phase 4 建议

先完善城市选择交互和所有游客查询的 cityId 透传，再实现 Journey 结构化领域模型，最后接入可配置的天气/地图/搜索 Provider 与 Travel Agent。
