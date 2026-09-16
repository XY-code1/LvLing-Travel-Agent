# Travel Domain Model

## 1. 核心实体关系

```text
City
 ├─ ScenicArea  0..N
 │    └─ POI     0..N
 ├─ ServiceFacility 0..N
 ├─ Announcement 0..N
 └─ KnowledgeSource 0..N

TouristUser 1 ── 0..N TravelProfile
TravelProfile 1 ── 0..N Journey
Journey 1 ── 1..N JourneyDay
JourneyDay 1 ── 0..N JourneyItem
JourneyItem ── 0..1 POI / ServiceFacility / custom item

TouristUser 1 ── 0..N Conversation
Conversation ── 0..N Message
Conversation ── 0..1 Journey
Conversation ── 0..N KnowledgeSource reference

City 1 ── 0..N AvatarProfile
```

## 2. 实体定义

| 实体 | 责任 | 关键字段 |
|---|---|---|
| City | 城市一级租户/内容上下文 | id, name, code, cover, status |
| ScenicArea | 城市内可运营的景区/区域 | id, cityId, name, address, geo, status |
| POI | 面向游客的统一地点 | id, cityId, scenicAreaId, type, name, intro, geo, tags |
| TravelProfile | 用户本次旅行偏好 | cityId, days, people, budget, interests, pace, mobility |
| Journey | 一次可保存/编辑的行程 | userId, profileId, cityId, title, status |
| JourneyDay | 行程中的某一天 | journeyId, dayNo, date, summary |
| JourneyItem | 一天内的景点/服务/餐饮/交通条目 | dayId, poiId, itemType, startTime, duration, note, sortOrder |
| ServiceFacility | 城市/区域服务设施 | cityId, scenicAreaId, type, name, location, hours, status |
| Announcement | 城市/景区运营公告 | cityId, scenicAreaId, title, content, level, validFrom, validTo |
| Conversation | 一次连续 AI 对话 | userId, cityId, journeyId, title, status |
| KnowledgeSource | RAG 可追溯知识来源 | cityId, scenicAreaId, sourceType, documentId, authority, status |
| AvatarProfile | 数字人的产品和语音身份 | cityId, avatarConfigId, welcomeText, voice, enabled |

## 3. POI 类型

至少支持：自然风光、历史人文、古镇、博物馆、寺庙、夜游、美食、摄影、亲子。类型应为可运营字典或 `poi_category`，不要写死在页面。

## 4. TravelProfile

结构化字段：城市、日期/天数、人数、同行关系、预算、兴趣、体力、节奏、必去 POI、少走路、餐饮、避开拥堵。自然语言输入先解析为同一结构，解析失败时保留原文并要求补充，不直接生成不可编辑路线。

## 5. Journey 约束

- 每个 `JourneyDay.dayNo` 在 Journey 内唯一。
- `JourneyItem.sortOrder` 在 JourneyDay 内唯一。
- 行程草稿可编辑、可重新生成；已保存版本不可被无提示覆盖。
- 删除 POI 后历史 JourneyItem 保留快照名称和说明，避免历史行程失真。

## 6. 现有模型映射

| 新模型 | Guido 现有对应 | 迁移策略 |
|---|---|---|
| City | 无；由现有景区推导默认城市 | 新增，旧数据暂归默认城市 |
| ScenicArea | `scenic` | 保留表，增加 city 关联或适配表 |
| POI | `spot` | 保留表，逐步补 city/type/统一地点字段 |
| Journey/JourneyDay/Item | `route`/`route_spot` | 旧路线只读兼容，新行程使用新表 |
| ServiceFacility | `admin_feature_item` 中 facility 类型 | 先适配，后按需要拆表 |
| Announcement | 现有公告功能/feature item | 先确认实现，再抽取统一表 |
| Conversation | `chat_session`/`chat_message` | 复用并增加 city/journey 关联 |
| KnowledgeSource | `kb_document`/`kb_chunk` | 增加 scope 元数据，复用切片能力 |
| AvatarProfile | `avatar_config` | 保留，增加 city 作用域适配 |
