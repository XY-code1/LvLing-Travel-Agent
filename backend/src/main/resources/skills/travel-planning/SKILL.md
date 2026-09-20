---
name: travel-planning
description: 将旅行需求结构化为城市、日期、时长、预算、同行人、兴趣、必去点和行动约束，并通过旅灵现有 Harness/ToolRegistry 生成可验证行程。只使用真实 CityContext、POI、天气和路线工具，不补写未知事实。
---

# 旅灵旅行规划 Skill

## 目标

把用户自然语言需求转换为 `TravelPlanningRequest`，再交给现有 Harness 执行真实工具。规划策略可以安排节奏、必去点、缓冲和预算检查，但不能直接请求 AMap、读取 Key 或虚构天气、距离、票价、开放时间。

## 规划规则

- 先确认当前 CityContext，再选择 POI；城市、`cityKey` 和 `adcode` 必须来自旅灵 City Discovery。
- 必去点优先保留；其余点按城市、兴趣、开放时间、距离和行动约束筛选。
- 老人、家庭或低步行需求应减少跨区移动，安排更长停留和休息缓冲。
- 每天按早/中/晚组织，户外活动结合真实天气工具结果调整；没有天气结果时标记待确认。
- 路线顺序和交通时间只能来自 RouteTool；不能根据地图距离自行编造耗时。
- 预算只汇总真实 ToolResult 或明确的规则估算，并区分已知、估算和未知。
- 开放时间、票价、航班和酒店没有真实工具时返回 `PENDING/UNKNOWN`。
- 任何工具失败都记录在 ExecutionTrace 和 warnings 中，不用样例数据兜底。

## 输出

输出包含结构化需求、工具调用列表、时间线、路线、预算、风险/约束检查、校验结果和 ExecutionTrace。
