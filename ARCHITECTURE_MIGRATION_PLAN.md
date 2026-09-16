# LvLing Travel Agent V2 增量迁移计划

## 总体约束

- 每个阶段都保持 `main` 可运行，可单独回退。
- 不移动现有 `module/*` 和 `integration/*`。
- 新 Agent 入口与旧 Chat/Planner 并行，验收后再切默认入口。
- Tool 只能调用 Service/Provider，不能调用 Controller。
- 不在缺少真实数据源时用 mock 冒充完成。

## V2.1 Agent Core

目标：建立后端权威的 TravelRequest/TravelPlan 契约和确定性主流程，不引入 Harness。

新增文件：

- `domain/trip/TravelRequest.java`
- `domain/trip/TravelPlan.java`、`TravelDay.java`、`TravelActivity.java`、`TravelCheck.java`
- `agent/api/TravelAgentController.java` 与请求/响应 DTO。
- `agent/context/TravelContext.java`、`TravelContextFactory.java`
- `agent/intent/IntentExtractor.java`
- `agent/planner/TaskPlan.java`、`TaskStep.java`、`TravelAgentService.java`

复用：CityResolver/CityContext/CityDiscovery、现有用户和会话身份。

修改：仅新增 API 注册；`tourist-app/services/travelPlanner.ts` 增加调用 Agent API 的可切换 facade，不立即删除 V1 规则。

风险：前后端 TravelPlan 字段漂移；LLM 解析不稳定。

测试：IntentExtractor 单测；TravelContext 优先级单测；Controller contract test；缺失预算/日期输入测试。

验收：固定杭州请求得到合法结构化 TravelRequest 和 TravelPlan 骨架；缺字段不导致 500；旧 Planner 仍可用。

## V2.2 Harness Core

目标：用最小执行内核替换 AgentService 中的手工步骤调用。

新增文件：

- `agent/harness/HarnessEngine.java`
- `ExecutionContext.java`、`ExecutionState.java`
- `ToolRegistry.java`、`ToolExecutionResult.java`
- `RetryPolicy.java`、`ValidationResult.java`、`ExecutionTrace.java`
- `tool/TravelTool.java`

复用：Spring Bean 容器、统一异常、日志基础设施。

修改：TravelAgentService 委托 Harness；暂不改现有 Tool/Provider。

风险：过度抽象、错误被统一结果吞掉。

测试：假 Tool 的成功、超时、不可重试、一次重试、未知 Tool、状态转换测试。

验收：同一 TaskPlan 可重放；Trace 顺序确定；任何失败都有 errorCode 和安全消息；无无限重试。

## V2.3 AMapRouteTool

目标：把已验证的多点步行路线接入 Harness。

新增文件：

- `tool/map/AMapRouteTool.java`
- `AMapRouteInput.java`、`RouteToolOutput.java`

复用：`AmapRouteProvider`、`AmapWebServiceClient` 及其真实 polyline。

修改：TaskPlanner 在坐标准备完成后生成 `amap.route.walking` 步骤；TravelPlanAssembler 接收距离、耗时、polyline。

风险：点位顺序错误、高德点数限制、坐标缺失、外部 API 超时。

测试：Provider 契约测试；2 点和多点路线；空 polyline；非法坐标；Key 未配置。

验收：Agent Trace 明确记录 Tool -> Provider；地图展示真实 polyline；Agent/Tool 未直接拼 URL；旧 AMap Controller 保持可用。

## V2.4 Weather + POI Tools

目标：让目的地、必去景点坐标和天气进入后端计划证据链。

新增文件：

- `tool/map/AMapPoiTool.java`
- `tool/weather/WeatherTool.java`
- `tool/city/CityDiscoveryTool.java`
- `tool/knowledge/RagTool.java`
- `tool/budget/BudgetTool.java`

复用：AmapPoiProvider、AmapGeocodingProvider、AmapWeatherProvider、CityDiscoveryService、LocalRagService。

修改：LocalRagService 增加 city scope 的兼容重载；TaskPlanner 声明步骤依赖；前端逐步移除浏览器端 POI/天气计划组装。

风险：同名 POI、城市 scope 不完整、实时天气不等于未来天气、预算规则缺少票价。

