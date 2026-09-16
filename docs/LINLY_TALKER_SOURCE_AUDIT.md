# Linly-Talker 源码审计

## 结论

源码已取得，实际目录为 `E:\Guido-main\third_party\Linly-Talker-main`。这是 GitHub 源码 ZIP 解压目录，不是 Git clone，因此没有可用的 `.git/HEAD`；不能把它当成可追踪的 Git 工作树。目录包含 `api`、`Musetalk`、`TTS`、`ASR`、`LLM`、`checkpoints`、多个 WebUI 入口和 README，源码完整性满足审计要求。

目录源码文件总量约 96 MB。`checkpoints` 当前只有 README，没有实际模型权重；本轮未下载模型、未安装依赖、未启动服务。

## 已确认的 API

来源：`api/README.md`、`api/tts_api.py`、`api/talker_api.py`。

| 服务 | 默认端口 | 已确认接口 | 结果 |
|---|---:|---|---|
| TTS | 8001 | `/tts_change_model/`、`/tts_response/` | POST；音频以 FileResponse 返回 |
| LLM | 8002 | `/llm_change_model/`、`/llm_response/` | POST |
| Talker | 8003 | `/talker_change_model/`、`/talker_response/` | multipart POST；直接返回 MP4 |

Talker 支持 `SadTalker`、`Wav2Lip`、`Wav2Lipv2`、`NeRFTalk` 等模型名。`/talker_response/`需要 source_image、driven_audio 以及若干表单参数。源码使用固定临时文件名，当前不是并发安全的生产服务，也没有可确认的任务状态接口或健康接口。

## 架构边界

旅灵游客端不直接访问 8001/8003。浏览器只访问旅灵 Backend，由 Backend 的 DigitalHumanProvider 负责调用 Python 服务、保存结果并返回安全的 `audioUrl/videoUrl`。在真实适配完成前，前端保持 Preview Mode，避免伪造已连接能力。

仓库 README 提及 MuseTalk、Wav2Lip v2 和 Linly-Talker-Stream，但本地源码未提供可直接确认的 Stream/WebRTC API；不能据此猜测接口。

