# MuseTalk 硬件与部署计划

## 审计结论

本地 Linly-Talker 已包含 MuseTalk 代码与推理脚本，但权重目录为空。源码引用 Whisper tiny、SD VAE、MuseTalk UNet、DWPose 和人脸解析等多个模型；因此“有源码”不等于“可运行”。

RTX 4050 Laptop 6 GB 显存可以作为低分辨率、单并发实验目标，但需要逐项验证显存峰值、生成延迟和驱动/PyTorch 组合。不能承诺实时效果。6 GB 环境下不建议与本地大模型或声音克隆模型同机常驻。

## 先后顺序

1. 隔离 Python 3.10 环境。
2. 安装匹配的 PyTorch，再按 MuseTalk requirements 安装。
3. 单独准备 FFmpeg 和官方模型权重。
4. 以一张静态头像、一段短音频做离线单任务测试。
5. 再由 Backend adapter 串接，不直接让前端调用 Python 端口。

## Wav2Lip 对比

Linly-Talker 已有明确的 Wav2Lip API 路径，输入/输出更简单，适合作为第一个 MVP 验证。MuseTalk 画面潜力更高，但依赖和模型组合更多，建议在 Wav2Lip 链路打通后再做 A/B 评估。两者都必须取得对应权重后才能算真实能力。

