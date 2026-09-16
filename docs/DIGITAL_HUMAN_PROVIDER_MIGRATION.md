# DigitalHumanProvider 迁移

主 Provider 从 HeyGem 调整为 Linly-Talker。

```text
DigitalHumanProvider
├── LinlyTalkerDigitalHumanProvider  主方案
├── MockDigitalHumanProvider         当前可运行降级
└── HeyGemDigitalHumanProvider       备用
```

游客端只依赖 `DigitalHumanPanel` 和旅灵 Backend，不直接调用 Linly-Talker。Linly-Talker 未连接时继续显示 `数字人服务未连接 · Preview Mode`。
