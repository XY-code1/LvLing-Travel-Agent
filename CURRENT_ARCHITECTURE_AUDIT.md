# LvLing Travel Agent 当前架构审计

> 审计范围：`backend`、`tourist-app`、`admin-web`、`database`、`docs`。
> 审计原则：只描述仓库中真实存在的代码；已有设计文档但尚未落地的能力单独标注。

## 1. 结论

当前项目是一个可运行的“景区导览 + 多城市上下文 + AI 问答 + 高德地图 + 数字人”系统，已经具备升级 Travel Agent 的大部分基础设施，但尚未形成后端统一的 Agent 执行内核。

- 已有：城市上下文、景区/景点/预设路线、聊天会话、RAG、LLM/VLM/Embedding 路由、阿里云 ASR/TTS、数字人配置、高德 POI/地理编码/天气/步行路线 Provider。
- 部分已有：Provider 架构、结构化 `TravelRequest`/`TravelPlan`、规划步骤与检查项。
- 关键缺口：结构化规划目前主要在前端 `tourist-app/services/travelPlanner.ts`；后端没有 `TravelAgent`、Harness、Tool Registry、Validator、Replanner、Agent Memory、Execution Trace。
- 当前聊天后端仍以“景区导览问答”为中心，LLM 输出是自然语言，不是可执行、可验证、可重规划的旅行计划。
- `docs/AGENT_ARCHITECTURE.md`、`docs/TRAVEL_DOMAIN_MODEL.md` 含有目标性设计，不能视作已经实现。

## 2. 技术栈与部署单元

### 2.1 Backend

- Java 17、Spring Boot 3.2.5。
- MyBatis-Plus + MySQL；Redis 依赖已引入。
- Sa-Token 区分管理端与游客端认证。
- Spring MVC REST + `SseEmitter` 流式响应。
- JDK `HttpClient` 调用外部 AI 与高德服务。
- 单 Maven 模块，主包为 `com.guido.scenicai`。

### 2.2 Tourist App

- UniApp/Vue 页面结构，TypeScript 服务层。
- Pinia 管理前端状态。
- 高德 JS API Loader 负责地图底图。
- Three.js / VRM 仍保留；正式首页使用 `DigitalHuman2D.vue`。

### 2.3 Admin Web

- Vue 3、Vite、TypeScript、Pinia、Element Plus。
- 管理城市、景区、景点、路线、知识库、AI 配置、数字人、用户、聊天记录等。

## 3. Backend 当前模块

### 3.1 `common`

提供基础实体、统一结果、异常、鉴权拦截、加密和通用工具。它是所有模块的基础依赖，不应被 Agent 反向依赖。

### 3.2 `module.city`

关键类：

- `City` / `CityMapper`：城市持久化。
- `CityResolver` / `CityResolverImpl`：按 code/name 解析启用城市。
- `CityContextService` / `CityContextServiceImpl`：加载城市上下文。
- `CityDiscoveryService` / `CityDiscoveryServiceImpl`：聚合城市、景区、POI、服务和知识源。
- `POIProvider`、`ServiceProvider`、`KnowledgeProvider`：城市内容聚合接口。
- `TouristCityController`、`AdminCityController`。

真实行为：`CityDiscoveryServiceImpl` 聚合本地数据库数据；未知城市返回 fallback 城市上下文和空数据。`CityContextServiceImpl` 当前委托 Discovery，传入的 `cityId` 未直接参与加载。

### 3.3 `module.scenic` / `module.spot` / `module.route`

- `Scenic`：城市下景区，含地址、开放时间、票务、坐标。
- `Spot`：城市/景区下景点，含介绍、标签、停留时间、坐标。
- `Route` + `RouteSpot`：运营人员配置的静态推荐路线及顺序。
- `TouristRouteRecommendServiceImpl`：按景区、兴趣、路线类型返回预设路线。

注意：这里的 `Route` 是内容运营路线，不等同于高德实时路径，也不等同于用户可保存的多日 Trip。

### 3.4 `module.map` + `integration.amap`

Controller：`TouristAmapController`。

Provider：

