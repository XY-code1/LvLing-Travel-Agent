# 数据库迁移计划

## 1. 原则

- 不一次性重写数据库。
- 不删除 Guido 现有表和数据。
- 先加表/加列，再回填，再双读/适配，最后才评估旧字段下线。
- 每一步可回滚；MySQL 5.7 兼容，避免 MySQL 8 专用语法。

## 2. 现有表复用

| 现有表 | 继续承担的职责 | Phase 2 处理 |
|---|---|---|
| `scenic` | 景区/区域基础数据 | 作为 ScenicArea 候选，增加 city 关联 |
| `spot` | 景点基础数据 | 作为 POI 候选，补类别和城市作用域 |
| `route` | 旧推荐路线 | 只读兼容，逐步映射 Journey |
| `route_spot` | 旧路线景点顺序 | 保留，作为旧路线来源 |
| `admin_feature_item` | 设施/标签/公告等通用运营项 | 先按 moduleType 适配，稳定后拆分 |
| `kb_document`/`kb_chunk` | RAG 文档与切片 | 增加城市/区域 scope |
| `chat_session`/`chat_message` | 对话记录 | 增加 city/journey 关联 |
| `avatar_config` | 数字人形象配置 | 通过 AvatarProfile 作用域复用 |
| `tourist_user` | 游客账号 | 保持不变 |
| `ai_service_config` | AI 服务配置 | 保持不变 |
| `tourist_feedback`、`sentiment_report`、`sys_log`、统计表 | 运营闭环 | 保持不变，后续补 city 维度 |

## 3. 建议新增表

第一阶段只新增必要核心表：

- `city`
- `city_home_content`
- `poi_category`
- `poi`（若不扩展 `spot`，否则可先使用 `spot` 兼容视图）
- `travel_profile`
- `journey`
- `journey_day`
- `journey_item`
- `conversation_context` 或在 `chat_session` 上扩展
- `knowledge_source_scope`
- `avatar_profile`

设施和公告优先评估现有 `admin_feature_item` 能否承载；只有查询、权限或数据生命周期确实不同，才新增 `service_facility`、`announcement`。

## 4. 推荐迁移顺序

1. 新增 `city`，插入默认城市“灵山/无锡示范城市”并建立稳定 ID。
2. 为 `scenic`、`spot`、知识文档和运营项增加可空 `city_id`，回填默认城市。
3. 新增旅行画像和 Journey 四表，不改旧 route 表。
4. 新增/扩展对话上下文与知识来源 scope。
5. 后端建立旧模型到新领域模型的 adapter，先双读只读接口。
6. 新增 cityId 过滤和城市级管理接口。
7. 迁移新数据写入；旧接口继续读兼容数据。
8. 完成验收、备份和回滚演练后，再讨论旧字段/表的长期处置。

## 5. MySQL 5.7 约束

- 使用 `utf8mb4`、`utf8mb4_general_ci` 或已验证的 5.7 字符集排序规则。
- 不使用 `utf8mb4_0900_*`、窗口函数、CTE、`JSON_TABLE`、生成列依赖等 8.0 特性。
- JSON 复杂字段可暂用 `TEXT` + 应用层校验，符合当前 `kb_chunk.embedding` 的做法。
- DDL 必须拆成小批次并先备份；大表加列需评估锁表时间。

## 6. 风险与回滚

| 风险 | 影响 | 控制措施 |
|---|---|---|
| 旧数据无城市 | 跨城市查询污染 | 默认城市回填并禁止 NULL 进入新接口 |
| spot 语义不统一 | POI 类型缺失 | 先建立 category 映射和人工复核 |
| route 与 Journey 不同 | 行程编辑能力不足 | 旧 route 只读，新 Journey 独立建模 |
| 多城市缓存串数据 | 错误推荐 | Redis key 强制含 cityId |
| 迁移中断 | 部分数据不一致 | 批次、计数校验、备份和回滚脚本 |
| MySQL 5.7 锁表 | 服务抖动 | 小批 DDL，低峰执行，先在副本验证 |
