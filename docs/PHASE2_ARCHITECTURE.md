# Phase 2 架构设计

## 1. 目标架构

```text
tourist-app / admin-web
          │
          │ REST / SSE，统一 City Context
          ▼
backend
 ├─ city        城市与城市首页
 ├─ poi         景区、POI、分类
 ├─ journey     旅行画像与结构化行程
 ├─ service     服务设施与查询
 ├─ announcement公告
 ├─ conversation对话上下文
 ├─ knowledge   RAG / KnowledgeSource
 ├─ agent       意图路由与 Tool Calling
 ├─ avatar      数字人、ASR、TTS
 └─ existing    现有 admin、tourist、route、feedback、log
          │
          ├─ MySQL：领域数据、行程、会话、知识元数据
          ├─ Redis：token、短期上下文、缓存
          └─ 外部服务：按工具配置启用
```

## 2. 分层原则

- Controller：只处理鉴权、输入和输出，不编排 AI 细节。
- Application Service：编排 City Context、Journey、Agent 工作流。
- Domain：保持领域对象与数据库 DTO 分离。
- Integration：复用现有 LLM/VLM/Embedding/ASR/TTS/Avatar 客户端。
- Repository/Mapper：优先复用 MyBatis-Plus 和现有 Mapper 习惯。

## 3. City Context

每次游客请求可带 `cityId`；未提供时使用游客当前选择的城市或系统默认城市。后端解析后生成只读上下文：

```json
{
  "cityId": 1,
  "cityName": "杭州",
  "scenicAreaId": null,
  "locale": "zh-CN",
  "knowledgeScope": ["city:1"],
  "source": "user_selection"
}
```

Context 必须进入查询过滤、RAG scope、Agent tools 和 Journey 生成，不允许由前端只改显示文本。

## 4. 兼容边界

- 继续保留 `/api/admin/**` 与 `/api/tourist/**`，新增能力优先使用 `/api/v2/**` 或清晰的新资源路径。
- 旧 `/api/tourist/scenic/**`、`/api/tourist/spot/**` 可以作为兼容适配层，内部补充默认城市。
- 管理端继续使用现有 Sa-Token `admin` 账号体系；游客继续使用 `tourist` 账号体系。
- 旧表先读不删；新表上线后通过视图/服务适配旧接口。

## 5. 非目标

Phase 2 不做最终 UI 重构、GPS/实时导航、大规模第三方 API 接入、数据库一次性重写或 Phase 3 实现。
