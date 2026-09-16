# 旅灵数字人员工架构

```text
Tourist/Admin Web
        ↓ unified Backend API
DigitalHumanService
        ↓ provider selection
DigitalHumanProvider
  ├─ LinlyTalkerDigitalHumanProvider (primary)
  ├─ HeyGemDigitalHumanProvider (backup)
  └─ MockDigitalHumanProvider (baseline fallback)
        ↓ server-side adapter
Linly-Talker TTS/Talker APIs
```

Linly-Talker 的源码目录为 `third_party/Linly-Talker-main`。真实端口和参数只以源码为准：TTS 8001、LLM 8002、Talker 8003。当前前端骨架不直连这些端口；真实接入必须在 Backend 完成鉴权、超时、临时文件隔离、结果存储和错误降级。

数字人不是 CityContext 的替代品。请求应携带 cityId/cityCode、用户上下文和对话文本，由旅灵 Agent 决定内容，再由 Provider 负责表现层生成。