- `AmapPoiProvider`：`/v5/place/text`。
- `AmapGeocodingProvider`：地理编码。
- `AmapWeatherProvider`：先取得 adcode，再查询实时天气。
- `AmapRouteProvider`：逐段调用 `/v5/direction/walking`，聚合距离、耗时和真实 polyline。
- `AmapWebServiceClient`：统一 Key、HTTP、错误信息与高德响应校验。

调用链：

```text
tourist-app/amapService.ts
  -> TouristAmapController
  -> Amap*Provider
  -> AmapWebServiceClient
  -> 高德 Web Service API
```

目前 `AmapRouteProvider` 是可直接复用的 Provider；未来 Tool 必须调用它，Agent 不应调用 Controller 或拼高德 URL。

### 3.5 `module.chat`

关键入口：

- `TouristChatController`：文字、文字流、语音、语音流。
- `TouristVisionController`：图片识别问答。
- `TouristRouteController`：静态路线推荐。
- `TouristChatServiceImpl`：同步聊天总编排。
- `TouristTextStreamService` / `TouristVoiceStreamService`：SSE 编排。

当前同步文字链路：

```text
TextChatDTO
  -> 校验游客与 ChatSession
  -> LocalRagService
  -> 未命中时 LocalKnowledgeService
  -> LlmClientRouter
  -> EmotionAnalyzer
  -> AliyunSpeechClient(TTS，失败时返回 null)
  -> ChatMessage 持久化
  -> ChatAnswerVO
```

当前 System Prompt 仍是“景区导览数字人”。此链路没有 Tool Calling、结构化计划、验证或重规划。

### 3.6 `module.knowledge` + AI integrations

- `KnowledgeServiceImpl`：文档上传、解析、分块和向量化管理。
- `LocalRagService`：优先 Embedding + 内存余弦 Top-K，失败降级关键词检索；返回上下文和来源。
- `EmbeddingClientRouter`：按 AI 配置选择 Embedding 客户端。
- `LlmClientRouter`：按默认启用的 LLM 配置和 protocol 路由到 OpenAI/Anthropic 兼容客户端。
- `VlmClientRouter`：多模态识别路由。
- `AiConfigLoader`：从 `ai_service_config` 读取启用且默认配置，解密敏感字段形成快照。

RAG 当前主要按 `scenicId` 过滤；实体已有 `cityId`，但 `LocalRagService.retrieve` 尚未以 `cityId` 作为显式检索参数。

### 3.7 `module.avatar` + `integration.aliyun`

- `AvatarConfigServiceImpl`：数字人配置、默认配置和用户选择。
- `TouristTtsController`：独立 TTS API。
- `AliyunSpeechClient`：ASR、TTS、Token 获取，支持请求 voice 覆盖数据库默认 voice。
- `AliyunAvatarClient`：阿里云数字人流相关适配。

TTS 调用优先级已实现为请求 voice > 数据库默认 voice。数字人是结果呈现通道，不应进入 Agent 规划核心。

### 3.8 `module.tourist`

- `TouristUser`：游客账户与兴趣标签。
- `ChatSession`：含 `cityId`、`scenicId`、数字人配置及会话统计。
- `ChatMessage`：问题、回答、来源、情绪、音频、耗时和成功状态。
- 认证、个人资料、会话创建/历史/消息查询 Controller 与 Service。

当前会话历史属于 Conversation Memory 的原始数据，但还不是 Agent Memory：没有偏好抽取、摘要、记忆检索、过期策略或行程关联。

## 4. Tourist App 当前架构

### 4.1 City

`CityContext` 通过城市列表和 `/api/tourist/cities/{cityId}/context` 获取数据，驱动首页、景点和 Planner 的当前城市。前端本地状态与后端城市校验并存，但 Planner 请求还没有统一携带一个后端 `TravelContext`。

### 4.2 Planner Workspace

`pages/chat/index.vue` 已形成对话、行程 Timeline、地图/路线三栏工作台。

`services/travelPlanner.ts` 当前承担：

- 正则解析 `TravelRequest`。
- 调用静态路线推荐。
- 调用高德 POI、地理编码、天气。
- 生成活动、预算估算、Checks 和 PlannerStep。
- 返回前端 `TravelPlan`。

