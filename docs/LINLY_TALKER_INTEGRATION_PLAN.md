# Linly-Talker 接入计划

Linly-Talker 当前作为旅灵的数字人生成引擎，不替代旅灵 Travel Agent、LLM、RAG 或 CityContext。

链路：旅灵 Backend → DigitalHumanService → DigitalHumanProvider → Linly-Talker。

第一阶段建议采用异步 TTS + MuseTalk/Wav2Lip 视频生成，再由 `DigitalHumanPanel` 播放 `videoUrl` 或 `audioUrl`。Linly-Talker-Stream / WebRTC 作为后续实时升级，不改变面板接口。

本地源码审计已完成：源码位于 `third_party/Linly-Talker-main`；TTS 默认 8001、LLM 默认 8002、Talker 默认 8003。Talker 的 `/talker_response/` 是 multipart POST 并直接返回 MP4，不是已确认的异步任务接口。当前代码保留 `LinlyTalkerClient`、`LinlyTalkerConfig` 和 `LinlyTalkerDigitalHumanProvider`，但真实调用仍应由 Backend adapter 完成，前端不直连 Python 端口；并发隔离、鉴权、超时和结果存储完成前保持 TODO/Preview Mode。
