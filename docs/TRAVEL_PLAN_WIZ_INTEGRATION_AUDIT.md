# travel-plan-wiz / travel-plan-viz 集成审计（H0）

审计范围：`E:\Guido-main\third_party\travel-plan-wiz-main`。

本阶段严格只读：未执行上游 JavaScript、Node 测试、安装命令、GitHub Action、网络调研或独立应用，也未修改旅灵业务代码。

## 1. 实际结构

仓库实际项目名是 `travel-plan-viz`，不是 `travel-plan-wiz`：

```text
travel-plan-wiz-main/
├─ README.md
├─ CLAUDE.md
├─ INSTALL.md
├─ LICENSE                         MIT
├─ travel-plan-viz/
│  ├─ SKILL.md                    Skill 主编排文件
│  ├─ assets/
│  │  ├─ map.js                   浏览器地图/坐标/导航引擎
│  │  ├─ reminders.js             提醒计算与 HTML 渲染
│  │  ├─ validate.js              HTML/trip 校验器
│  │  └─ page-contract.md         内容与数据契约
│  └─ references/
│     ├─ research-guide.md
│     ├─ design-guidelines.md
│     └─ porting-to-other-agents.md
├─ samples/                       成都/香港/深圳/东京 HTML
├─ test/                          Node 测试
├─ docs/superpowers/              设计规格与实施计划
└─ .github/                       星标快照 Action 和脚本
```

没有 `.claude-code` 目录。`CLAUDE.md` 是开发/生成约束，不是独立运行时 Skill。

## 2. 它到底是什么

它是“旅行计划可视化”Agent Skill，不是可直接接入后端的旅行数据 Provider，也不是完整的运行时 Travel Agent。

`travel-plan-viz/SKILL.md` 定义两种入口：从目的地和天数规划，或读取已有行程；然后联网补全资料、组织 `trip` JSON、生成手机优先的单文件 HTML、内联地图/提醒引擎并执行机械校验。核心产物是可视化 HTML，而非旅灵后端的结构化可执行 `TravelPlan`。

## 3. 文件分类

### 真正的 Skill

`travel-plan-viz/SKILL.md` 是唯一明确的 Skill 主定义，包含触发描述、模式判断、调研、数据组织、HTML 生成、校验和边界。

### Prompt / 领域规则

- `references/research-guide.md`：POI、坐标、图片、营业时间、餐饮、航班、酒店、天气/季节、提醒、来源和免责声明规则。
- `assets/page-contract.md`：完整 `trip` 数据结构、HTML 必备区块和输出约束。
- `references/design-guidelines.md`：HTML 响应式布局与视觉兜底，不属于 Travel Agent 决策核心。
- `references/porting-to-other-agents.md`：宿主能力与移植提示，不是运行时规划算法。

### Workflow

`SKILL.md` 是“判断模式 → 调研/补全 → 组织 trip → 设计生成 → 机械校验 → 后续迭代”的工作流。

`docs/superpowers/specs` 是设计规格，`docs/superpowers/plans` 是开发实施计划；后者含 shell 和 Git 命令示例，不是运行时 Workflow。

### Claude Code / 开发配置

- `CLAUDE.md`：项目约束、生成规则、测试和提交习惯；不能原样注入 Spring Boot。
- `INSTALL.md`：Claude Code 安装教程，包含 `irm ... | iex` 和 `curl ... | bash`；禁止执行或迁移。
- `.github/workflows/star-history.yml`：读取 GitHub API 并 push 分支；与旅行规划无关。
- `.github/scripts/star-history-chart.js`：星标 SVG 工具；与旅行规划无关。

### 工具定义 / 可复用代码

`assets/reminders.js` 是纯日期与提醒计算，可作为规划结果提醒规则的参考。它不是 Budget/Booking Tool。

`assets/map.js` 包含导航链接、GCJ-02→WGS-84、路线点数组和 Leaflet 初始化。它适合 HTML 展示，不应替换旅灵 AMap Provider；旅灵链路已完成坐标处理时不得重复转换。

`assets/validate.js` 读取 HTML、提取 `trip-data` 并校验字段、坐标和区块。其机械校验思想值得迁移为 Java Validator，但不应在 Spring Boot 请求线程启动 Node 子进程。

`samples/` 是展示 fixture，包含参考价格、营业时间、评分、天气和坐标，不能成为运行时事实或城市 fallback。

## 4. 安全审计

静态检查发现：

- INSTALL 含 PowerShell/curl 安装命令；不执行。
- CLAUDE 和实施计划含 git add/commit/push 示例；不执行。
- GitHub workflow 有 API、secret 和强制 push 行为；不迁移。
- validate.js 有文件读取和 CLI 输出；不作为后端子进程运行。
- map.js 使用外部 OSM 瓦片和地图 URI；无 API Key，但属于浏览器网络依赖。
- Skill/reference 描述外部旅行 Skill/MCP 和联网搜索，但仓库没有相应 Provider 实现；不能当成旅灵已接入能力。
- 未发现需要迁移的 `.env`、token、credential 或 API Key 文件。

没有执行任何上述脚本、workflow、安装或测试命令。

## 5. 旅灵现有架构

```text
TravelAgent
  → TravelContextBuilder
  → IntentExtractor
  → TaskPlanner
  → HarnessEngine
  → ToolRegistry
  → WeatherTool / AMapRouteTool
  → AmapWeatherProvider / AmapPoiProvider / AmapRouteProvider
```

另已有：