这是真实可运行的 V1，但执行发生在浏览器，缺少后端权限、统一重试、可追踪工具调用、服务端验证和持久化，不能作为 V2 最终 Agent 核心。

`components/planner/AmapPlannerMap.vue` 使用 TravelPlan 中的真实坐标展示 Marker，并通过后端步行路线 API 获取 polyline。

### 4.3 Chat 与数字人

- `api/chat.ts` 支持同步和 SSE 文本/语音请求。
- `DigitalHuman2D.vue` 及 `digital-human-2d/*` 提供状态、服装、VoiceProfile、LipSync 接口。
- `DigitalHuman3D.vue`、Three.js/VRM 代码保留为实验能力。
- `services/ttsService.ts` 负责后端 TTS 优先及浏览器 fallback。

## 5. Admin Web 当前架构

管理页面真实存在：AI 配置、数字人、城市、景区、景点、路线、知识库、游客、聊天记录、反馈、日志、情感分析和功能项。

与 Agent V2 直接相关的可复用入口：

- `views/ai-config/AiConfigManage.vue` + `api/aiConfig.ts`。
- `views/city/CityManage.vue` + `api/city.ts`。
- `views/knowledge/KnowledgeManage.vue` + `api/knowledge.ts`。
- `views/route/RouteManage.vue`、`views/spot/SpotManage.vue`。
- `views/chat-record/ChatRecordManage.vue`、`views/system/SysLogManage.vue`。

目前没有 Tool Registry、Agent Policy、Execution Trace 或 Memory 管理页面。

## 6. 数据模型与表

当前 schema 主要表：

- 身份：`sys_admin`、`tourist_user`。
- 城市内容：`city`、`scenic`、`spot`、`route`、`route_spot`、`admin_feature_item`。
- 知识：`kb_document`、`kb_chunk`、`kb_test_record`。
- AI/数字人：`ai_service_config`、`avatar_config`。
- 会话：`chat_session`、`chat_message`。
- 运营：`tourist_feedback`、`sentiment_report`、`sys_log`、`stat_daily`。

`phase3_city_context.sql` 已为 scenic、spot、route、route_spot、知识、会话、数字人等增加 `city_id`。

未发现真实 Trip/Journey、TravelPlan、TravelDay、TravelActivity、AgentExecution、ToolCall、Memory 表。当前结构化计划仅存在于前端 TypeScript 类型和运行时状态。

## 7. 当前外部 API

- 高德 Web Service：POI、地理编码、天气、步行路线。
- 高德 JS API 2.0：前端地图底图、Marker、Polyline 展示。
- LLM：OpenAI-compatible / Anthropic-compatible，具体供应商由数据库配置。
- Embedding：OpenAI-compatible。
- VLM：OpenAI-compatible。
- 阿里云 NLS：ASR、TTS；另有 Avatar 适配。

## 8. 当前关键依赖关系

```text
tourist-app pages
  -> frontend api/services
  -> backend controllers
  -> module services
  -> mapper / integration clients
  -> MySQL / external APIs

Planner V1 特例：
pages/chat
  -> frontend travelPlanner
  -> route API + AMap APIs
  -> frontend assembles TravelPlan

Chat 特例：
TouristChatService
  -> RAG/LocalKnowledge + LLM + Emotion + TTS + Avatar + Chat persistence
```

## 9. 架构风险

1. 规划逻辑在前端，业务规则无法集中治理、复放和审计。
2. `TouristChatServiceImpl` 同时负责认证、检索、LLM、情绪、TTS、持久化，继续叠加 Agent 会进一步膨胀。
3. 城市本地 Provider 与外部 AMap Provider 名称相似但职责不同，未来需要 Tool 输入/输出契约隔离。
4. 当前 `TravelPlan` 没有后端权威模型和版本，无法可靠保存、分享或动态重规划。
5. RAG 城市隔离尚未贯穿检索入口。
6. 当前错误降级多为局部 try/catch，没有统一 RetryPolicy、ValidationResult 和 ExecutionTrace。
7. LLM Router 负责模型路由，但尚无结构化输出校验和工具调用协议。
8. ChatSession/Message 可作为记忆来源，但不能直接等同长期记忆。

## 10. 可复用清单

