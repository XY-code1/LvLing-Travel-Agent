# 灵灵 3D 资产导入检查清单

## 当前状态

正式资产路径：`E:\Guido-main\tourist-app\static\digital-human\lingling.glb`

当前检查结果：`lingling.glb` 尚未存在。现有 `test-avatar.glb` 仅作为技术 fallback，不代表正式“灵灵”角色。

| 能力 | 状态 | 现有实现 |
|---|---|---|
| 自动加载 `lingling.glb` | PASS | 默认 URL 为 `/static/digital-human/lingling.glb` |
| 缺失时优雅 fallback | PASS | 正式资产加载失败时使用测试资产，并标记为预览模式 |
| Skeleton 检测 | PASS | GLB 审计遍历 `skeleton` 并统计骨骼数 |
| Animation 检测 | PASS | 读取 `gltf.animations` 并记录动画名称 |
| Morph Target 检测 | PASS | 读取 mesh 的 `morphTargetDictionary` 并记录名称 |
| 模型自动居中 | PASS | 使用 `Box3` 计算包围盒并调整模型位置 |
| Camera 自动适配 | PASS | 根据模型高度计算镜头距离并重新 lookAt |
| Responsive resize | PASS | 使用 `ResizeObserver` 更新 renderer 与 camera aspect |

## 放置正式资产

将合法的单文件 GLB 放入：

`E:\Guido-main\tourist-app\static\digital-human\lingling.glb`

浏览器实际请求必须是：

`/static/digital-human/lingling.glb`

不能使用 Windows 绝对路径、`file://` 或本地磁盘路径。单文件 GLB 优先，避免外部纹理丢失；如果交付的是 glTF 多文件包，则必须保持其 `.gltf`、纹理和二进制文件的相对目录结构，并同步调整资源入口。

## 自动导入流程

1. H5 页面通过 `GLTFLoader` 请求 `/static/digital-human/lingling.glb`。
2. 检查 HTTP 状态、文件是否为空，以及是否能被 GLTFLoader 解析。
3. 输出模型审计信息：文件大小、Mesh 数量、三角面数、纹理数、骨骼数、动画列表和 Morph Target 列表。
4. 对面部目标执行别名匹配，兼容 `blink`/`eyeBlink`、`jawOpen`/`mouthOpen`、`smile` 等不同命名，不把核心逻辑绑定到单一建模工具。
5. 计算包围盒，将模型居中并按头部至腰部/大腿的展示目标自动调整镜头。
6. 优先播放 GLB 内置 `idle`；如果没有，则使用现有程序化轻微呼吸与头部动作。
7. 正常页面使用正式资产模式，不显示红框、测试立方体、Demo 文案或 Debug 面板。
8. 数字人状态继续由现有状态机控制：`IDLE`、`LISTENING`、`THINKING`、`SPEAKING`、`SUCCESS`、`WARNING`、`ERROR`。
9. 语音播放时由现有 Web Audio 能量分析驱动 `jawOpen`/`mouthOpen`；未来有音素时间戳时可替换为 Viseme 时间线。
10. 页面销毁或模型替换时释放 renderer、动画 mixer、音频节点、材质和纹理。

## 资产验收要求

- [ ] GLB 2.0，来源和许可证明确，可用于比赛展示
- [ ] 包含正常站立的 Humanoid Skeleton/Rig，不是 T Pose 展示图
- [ ] 包含 `idle`，最好还包含 `greeting`、`talk`、`thinking`、`pointing`
- [ ] 包含或可别名映射：`blinkLeft`、`blinkRight`、`jawOpen`、`mouthOpen`、`mouthSmile`、`mouthFunnel`、`mouthPucker`
- [ ] 最好包含 `sil`、`PP`、`FF`、`TH`、`DD`、`kk`、`CH`、`SS`、`nn`、`RR`、`aa`、`E`、`ih`、`oh`、`ou`
- [ ] 桌面版建议不超过 20 MB、约 50k–100k 三角面、以 2K 纹理为主
- [ ] Morph Target 建议约 30–60 个，避免移动端首屏过重
- [ ] 材质、纹理、骨骼和动画均能在 Three.js GLTFLoader 中解析

## 放入文件后的检查项目

- [ ] 网络请求 `/static/digital-human/lingling.glb` 返回 200
- [ ] 控制台没有 404、CORS、解析或外部纹理错误
- [ ] 审计日志显示合理的 Mesh、三角面、骨骼、动画和 Morph Target 数量
- [ ] 模型自动居中，头部和上半身构图合适，没有跑出镜头
- [ ] `idle` 或程序化待机动作正常
- [ ] 左右眨眼目标可触发
- [ ] `jawOpen`/`mouthOpen` 可控制，语音停止后自然回到闭合
- [ ] 桌面、平板和 H5 尺寸变化后模型仍可见且比例稳定
- [ ] 正式页面默认没有任何调试元素
- [ ] 如果资产缺失或解析失败，页面保持可用并显示现有 fallback，不把测试模型标记为正式灵灵