- CityContext / City Discovery：`module/city`；
- AMap：`integration/amap`；
- Route：`module/route`、AMap Route Tool/Provider；
- POI：AmapPoiProvider、POIProvider；
- Weather：AmapWeatherProvider、WeatherTool；
- RAG：LocalRagService、Knowledge Service；
- Agent/Harness：agent/core、intent、planner、harness；
- TTS：TouristTtsController 和 Aliyun speech；
- DigitalHuman：tourist-app 数字人组件及 avatar/TTS 模块。

现有 Harness/ToolRegistry 应继续作为唯一执行体系。

## 6. 冲突判断

可兼容的策略：需求字段、老人/家庭/行动不便约束、POI 选择、时间段拆分、节奏缓冲、预算分类、提醒规则、输出契约、生成后机械校验。

冲突点：

- 上游面向单文件 HTML，不能替换 CityContext 或后端 TravelPlan；
- 上游依赖联网调研，旅灵必须优先走已有 Tool/Provider；
- Leaflet/OSM 不能替换 AMap；
- GCJ02→WGS84 仅用于 OSM 展示，不能在后端重复转换；
- 航班、酒店、票价、评分、开放时间字段不代表旅灵已有真实 Provider；
- actionLink 不得手拼或伪造；
- HTML/Node 校验器不能直接成为 Java 运行时；
- 上游没有 authoritative CityContext，不应引入新的城市状态。

## 7. 推荐集成关系

```text
TravelAgent
  ↓
TravelPlanningSkill（策略 + TravelPlanningRequest）
  ↓
现有 HarnessEngine / ToolRegistry
  ↓
CityContextTool  WeatherTool  PoiTool  RouteTool  BudgetTool  RAGTool
  ↓
现有 City Discovery / AMap / 规则引擎 / Local RAG
```

TravelPlanningSkill 不直接 HTTP 请求、不读取 API Key、不控制 Provider。没有真实 Tool 的天气、POI、路线、距离、开放时间和票价必须是 `PENDING/UNKNOWN`。

## 8. H0 结论

### 1. 是否可直接运行

作为 Claude Code/Codex 的旅行计划可视化 Skill，它可按宿主流程生成 HTML。作为旅灵后端 Skill，不能直接运行：它依赖联网研究、文件写入、设计步骤、Node 校验和 Leaflet 展示，没有 Java Tool/Provider/CityContext 适配。

### 2. 最值得复用

- 两种规划入口和“已有计划体检”；
- research-guide 的约束、来源、坐标系和免责声明原则；
- page-contract 的结构化行程字段思想；
- reminders 的确定性规则；
- validate 的机械校验思想；
- 天气/季节、行动不便、节奏和缓冲策略。

### 3. 不应复用

- Claude Code 安装/开发配置；
- GitHub Action、星标脚本、shell/Git 操作；
- HTML/Leaflet 渲染器作为后端执行器；
- samples 的事实数据；
- 自行联网抓取或外部 MCP 作为旅灵运行时；
- 未经真实 Tool 支持的航班、酒店、票价、开放时间和评分。

### 4. 是否冲突

策略层兼容；执行层、City 状态、地图/Provider、坐标转换和数据责任边界冲突，必须适配而非替换。

### 5. Skill 安装位置

```text
backend/src/main/resources/skills/travel-planning/
├─ SKILL.md
├─ prompt/
├─ schema/
└─ examples/
```

仅放人工审核后的纯策略和契约，不镜像第三方仓库。

### 6. Java Adapter

需要薄 Adapter，建议放 `backend/src/main/java/com/guido/scenicai/agent/skill/`。不创建第二套 Harness、Tool Registry 或 City Store。

### 7. 接入未来 Harness

Adapter 接受 `TravelContext + TravelIntent`，生成 `TravelPlanningRequest` 和任务策略，委托现有 Harness；HarnessContext 携带 CityContext 和前序 ToolResult；最终聚合 Timeline、路线、交通时间、预算、风险、证据和 ExecutionTrace。

### 8. H1 最小文件范围

预计新增：

```text
backend/src/main/resources/skills/travel-planning/SKILL.md
backend/src/main/resources/skills/travel-planning/prompt/planning-system.md
backend/src/main/resources/skills/travel-planning/schema/travel-planning-request.schema.json
backend/src/main/resources/skills/travel-planning/schema/travel-plan.schema.json
backend/src/main/resources/skills/travel-planning/examples/hangzhou-family-low-mobility.json
backend/src/main/java/com/guido/scenicai/agent/skill/TravelPlanningSkill.java
backend/src/main/java/com/guido/scenicai/agent/skill/TravelPlanningRequest.java
backend/src/main/java/com/guido/scenicai/agent/skill/TravelPlanningResult.java
```

预计最小修改：

```text
agent/context/TravelContext.java
agent/context/TravelContextBuilder.java
agent/intent/TravelIntent.java
agent/intent/IntentExtractor.java
agent/planner/TaskPlanner.java
agent/harness/HarnessContext.java
domain/trip/TravelPlan.java
agent/api/TravelAgentResponse.java
agent/core/TravelAgentTest.java
agent/harness/HarnessEngineTest.java
```

可能新增 `CityContextTool`、`PoiTool`、`BudgetTool`、`ValidatorTool`。OpeningHours、票价、航班和酒店 Tool 只有真实 Provider 存在时才增加，否则保持 `PENDING/UNKNOWN`。

## 9. H0 边界确认

本次没有复制上游文件到 backend，没有创建 Java Adapter，没有修改 CityContext、AMap、Route、Weather、RAG、TTS 或 DigitalHuman，没有安装依赖，没有执行 Node/上游脚本，没有 commit 或 push。