- 直接复用：City/CityDiscovery、Spot/Scenic/Route 查询、Amap Providers、LocalRagService、LLM/Embedding/VLM Router、AiConfigLoader、ChatSession/Message、游客认证、AliyunSpeechClient、AvatarConfig、前端 AMap Map、DigitalHuman2D。
- 包装后复用：Amap Providers -> Tools；LocalRagService -> RagTool；CityDiscoveryService -> CityDiscoveryTool；静态路线推荐 -> 候选路线 Tool。
- 逐步下沉：前端 `parseTravelRequest`、预算规则、Checks、PlannerStep 和 TravelPlan 组装。
- 暂不需要：通用工作流 DSL、插件市场、分布式 Agent、事件总线、独立向量数据库、复杂中间件链。

## 11. Controller / Service / Provider 清单

### 11.1 游客端 Controller

- 城市：`TouristCityController`。
- 内容：`TouristScenicController`、`TouristSpotController`、`TouristRouteController`。
- 地图：`TouristAmapController`。
- 对话：`TouristChatController`、`TouristVisionController`。
- 数字人：`TouristTtsController`、`TouristAvatarConfigController`。
- 用户与会话：`TouristAuthController`、`TouristUserController`、`TouristSessionController`。
- 反馈：`TouristFeedbackController`。

### 11.2 管理端 Controller

- `AdminAuthController`、`DashboardController`。
- `AdminCityController`、`ScenicController`、`SpotController`、`RouteController`。
- `KnowledgeController`、`AiConfigController`、`AvatarConfigController`。
- `AdminTouristUserController`、`AdminChatController`、`AdminFeedbackController`。
- `AdminFeatureItemController`、`SentimentReportController`、`SysLogController`、`AdminFileController`。

### 11.3 核心 Service

- 城市：`CityContextService`、`CityDiscoveryService`、`CityResolver` 及实现类。
- 内容：`ScenicService`、`SpotService`、`RouteService` 及实现类。
- 对话：`TouristChatServiceImpl`、`TouristTextStreamService`、`TouristVoiceStreamService`、`TouristRouteRecommendServiceImpl`。
- 知识：`KnowledgeServiceImpl`、`LocalRagService`、`LocalKnowledgeService`。
- 用户：`TouristAuthServiceImpl`、`TouristUserServiceImpl`、`ChatSessionServiceImpl`。
- 数字人：`AvatarConfigServiceImpl`。
- 运营：Feedback、Feature、Dashboard、Sentiment、Log 对应 Service。

### 11.4 Provider / Client

- 地图：`AmapWebServiceClient`、`AmapPoiProvider`、`AmapGeocodingProvider`、`AmapRouteProvider`、`AmapWeatherProvider`。
- 城市聚合：`POIProvider`、`ServiceProvider`、`KnowledgeProvider` 及本地实现。
- AI：`LlmClientRouter` + OpenAI/Anthropic compatible client；`EmbeddingClientRouter`；`VlmClientRouter`。
- 阿里云：`AliyunSpeechClient`、`AliyunAvatarClient`。
- 配置与日志：`AiConfigLoader`、`AiCallLogHelper`。

## 12. 主要前后端调用链

```text
城市切换
tourist-app scenic API -> TouristCityController
  -> CityContextService/CityDiscoveryService -> City/Scenic/Spot/Feature/Knowledge Mapper

旅行规划 V1
pages/chat -> travelPlanner.ts
  -> TouristRouteController -> TouristRouteRecommendService -> Route/RouteSpot/Spot
  -> TouristAmapController -> Amap Providers -> 高德 Web Service
  -> 前端组装 TravelPlan -> AmapPlannerMap

AI 问答
pages/chat -> api/chat.ts -> TouristChatController
  -> TouristChatService/TextStreamService
  -> LocalRagService -> LlmClientRouter -> AliyunSpeechClient
  -> ChatMessage -> SSE/ChatAnswerVO -> DigitalHuman2D

管理配置
admin-web aiConfig/avatar/knowledge/city APIs -> 对应 Admin Controller
  -> Service/Mapper -> MySQL
  -> AiConfigLoader/AvatarConfigService 在运行时读取
```
