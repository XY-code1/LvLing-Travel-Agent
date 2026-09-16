# LvLing Travel Agent Architecture V2

## 1. 设计目标

V2 在当前可运行版本上增加一个后端权威的旅行任务执行层，使旅行计划具备结构化输入、真实工具调用、验证、有限重试、动态重规划和可追踪性。它不是重写现有 Guido 模块，也不是通用 Agent 框架。

核心原则：

- 增量迁移；现有 Controller、Service、Provider 和前端功能持续可用。
- Provider 只封装外部服务，Tool 将 Provider 转换为 Agent 可调用能力。
- Agent 不直接调用高德、数据库 Mapper 或数字人。
- Harness 只负责执行、状态、重试、验证和 Trace，不做旅行领域决策。
- LLM 可参与意图提取和规划，但所有实时事实必须来自 Tool，并经过 Validator。
- 数字人只消费最终回答/计划事件，不耦合规划过程。

## 2. 目标分层

```text
API Layer
  -> Travel Agent
      -> Context + Intent + Planner
      -> Harness Engine
          -> Tool Registry
          -> Tools
              -> Existing Services / Providers
                  -> DB / AMap / Weather / LLM / RAG
          -> Validator
          -> Dynamic Replanner (bounded)
      -> TravelPlan + ExecutionTrace
  -> Frontend Planner Workspace
  -> DigitalHuman2D / TTS (presentation)
```

### 2.1 对用户建议目录的裁剪结论

- `api/`：需要，但作为 `agent.api`，避免与现有模块 Controller 混淆。
- `agent/context|intent|planner|executor|validator|replanner`：需要，分阶段创建。
- `harness/registry|policy|middleware|trace`：概念需要；V1 不拆四层子框架，先在 `agent.harness` 放最小类，成熟后再拆。
- `tool/map|weather|knowledge|budget|event|city`：按工具落地顺序创建；V2.3 前不机械建空目录。
- `memory/`：V2.7 创建，先以 ChatSession/Message 为数据源。
- `domain/city|poi|route|user`：当前已有 `module.city/scenic/spot/route/tourist`，不迁移；新建重复领域模型会造成双写。
- `domain.trip`：当前不存在，等 V2.1 确定后端 TravelPlan 契约后新增。
- `provider/amap|weather|llm|aliyun|digitalhuman`：当前 `integration/*` 已承担 Provider 责任，保留原路径；不为命名统一而移动。
- `knowledge/`：当前 `module.knowledge` + `integration.embedding` 已存在，复用。
- `avatar/`：当前 `module.avatar` + 前端 DigitalHuman 已存在，复用。
- `common/`：已存在，禁止塞入 Agent 领域逻辑。

## 3. Agent 主执行链

### 3.1 UserRequest -> TravelRequest

输入：原始文本、游客/匿名会话标识、显式 cityId、locale、请求时间。
处理：`IntentExtractor` 生成结构化 `TravelRequest`。
输出：destination、startDate、days、budget、travelers、preferences、requiredPlaces、mobilityPreference、transportPreference、rawText，以及缺失字段列表和解析置信度。

缺失字段允许为空；只有无法确定城市且工具执行必须依赖城市时，才返回可补充问题。

### 3.2 TravelRequest -> TravelContext

`TravelContextFactory` 按以下优先级合并上下文：

```text
请求显式城市 > 当前会话城市 > 已保存 Trip 城市 > 用户默认城市 > 系统默认城市
```

输出包含：可信 City、用户偏好摘要、会话引用、时间/预算约束、允许的工具和 provider 可用性。上下文保留 snapshot，供 Trace 重放。

### 3.3 TaskPlanner -> TaskPlan

输入：TravelRequest + TravelContext。
输出：有序且有依赖的 `TaskStep`，例如 city.resolve、poi.search、weather.current、route.walking、budget.calculate。每步包含 toolName、typed input、依赖、超时、是否必需。

V2.1 可先用确定性 Java 规则生成；LLM 只用于补充意图，不依赖模型自由生成任意工具名。

### 3.4 HarnessEngine -> Tool Execution

