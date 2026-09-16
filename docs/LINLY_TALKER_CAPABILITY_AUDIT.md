# Linly-Talker 能力审计

## 源码状态

官方仓库为 `https://github.com/Kedreamix/Linly-Talker`。本机已取得源码 ZIP，实际目录为 `third_party/Linly-Talker-main`。目录包含 ASR、TTS、GPT-SoVITS、CosyVoice、MuseTalk、Wav2Lip、SadTalker、API 和 WebUI；`checkpoints` 当前没有实际权重。README 提到 Stream 方向，但本地未确认可直接调用的 Stream/WebRTC API。

## 初步分类

- 可直接确认：项目包含 WebUI、API 目录和多种模块入口。
- 需要模型：MuseTalk、Wav2Lip、SadTalker、CosyVoice、GPT-SoVITS、Whisper/FunASR。
- 需要外部配置：部分 LLM、Microsoft TTS 或在线服务。
- 暂不采用：Linly-Talker 自带 LLM 对话链路，旅灵继续使用自己的 Travel Agent。
- 已源码确认：TTS 8001、LLM 8002、Talker 8003；Talker multipart 输入并同步返回 MP4。未确认健康检查、任务状态或流式协议。

## 当前机器匹配

RTX 4050 Laptop 6GB、CUDA 12.7，适合先做低分辨率、单任务的离线准实时验证；不适合同时运行大型 LLM、CosyVoice/GPT-SoVITS 和高分辨率视频链路。当前没有 Conda 和 FFmpeg，C 盘约 57.5GB，不满足大模型部署的安全空间余量。

## 推荐

第一版优先用 Wav2Lip 验证端到端 MVP，因为源码已有明确 Talker API；MuseTalk 作为低分辨率单任务实验。TTS 优先复用旅灵已有 TTS；Linly-Talker EdgeTTS 仅作在线实验，声音克隆放后续。Python 3.10 + 隔离环境 + FFmpeg 是后续部署前置条件，本轮不安装。
