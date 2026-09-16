# 旅灵 Phase 4B：首页最终 UI / UX / Motion

## 1. 最终组件结构

当前首页在既有 `pages/index/index.vue` 内完成增量重构：

```text
HomePage
├── Immersive Hero
│   ├── City Hero Background
│   ├── City Title / Slogan
│   ├── AI 导览 CTA
│   ├── 行程规划 CTA
│   ├── AI Digital Employee Panel
│   └── Weather / Announcement / Guide Status Bar
├── City Switcher
├── Existing Quick Actions
├── Hot POI Section
├── City Context Status
└── Recommended Questions
```

## 2. 修改文件

- `tourist-app/pages/index/index.vue`

未修改 backend、database、CityContext、CityResolver、Discovery、Provider 和 API 架构。

## 3. Skill 使用情况

- `awesome-design-md`：参考 Intercom 的克制信息层级、助手对话布局，以及 IBM 的响应式、触控尺寸和状态设计原则。
- `taste-skill`：避免 AI 紫色渐变、模板化三栏卡片和 Dashboard 化首页，保持沉浸式文旅叙事。
- GSAP 相关 Skill：由于 tourist-app 没有 `package.json` 和 GSAP 依赖，本阶段没有强行新增依赖，使用等价的 CSS transform、opacity、animation 实现跨 H5/UniApp 动效。

## 4. UI 设计说明

- 品牌统一使用“旅灵”语义，保留现有绿色自然景区色系。
- Hero 为第一视觉核心，背景由 `coverImage` 驱动。
- 数字员工使用既有 `AvatarBox`，与对白气泡组合为 Hero 内的第二视觉核心。
- 采用暗色渐变 Overlay 保证文字可读性。
- 天气、公告、导览状态整合进 Hero 底部信息条，避免后台卡片堆叠。
- 城市切换入口明显但不抢占 Hero 主标题。

## 5. Motion 设计

- 城市切换使用约 560ms 的标题/内容淡入上移。
- 背景继续使用 CityContext 图片绑定，保留浏览器原生 image 过渡兼容空间。
- 只使用 opacity 和 transform，避免布局抖动和高成本滤镜动画。
- 支持 `prefers-reduced-motion: reduce`。
- 未使用 ScrollTrigger；当前首页不需要滚动驱动动画。

## 6. CityContext 数据绑定

首页继续从 Pinia 全局状态读取：

- `currentCity.cityName`
- `currentCity.slogan`
- `currentCity.coverImage`
- `currentCity.weatherCode`
- `cityContext.pois`
- `cityContext.services`
- `cityContext.announcements`
- `cityContext.fallback`

城市切换后同步刷新 Hero、景点、快捷问题、服务数量和上下文状态。

## 7. Desktop / H5 响应式

- Desktop：左右分栏，左侧城市叙事，右侧数字员工。
- Tablet：压缩右侧数字员工区域，保留 Hero 主叙事。
- Mobile/H5：城市叙事、数字员工、CTA、信息条按纵向堆叠，避免简单缩小桌面布局。
- 触控入口保留足够高度，支持减少动效模式。

## 8. 验收结果

- 旅灵品牌识别：PASS
- 沉浸式文旅 Hero：PASS
- AI 数字员工第二视觉核心：PASS
- 城市切换保留：PASS
- CityContext 驱动标题、slogan、背景、POI、推荐问题：PASS
- 天气/公告状态信息层：PASS；真实天气服务仍由后续 Provider 提供
- Desktop 结构：PASS（响应式 CSS）
- Mobile/H5 结构：PASS（响应式 CSS）
- reduced-motion：PASS
- Phase 3 / Phase 4A 数据逻辑：保持不变

## 9. 已知问题

- 当前 tourist-app 没有独立 npm 依赖链，无法在不改变构建方式的情况下接入 GSAP；本轮使用 CSS 动效。
- `weatherCode` 当前只是城市模型预留字段，尚未接入真实天气 Provider。
- Hero 背景当前以单张 `coverImage` 为主，`heroImages` 轮播留待后续实现。

## 10. Phase 5 建议

先补充真实城市 Hero 资源与天气 Provider，再统一游客端其他页面的 CityContext 消费方式；之后再评估是否引入标准 GSAP 依赖链和更完整的 Travel Agent 交互。