输入：TaskPlan + ExecutionContext。
行为：从 ToolRegistry 获取允许的工具，按依赖执行，记录输入摘要、来源、耗时、结果与错误。
输出：`ToolExecutionResult<?>` 集合和持续更新的 `ExecutionState`。

### 3.5 ResultValidator

输入：TravelRequest、TravelContext、工具结果、候选 TravelPlan。
输出：`ValidationResult`，包含 weather、openingHours、route、timeConflict、budget 的状态、消息、来源和可修复性。

### 3.6 DynamicReplanner

只在以下条件触发：必需工具失败、POI 无坐标、预算超限、时间冲突、天气风险或路线不可达。它根据 ValidationResult 生成修订后的 TaskPlan，而不是无限重跑原计划。

约束：默认最多一次重规划；相同失败签名不重试；外部服务配置缺失不可重试。

### 3.7 Final TravelPlan -> Frontend -> Digital Human

最终输出：

- 结构化 `TravelPlan`：summary、budget、checks、days/activities、route geometry、sources、warnings。
- `ExecutionTrace`：step、tool、provider、status、duration、source、errorCode，不含 Secret。
- 面向用户的自然语言摘要。

前端 Timeline 和地图消费 TravelPlan；数字人消费摘要文本和状态事件，再进入 TTS/口型链。数字人失败不回滚 TravelPlan。

## 4. Harness V1

Harness V1 只包含以下真实需要的类型：

### 4.1 `HarnessEngine`

执行 TaskPlan；管理状态转换；调用 ToolRegistry；应用 RetryPolicy；调用 Validator；产出 Trace。它不解析自然语言、不计算旅行预算、不访问外部 API。

### 4.2 `ExecutionContext`

不可变执行快照：executionId、request、travelContext、user/session、deadline、允许工具、已完成结果。禁止存放 API Key。

### 4.3 `ExecutionState`

最小状态：`CREATED`、`PLANNING`、`RUNNING`、`VALIDATING`、`REPLANNING`、`SUCCEEDED`、`FAILED`。

### 4.4 `ToolRegistry`

按稳定 tool name 注册和获取 Spring Bean；拒绝未知或当前上下文不允许的工具。V1 不做动态插件加载。

### 4.5 `ToolExecutionResult<T>`

字段：toolName、status、data、source、startedAt、durationMs、retryable、errorCode、safeMessage。Tool 不通过异常向 Agent 泄漏 Provider 细节。

### 4.6 `RetryPolicy`

根据错误码和工具幂等性决定是否重试。配置缺失、参数无效、无数据不重试；网络超时和明确限流可有限重试。V1 上限 1 次。

### 4.7 `ValidationResult`

包含总状态、各 Check、violations、replannable。Check 必须带 source，避免 LLM 自证正确。

### 4.8 `ExecutionTrace`

按序记录 state change、tool call、validation、replan 和 final。先以内存返回并写应用日志；数据库持久化留到 Trace UI 阶段。

明确延期：通用 middleware pipeline、DSL、并行 DAG 调度器、分布式队列、动态插件发现。

## 5. Tool Architecture

### 5.1 最小工具契约

```java
public interface TravelTool<I, O> {
    String name();
    Class<I> inputType();
    ToolExecutionResult<O> execute(I input, ExecutionContext context);
}
```

Tool 输入输出使用 Agent 领域 DTO，不直接暴露 Controller DTO、JsonNode 或供应商响应。

### 5.2 工具与现有实现映射

- `AMapRouteTool` -> `AmapRouteProvider.walking(points)`。
- `AMapPoiTool` -> `AmapPoiProvider.search(keywords, city)`。
- `WeatherTool` -> `AmapWeatherProvider.current(city)`；名称保持供应商中立，未来可替换 Provider。
- `CityDiscoveryTool` -> `CityDiscoveryService.discover(code, name)`。
- `RagTool` -> `LocalRagService.retrieve(...)`，后续补 city scope。
- `BudgetTool` -> 新增确定性预算规则；不需要外部 Provider。
- `TravelEventTool` -> 暂无真实事件数据源，标记 pending，不在 V2.1 创建空实现。

### 5.3 AMapRouteTool 正确链路

