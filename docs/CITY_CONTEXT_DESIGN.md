# City Context 设计

## 1. 目标

City Context 是旅灵的全局状态，不是前端局部筛选器。城市切换后，页面数据、查询范围、知识库范围、数字人上下文和推荐结果必须一致变化。

## 2. Context 结构

```ts
type CityContext = {
  cityId: number;
  cityName: string;
  scenicAreaId?: number;
  locale: string;
  source: 'user_selection' | 'deep_link' | 'default' | 'journey';
  knowledgeScope: string[];
  updatedAt: string;
};
```

前端保存当前选择用于页面恢复；后端每次请求重新校验城市是否存在、是否启用以及资源是否属于该城市，不能信任前端传来的名称。

## 3. 切换流程

```text
用户选择城市
 → 更新本地 CityContext
 → 请求城市摘要
 → 并行刷新首页/POI/服务/公告
 → 清理不属于新城市的临时筛选
 → 重新绑定 Conversation Context
 → 后续 Agent 请求携带 cityId
```

正在编辑的 Journey 不因首页切换而静默改变城市；应显示当前行程城市，并由用户明确切换或新建行程。

## 4. 数据隔离规则

- POI、设施、公告、知识源、AvatarProfile 均必须可追溯到 City。
- ScenicArea 是 City 下的区域容器；跨区域查询必须显式允许。
- RAG 检索先按 `cityId/scenicAreaId` scope 过滤，再做向量/关键词检索。
- 城市未配置内容时返回空状态和默认提示，不回退到其他城市的事实数据。

## 5. API 契约方向

建议新增：

- `GET /api/tourist/cities`
- `GET /api/tourist/cities/{cityId}/home`
- `GET /api/tourist/pois?cityId=&scenicAreaId=&category=`
- `GET /api/tourist/services?cityId=&scenicAreaId=&type=`
- `GET /api/tourist/announcements?cityId=&scenicAreaId=`
- `POST /api/tourist/context/resolve`

管理端对应 `/api/admin/city/**`、`/api/admin/poi/**` 等资源接口，继续使用 admin Sa-Token。

## 6. 缓存

Redis 可缓存城市摘要、热门 POI 和有效公告，key 必须包含 `cityId`、版本或更新时间。切换城市不复用未带城市维度的旧缓存。

## 7. 验收标准

选择杭州后，首页、热门 POI、公告、服务和 AI 检索均只出现杭州数据；切换西安后同样成立；刷新页面能够恢复城市；旧灵山默认数据仍可通过默认城市访问。
