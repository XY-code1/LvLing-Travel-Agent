# Phase C1 Legacy City Audit

审计范围：`tourist-app` 源码（排除构建产物 `dist/`、`unpackage/`）。

## 结论

源码中已没有“无锡、灵山胜境、灵山大佛、灵山梵宫、九龙灌浴、五印坛城、灵山大照壁”的页面中文硬编码。当前遗留污染主要由旧本地景区 API 和静态素材间接产生，而不是页面中的城市字符串。

| 文件 / 模块 | 分类 | 用途 | 是否修改 | 修改方案 |
| --- | --- | --- | --- | --- |
| `tourist-app/composables/useCityContext.ts` | B fallback | 无持久化上下文时自动请求浏览器定位；没有统一默认城市 | 是 | 优先恢复持久化上下文，否则解析默认杭州；浏览器定位只在用户主动操作时执行 |
| `tourist-app/stores/index.ts` | B fallback / state | authoritative `cityContext/currentCity` 和 localStorage 持久化 | 是 | 保持唯一 store；补强上下文有效性与城市切换后的会话清理 |
| `tourist-app/pages/index/index.vue` | C 页面逻辑 / F 文案 | 城市列表来自 `/cities`；示例行程固定为杭州；无封面时使用旧景区 Hero | 是 | 当前城市内容完全读取 store；杭州示例仅用于杭州；其他城市使用当前 POI；Hero 使用通用城市图，不使用灵山素材兜底 |
| `tourist-app/pages/spots/index.vue` | C 页面逻辑 | 已读取 `cityContext.pois`，无独立城市状态 | 小改 | 监听 `cityKey`，明确城市切换后的 loading/empty 状态 |
| `tourist-app/pages/services/index.vue` | C 页面逻辑 | 已读取 `cityContext.services`，无独立城市状态 | 小改 | 监听 `cityKey`，明确数据来源与空状态 |
| `tourist-app/pages/route/index.vue` | C 页面逻辑 | GPS 附近和手动景点仍调用本地 `scenicId` / `getHotScenic()` 接口 | 是 | 动态城市使用 `cityContext.pois`；不让本地灵山热门景点跨城市回退；路线 prompt 继续注入当前城市 |
| `tourist-app/services/travelPlanner.ts` | A Demo fixture | `KNOWN_PLACES` 是意图抽取词典，含杭州、成都、西安样例 | 否 | 不作为页面数据源，不会把地点注入其他城市；保留解析能力 |
| `tourist-app/components/layout/AppHeader.vue` | C 页面逻辑 | 当前城市/最近访问/搜索/定位入口 | 小改 | 保持只读唯一 store；不恢复全国硬编码列表 |
| `tourist-app/pages/chat/index.vue` | C 页面逻辑 | 已从 store 读取城市名与中心点，并传入旅行规划 | 核验 | 保持 currentCity 注入，城市切换时清除旧景区会话 |
| `tourist-app/pages/announcements/index.vue` | C 页面逻辑 | 已读取当前 CityContext 公告 | 核验 | 保持当前城市空状态，不回退旧景区公告 |
| `tourist-app/static/images/spot-*.webp` | E 图片/静态资源 | 灵山旧景点素材 | 否（保留） | 不删除现有功能和资产；禁止作为其他城市 fallback |
| `tourist-app/static/images/home-hero-scenic.webp` | E 图片/静态资源 | 旧景区 Hero 素材 | 是（停止兜底引用） | 改用通用城市背景；杭州如后端提供封面则优先使用 |
| 后端 `/cities`、`/scenic/hot`、`/route/recommend` 返回内容 | D 数据库真实数据 | 原 Guido 本地景区资料 | 不改后端 | 前端仅在上下文确实匹配本地城市/景区时使用；动态城市不回退这些数据 |

## CityContext 权威链路

`useTouristStore.cityContext` 是唯一 authoritative context。`currentCity` 是该上下文中 `city` 的响应式引用，不建立第二个 city store。

初始化顺序：

1. 当前 Pinia 会话内已选择城市；
2. 用户主动定位成功后写入的城市；
3. `guido_city_context` 中最后一次有效城市；
4. 初始化层统一解析默认杭州（`adcode=330100`）；解析不可用时仅建立无内容的杭州 fallback，不伪造 POI、服务或天气。

浏览器定位不再在首次页面加载时自动弹权限；只由“定位当前位置”触发。
