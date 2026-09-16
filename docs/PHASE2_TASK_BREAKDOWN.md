# Phase 2 任务拆解

## 0. 设计冻结（当前阶段）

- 冻结 City Context、领域模型和迁移边界。
- 确认旧接口兼容策略和默认城市。
- 不编写最终页面，不进入 Phase 3。

## 1. P2-A 数据与领域基础

- 建立 City、Category 和 city 作用域设计。
- 评估 `scenic/spot/admin_feature_item` 的字段扩展与适配。
- 定义 TravelProfile、Journey、JourneyDay、JourneyItem DTO/校验规则。
- 产出 MySQL 5.7 DDL、回填和回滚脚本。

## 2. P2-B City Context

- backend 城市查询、城市摘要和 Context resolve。
- tourist-app 全局 CityContext store 与请求注入。
- 旧灵山数据映射为默认城市。
- Redis 缓存键按 cityId 隔离。

## 3. P2-C POI 与内容中心

- 游客端城市/区域/分类/搜索查询。
- POI 详情、讲解入口、加入行程。
- admin-web 城市、景区、POI、分类内容管理。
- 先保持现有页面和接口可用，再逐步接入新字段。

## 4. P2-D Journey Engine

- 结构化 TravelProfile 表单。
- 自然语言解析为 TravelProfile。
- 生成结构化 Journey。
- Journey 编辑、版本、重新规划和局部替换。
- 旧 route 作为候选来源，不强制迁移为新 Journey。

## 5. P2-E Agent 编排

- Intent Router 和工具 schema。
- 先实现 Database Search、RAG Search、Journey Planner、Service Search。
- 再按实际需求接 Weather/Web/External POI。
- 统一 sources、warnings、tool logs 和降级结果。

## 6. P2-F 服务、公告、SOS、数字人贯通

- 统一城市/区域查询条件。
- Conversation 关联 CityContext 和 Journey。
- 复用现有 ASR/TTS/Avatar，在 Agent 结果上增加事件适配。
- 保持 SOS 原功能，补充城市与区域信息。

## 7. P2-G 管理端运营闭环

- 城市首页内容。
- 景点/POI、设施、公告、知识源作用域。
- AvatarProfile 与现有 avatar_config 的映射。
- AI 日志和数据分析补充 cityId。

## 8. 开发顺序与验收门槛

```text
领域/DDL设计
 → City Context
 → POI查询与管理
 → Journey结构化模型
 → Agent最小工具集
 → 服务/公告/SOS贯通
 → 数字人事件适配
 → 运营后台补齐
```

每个阶段必须通过：旧登录接口、旧景区/路线接口、MySQL 5.7、Redis、OPTIONS/CORS、admin-web 基线回归。任何阶段不得以删除旧功能作为完成条件。

## 9. 不在 Phase 2 首批实现

- GPS 实时定位和导航。
- 大规模天气、地图、Web Search 供应商接入。
- 视觉最终稿和全量 UI 重构。
- 多 Agent 自主协作、复杂计费和企业级多租户。

## 10. Phase 3 最优先候选

完成 Phase 2 后，优先开发“城市切换 → POI 查询 → TravelProfile → Journey 草案 → AI 数字人解释”的单城市垂直闭环，再扩展天气、外部 POI 和多城市运营规模。