测试：杭州/异地同名 POI；无坐标 geocode；天气无结果；RAG city 隔离；预算部分数据。

验收：每个 Check 带真实 source；无数据返回 PENDING/WARN，不伪造 PASS；前端 TravelPlan 来自后端。

`TravelEventTool` 继续 pending，直到存在可靠事件/公告数据源。

## V2.5 Validator

目标：建立可验证的计划质量门槛。

新增文件：

- `agent/validator/TravelPlanValidator.java`
- weather、openingHours、route、timeConflict、budget 规则类。

复用：TravelPlan、Tool Results、现有景点开放时间、路线和天气数据。

修改：Harness 在组装候选计划后进入 VALIDATING；响应暴露 Checks 和 violations。

风险：把未知误判为通过；规则冲突；时区/营业时间解析。

测试：预算超限、时间重叠、无路线、恶劣天气、开放时间未知。

验收：未知事实只能是 PENDING/WARN；每个 PASS 都有 source；失败明确标记是否可重规划。

## V2.6 Dynamic Replanner

目标：对可修复冲突执行一次受控重规划。

新增文件：

- `agent/replanner/DynamicReplanner.java`
- `ReplanDecision.java`、`ReplanReason.java`

复用：TaskPlanner、Harness、Validator、原 Tool Results 中仍有效的结果。

修改：Harness 增加 REPLANNING 转换和最大次数；Trace 记录旧计划、冲突和差异摘要。

风险：循环、重复外部调用、重规划改变必去地点、结果不可解释。

测试：预算超限替换候选；天气冲突调整室内点；不可达路线；相同错误签名。

验收：最多一次自动重规划；requiredPlaces 不被静默删除；失败时返回最佳可用计划和明确 warning。

## V2.7 Memory

目标：在明确授权和 scope 下复用会话与偏好。

新增文件：

- `memory/ConversationMemoryService.java`
- `memory/PreferenceMemoryService.java`
- `memory/MemorySnapshot.java`

复用：ChatSession、ChatMessage、TouristUser.interestTags。

修改：TravelContextFactory 读取 MemorySnapshot；必要时给 ChatSession 增加 Trip 关联应走独立数据库 migration。

风险：跨用户/跨城市泄漏、陈旧偏好覆盖当前请求、未经确认写长期记忆。

测试：user/city 隔离；显式请求优先；匿名会话不持久化；删除/退出后的行为。

验收：当前请求始终覆盖记忆；Trace 只记录记忆摘要和来源，不记录敏感原文；长期偏好需用户确认。

## V2.8 Execution Trace UI

目标：在 Planner Workspace 展示真实 Agent 进度和可诊断失败。

新增文件：

- Backend `agent/trace` 查询 DTO/Controller；需要历史查询时才新增 trace 表和 migration。
- Frontend `api/agent.ts`、Agent status/trace 组件。
- Admin 可选 Trace 查看页，仅在持久化落地后创建。

复用：现有 PlannerStep UI、SSE 工具、系统日志、Planner Workspace。

修改：`pages/chat/index.vue` 消费 Agent SSE：理解需求、查询城市、查询景点、天气、路线、预算、验证、重规划、完成。

风险：Trace 泄露 prompt、用户信息或 Provider 错误细节；SSE 断线导致 UI 状态悬挂。

测试：正常完成、Tool 失败、重规划、断线恢复、移动端 Tab、敏感字段扫描。

验收：UI 展示的每一步对应真实后端事件；不伪造完成；错误可定位到 tool/errorCode；Trace 不包含 Key/Secret。

## 阶段顺序与停止条件

```text
V2.1 契约稳定
  -> V2.2 执行可追踪
  -> V2.3 首个真实 Tool
  -> V2.4 补齐计划证据
  -> V2.5 可验证
  -> V2.6 可重规划
  -> V2.7 有边界的记忆
  -> V2.8 对用户可见的执行轨迹
```

任一阶段若破坏现有 Chat、AMap 路线或 Planner Workspace，应停止进入下一阶段，先恢复兼容性。V2.1 实施前需要先确认本设计及 TravelPlan 后端契约。
