# Phase 4D 状态

## 已完成

- 首页根路径不再强制登录，游客可先浏览首页和城市内容。
- 保留现有 CityContext、城市切换和首页数据绑定。
- 增加 Desktop 宽屏约束，统一首屏最大宽度、Hero 高度、数字人区域比例，减少 H5 放大到 Desktop 时的错位。
- 增加 `DigitalHumanProvider`、`HeyGemDigitalHumanProvider`、`MockDigitalHumanProvider`、`HeyGemClient` 和 `HeyGemConfig` 骨架。
- 首页数字人区域明确标记为“数字人服务未连接 · Preview Mode”，没有伪装成真实 HeyGem 接入。

## 修改文件

- `tourist-app/manifest.json`
- `tourist-app/utils/request.ts`
- `tourist-app/utils/auth.ts`
- `tourist-app/router/index.ts`
- `tourist-app/pages/index/index.vue`
- `tourist-app/components/DigitalHumanPanel/index.vue`
- `tourist-app/services/digitalHuman/*`

## HeyGem 状态

官方仓库 Git clone 和源码 ZIP 获取均因当前环境网络传输中断失败，未留下半成品。未执行 Docker pull、模型下载或服务启动。HeyGem API 地址和参数未猜测，Adapter 保留 TODO。

## 真实能力与 Mock

- 真实：现有旅灵 CityContext、景点数据、城市切换和原有 Avatar/ASR/TTS 代码。
- Mock：新增数字人 Provider 的 Preview Mode 状态和返回结构。
- 未实现：HeyGem 真实 API 调用、模型推理、视频合成和 Lip Sync。

## 检查

- `tourist-app` Vite 构建命令已执行。
- 仍需在 HBuilderX 重新运行 H5，进行浏览器视觉验收。

## 下一步

待取得完整 HeyGem 源码后，依据实际源码补齐 API Adapter；随后再进行真实数字人联调。当前不进入 Phase 5。
