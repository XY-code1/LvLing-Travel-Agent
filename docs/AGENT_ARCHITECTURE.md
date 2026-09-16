# Travel Agent 与工具架构

## 1. 定位

Travel Agent 是现有聊天/RAG/数字人链路之上的编排层，不替换 LLM、Embedding、ASR、TTS 或 Avatar 客户端。它负责理解意图、选择可信数据源、调用工具、组织结构化结果并维护上下文。

```text
ASR / text / image
        ↓
Conversation + CityContext + UserContext + JourneyContext
        ↓
Intent Router
        ↓
Tool Planner
        ↓
Database → RAG → External API → Web Search
        ↓
LLM structured response
        ↓
Journey / answer / action event
        ↓
TTS → Avatar（需要时）
```

## 2. 工具清单

| 工具 | 默认数据源 | 输出 |
|---|---|---|
| Database Search | MySQL | POI、设施、公告、路线候选 |
| RAG Search | kb_document/kb_chunk | 可引用知识片段 |
| Web Search | 外部搜索 | 仅在本地资料不足且允许时使用 |
| Weather Tool | 外部天气服务 | 城市天气摘要 |
| POI Search | POI 数据 | 地点候选与详情 |
| Route Planner | POI/旧路线/约束 | 有序路线候选 |
| Journey Planner | TravelProfile + POI | Journey JSON |
| Announcement Search | MySQL | 有效公告 |
| Service Search | ServiceFacility | 服务设施列表 |

工具必须有：名称、输入 schema、城市范围、权限、超时、来源、错误码和可追溯引用。工具失败时返回可解释的降级结果，不让 LLM 编造实时事实。

## 3. 意图路由

第一版最小意图：`POI_INFO`、`POI_GUIDE`、`JOURNEY_CREATE`、`JOURNEY_EDIT`、`SERVICE_QUERY`、`ANNOUNCEMENT_QUERY`、`SOS_HELP`、`GENERAL_CITY_QA`。路由结果必须包含 `cityId`、置信度和所需工具。

## 4. 上下文优先级

```text
请求显式 cityId
  > 会话 CityContext
  > Journey.cityId
  > 游客默认城市
  > 系统默认城市
```

用户上下文、行程上下文和会话上下文不能覆盖显式城市选择。每次工具调用记录 context snapshot，便于审计和重放。

## 5. 结构化输出

Journey Planner 返回 `TravelProfile + Journey + warnings + sources`；Journey Edit 返回变更摘要和新版本，不只返回自然语言。普通问答返回 answer、sources、suggestedActions，可选触发数字人事件。

## 6. 安全与成本

- 管理员工具和游客工具分开授权。
- Web Search、Weather、外部 POI 默认关闭或按配置启用。
- 记录 tool、provider、耗时、成功、来源和 token/cost（若可得）。
- 维持现有 Sa-Token 双账号隔离和 AI 配置加密。
- Agent 不能直接写任意数据库；写操作必须经过领域服务校验。
