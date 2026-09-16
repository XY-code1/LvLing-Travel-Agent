# 旅灵数字人 MVP 部署方案

## 推荐链路

现有 Travel Agent / TTS → 音频文件 → Linly-Talker Talker（首选 Wav2Lip 适配）→ MP4 → Backend → `DigitalHumanPanel`。

第一阶段不运行 Linly-Talker 自带 LLM，不替换旅灵 CityContext、RAG 或 Travel Agent。TTS 优先复用旅灵已有能力；Linly-Talker 的 EdgeTTS 可作为实验性替代，但它依赖在线服务。

## Provider

`DigitalHumanProvider` 的主实现为 `LinlyTalkerDigitalHumanProvider`，降级为 `MockDigitalHumanProvider`，HeyGem 保留为备用。Provider 应在 Backend 侧选择，前端只消费统一结果和状态：`idle/listening/thinking/speaking/error`。

## 当前状态

前端已保留 Linly-Talker 配置、Client 和 Provider 骨架，但真实 Backend adapter 尚未接通；面板应明确显示“数字人服务未连接 / Preview Mode”。不把 8001/8003 暴露给浏览器，也不宣称已经具备真实数字人能力。

## 最小部署前置条件

- 独立 Python 3.10 环境（源码依赖较旧，Python 3.13 不作为首选）。
- 与显卡驱动匹配的 PyTorch/CUDA wheel，单独验证，不改系统 Python。
- FFmpeg；视频和音频处理链需要它或 imageio-ffmpeg。
- Wav2Lip/MuseTalk 对应权重、单张数字人形象和音频输入。

本轮不安装以上依赖和权重。6 GB RTX 4050 适合先做单任务、低分辨率实验，不适合同时运行大型 LLM、CosyVoice/GPT-SoVITS 和高分辨率生成。

