# 灵灵 3D 角色制作规范

## 定位

灵灵是“旅灵 AI 旅行数字员工”，视觉方向为现代新中式、高级旅行顾问与克制的未来 AI 感。禁止古装、武侠、仙侠、机器人和二次元游戏角色表达。

## 文件与预算

- 主文件：`lingling.glb`，glTF 2.0，优先单文件内嵌纹理。
- 路径：`tourist-app/static/digital-human/lingling.glb`。
- Desktop 目标：20MB 以内、5 万～10 万三角面、2K 纹理。
- Mobile 版本：`lingling-mobile.glb`，10MB 以内、1K～2K 纹理、1.5 万～3.5 万三角面。
- 使用 Draco 几何压缩；纹理可使用 KTX2/Basis。

## 骨骼与动画

- 需要完整 Humanoid Rig；建议 60～120 根骨骼，移动版 40～80 根。
- 动画名称：`idle`、`greeting`、`talk`、`thinking`、`pointing`。
- 默认姿态为自然站立、双手交叠或礼宾姿态，不使用 T-Pose 作为运行姿态。

## 面部 Morph Target

必须包含并保持名称稳定：

`blinkLeft`、`blinkRight`、`jawOpen`、`mouthOpen`、`mouthSmile`、`mouthFunnel`、`mouthPucker`。

建议包含：

`sil`、`PP`、`FF`、`TH`、`DD`、`kk`、`CH`、`SS`、`nn`、`RR`、`aa`、`E`、`ih`、`oh`、`ou`、`browUp`、`browDown`、`eyeLookLeft`、`eyeLookRight`、`eyeLookUp`、`eyeLookDown`。

建议总 Morph Target 数量 30～60，移动版 20～40。所有目标必须与基础网格保持相同拓扑。

## 材质与造型

- 半写实东方女性，视觉年龄约 23～28 岁。
- 深黑或深棕自然长发、低马尾或简洁盘发。
- 象牙白与浅青绿色现代新中式旅行制服，少量金色点缀。
- 可有小型“旅灵/灵灵”铭牌、耳侧装置或衣领微光。
- 禁止大型发冠、战甲、全身发光线路和过度磨皮。

## 导出检查

1. 使用 Three.js `GLTFLoader` 能加载。
2. `scene.traverse` 能找到 SkinnedMesh、Skeleton、AnimationClip 和 Morph Target。
3. Morph Target 名称能被 alias 映射到左右眨眼、嘴巴开合和微笑。
4. 所有纹理路径在 GLB 内可解析，不依赖本机绝对路径。
5. 使用 glTF Validator 检查无阻塞错误。
6. 在桌面和移动浏览器分别验证 45～60 FPS 与 30 FPS 目标。