```text
TravelAgent
  -> HarnessEngine
  -> ToolRegistry.get("amap.route.walking")
  -> AMapRouteTool
  -> AmapRouteProvider
  -> AmapWebServiceClient
  -> AMap API
```

禁止 Agent 调用 `TouristAmapController`；Controller 是 HTTP 边界，不是内部 Service。禁止 Tool 重新拼高德 URL。

## 6. Memory 设计

V2.7 前不新增复杂记忆系统。

- Working Memory：当前 ExecutionContext 和 Tool Results，随执行结束释放。
- Conversation Memory：复用 ChatSession/ChatMessage，新增摘要适配器，不改原表语义。
- Preference Memory：从 TouristUser.interestTags 和确认过的旅行偏好读取；只有用户确认后写入。
- Trip Memory：依赖未来 Trip 持久化，不能用 ChatMessage 代替。

记忆检索必须带 userId/cityId scope；匿名用户只使用当前会话内存。

## 7. RAG 与 Provider 边界

- RAG 提供有来源的知识证据，不负责路线计算。
- LLM Provider 生成结构或自然语言，不提供实时天气、开放时间或距离事实。
- AMap Provider 提供实时/地理事实，不参与业务编排。
- Tool 把 Provider 输出归一化，Validator 判断其能否满足计划约束。
- `AiConfigLoader` 继续作为 AI Provider 配置入口；Harness 不读取密钥。

## 8. 推荐 Java/Spring 目录树

以下是最终方向，不代表一次性创建全部目录：

```text
backend/src/main/java/com/guido/scenicai/
├─ agent/
│  ├─ api/                    # TravelAgentController、API DTO/VO（V2.1）
│  ├─ context/                # TravelContext、Factory（V2.1）
│  ├─ intent/                 # IntentExtractor（V2.1）
│  ├─ planner/                # TaskPlanner、TaskPlan、TravelPlanAssembler（V2.1）
│  ├─ harness/                # V1 最小执行内核（V2.2）
│  ├─ executor/               # 仅当 Harness 编排复杂后再拆
│  ├─ validator/              # ResultValidator（V2.5）
│  ├─ replanner/              # DynamicReplanner（V2.6）
│  └─ trace/                  # Trace query/persistence（V2.8）
├─ tool/
│  ├─ map/                    # AMapRouteTool（V2.3）
│  ├─ city/                   # CityDiscoveryTool（V2.4）
│  ├─ weather/                # WeatherTool（V2.4）
│  ├─ knowledge/              # RagTool（V2.4）
│  ├─ budget/                 # BudgetTool（V2.4）
│  └─ event/                  # 有真实数据源后再建
├─ memory/                    # V2.7
├─ domain/
│  └─ trip/                   # TravelRequest/Plan/Day/Activity/Check
├─ module/                    # 全部现有业务模块原位保留
│  ├─ city/ scenic/ spot/ route/ chat/ tourist/ knowledge/ avatar/ ...
├─ integration/               # 全部现有 Provider 原位保留
│  ├─ amap/ aliyun/ llm/ embedding/ vlm/ common/
└─ common/                    # 现有基础设施
```

前端保留：

```text
tourist-app/
├─ pages/chat/index.vue
├─ components/planner/AmapPlannerMap.vue
├─ services/amapService.ts
├─ services/travelPlanner.ts  # 迁移期 facade，最终只调 Agent API
├─ api/agent.ts               # V2.1 新增
├─ stores/CityContext...
└─ components/DigitalHuman2D.vue
```

## 9. 兼容策略

- 新增 `/api/tourist/agent/plan`，旧 chat、route、amap API 不变。
- 前端通过 feature flag 或小范围入口切换到 Agent API；失败可退回当前 V1 Planner，不静默伪造结果。
- 现有实体、Mapper、Provider 不移动。
- V2 初期 TravelPlan 可不入库；保存行程待领域模型稳定后单独迁移。
- ExecutionTrace 先随响应返回，确认字段稳定后再建表。
- TTS、ASR、DigitalHuman2D 不修改，只消费 Agent 最终输出。

## 10. 非目标

V2 当前不建设多 Agent 协作、通用工作流平台、自动代码执行、浏览器搜索 Agent、任意数据库写 Tool、分布式 Harness 或模型驱动的无限自主循环。
