<h1 align="center">UpRight 坐姿卫士</h1>

<p align="center">
  <strong>基于端侧 AI 的实时坐姿监测应用</strong>
</p>

<p align="center">
  把旧安卓手机变成专属坐姿教练，实时监测你的坐姿——歪头、高低肩、驼背、探颈，统统不放过。全部计算在本地完成，无需联网。
</p>

<p align="center">
  <img src="https://img.shields.io/badge/platform-Android-green" alt="Platform" />
  <img src="https://img.shields.io/badge/minSDK-26-orange" alt="Min SDK" />
  <img src="https://img.shields.io/badge/license-MIT-blue" alt="License" />
  <img src="https://img.shields.io/badge/MediaPipe-Pose_Full-red" alt="MediaPipe" />
</p>

<p align="center">
  <a href="README.md">English</a> | 中文
</p>

---

## 功能特性

- **3D 生物力学分析** — 利用 MediaPipe World Landmarks 计算 CVA（颅椎角）和躯干倾角，实现临床级的探颈和驼背检测
- **2D + 3D 融合检测** — 2D 归一化坐标检测歪头和肩膀不对称；3D 世界坐标检测探颈和驼背
- **用户校准** — 3 秒采集个人基准姿势，个性化设定检测阈值，校准数据跨会话持久保存
- **骨骼叠加** — 调试模式下在摄像头画面上实时显示检测到的关节点和骨骼连线
- **音效提醒** — 合成鸟鸣（姿势纠正时）和乌鸦叫（不良姿势持续 >1 分钟），可配置提醒间隔
- **振动反馈** — 姿势状态变化时振动提醒，即时触觉感知
- **会话统计** — 实时追踪良好/不良坐姿时长，显示百分比得分
- **动画姿态指示器** — 带脉冲动画的彩色圆环，一目了然显示坐姿状态
- **省电模式** — 暗屏 + 跳过 75% 帧率，大幅延长续航
- **完全离线** — 无需网络、无需注册、数据不离开设备

## 工作原理

```
前置摄像头 → CameraX 图像分析 → MediaPipe 姿态检测 (Full 模型)
                                         ↓
                               33 个关键点 (2D + 3D 世界坐标)
                                         ↓
                                    1 Euro 滤波 (去抖动)
                                         ↓
                                    校准分析引擎
                                    ┌──────────────────┐
                                    │ 3D: CVA < 48°?   │ → 探颈
                                    │ 3D: 躯干角 > 20°? │ → 驼背
                                    │ 2D: 耳朵 Y 偏差?  │ → 歪头
                                    │ 2D: 肩膀 Y 偏差?  │ → 高低肩
                                    └──────────────────┘
                                         ↓
                                    状态机 (防抖)
                                         ↓
                                  音效提醒 (不良持续 >1 分钟)
```

### 检测类型

| 坐姿状态 | 检测方式 | 检测指标 |
|----------|---------|---------|
| 头部歪斜 | 2D 耳朵 Y 坐标差 | 耳朵偏离基准线 |
| 肩膀不平 | 2D 肩膀 Y 坐标差 | 肩膀水平偏差 |
| 探颈（龟壳颈） | 3D CVA 颅椎角 | CVA < 48°（或偏离基准 > 10°） |
| 驼背 | 3D 躯干倾角 | 躯干角 > 20°（或偏离基准 > 10°） |

## 技术栈

| 组件 | 技术 |
|------|------|
| 语言 | Kotlin |
| UI 框架 | Jetpack Compose + Material3 + Material Icons |
| 相机 | CameraX (Preview + ImageAnalysis) |
| 姿态检测 | Google MediaPipe Pose Landmarker (Full 模型, GPU/CPU 自动切换) |
| 信号平滑 | 1 Euro Filter (自适应去抖) |
| 音效输出 | 合成鸟鸣 / 乌鸦叫声 (AudioTrack PCM) |
| 振动 | Android Vibrator (状态变化提醒) |
| 主题 | 自定义暗色主题，UpRight 品牌配色 |
| 构建系统 | Gradle Kotlin DSL |

## 快速开始

### 环境要求

- Android Studio Hedgehog (2023.1.1) 或更新版本
- Android 8.0+ (API 26) 设备，带前置摄像头
- 设备已开启 USB 调试

### 构建安装

```bash
# 克隆仓库
git clone https://github.com/RichardLoZC/upright.git
cd upright

# 构建并安装到设备
./gradlew installDebug

# 授予相机权限（首次安装）
adb shell pm grant com.example.upright android.permission.CAMERA
```

也可以在 Android Studio 中打开项目，同步 Gradle 后点击 **Run**。

### 首次使用

1. 允许相机权限
2. 将手机竖放在面前（竖屏方向）
3. 确保光线充足，以获得最佳检测精度
4. 保持正确坐姿，点击 **校准** 按钮进行 3 秒基准采集
5. 状态指示器会实时显示当前坐姿状态

## 项目结构

```
app/src/main/java/com/example/upright/
├── MainActivity.kt            # Compose UI，含动画姿态环、骨骼叠加、会话统计
├── UpRightViewModel.kt   # ViewModel: 音效、校准、状态机、振动、会话追踪
├── PoseDetector.kt            # MediaPipe 姿态检测封装 (实时流, GPU/CPU)
├── PostureLogic.kt            # 生物力学分析、校准、状态机
├── OneEuroFilter.kt           # 关键点自适应时序平滑滤波
├── SpatialRefinement.kt       # 骨长约束优化、仿射旋转变换
├── CalibrationStore.kt        # DataStore 校准数据持久化
├── PostureMonitorService.kt   # 前台服务（后台持续监测）
├── PostureDiagnosis.kt        # 诊断结果数据类
└── ui/theme/Theme.kl          # Material3 主题

app/src/main/assets/
└── pose_landmarker_full.task   # MediaPipe Full 模型 (~9 MB)
```

## 配置参数

### 检测阈值

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `TILT_THRESHOLD` | 0.05 | 耳朵 Y 坐标差判定歪头（绝对模式） |
| `SHOULDER_LEVEL_THRESHOLD` | 0.04 | 肩膀 Y 坐标差判定高低肩（绝对模式） |
| `CVA_THRESHOLD_DEG` | 48° | 颅椎角低于此值判定为探颈 |
| `TRUNK_INCLINATION_THRESHOLD_DEG` | 20° | 躯干倾角高于此值判定为驼背 |
| `TILT_DEVIATION` | 0.03 | 耳朵偏离校准基准 |
| `SHOULDER_DEVIATION` | 0.03 | 肩膀偏离校准基准 |
| `CVA_DEVIATION_DEG` | 10° | CVA 偏离校准基准 |
| `TRUNK_DEVIATION_DEG` | 10° | 躯干角偏离校准基准 |

### 状态机

应用使用防抖机制避免状态频繁跳变：
- 连续 **3 帧** 检测到异常才触发提醒
- 连续 **5 帧** 检测到正常才解除提醒

### 省电模式

跳过 3/4 的帧，将分析帧率从约 16 FPS 降至约 4 FPS，在保持检测可靠性的同时显著降低功耗。

## 测试平台

| 平台 | 设备 | 系统 | 结果 |
|------|------|------|------|
| 真机 | Xiaomi 14 Pro | Android 16 | 全部通过 |
| 姿态检测 | MediaPipe Pose Landmarker Full (GPU) | — | ~16 FPS |
| 横屏（左/右） | — | — | 已验证 |
| 多语言（中/英） | — | — | 已验证 |

单元测试：`./gradlew testDebugUnitTest` — 全部通过。

## 开发路线

- [x] 探颈检测（CVA 颅椎角）
- [x] 驼背检测（躯干倾角）
- [x] 用户校准流程
- [x] 骨骼叠加可视化
- [x] 3D 生物力学分析（World Landmarks）
- [x] 状态机防抖
- [x] MVVM 架构（ViewModel + DataStore）
- [x] 后台保活前台服务
- [x] 物理约束骨长优化
- [x] 仿射坐标归一化（相机倾斜补偿）
- [x] 校准数据跨会话持久化
- [x] 核心逻辑单元测试
- [x] 会话记录与历史统计
- [x] 振动反馈
- [ ] Wear OS 配套应用

## 常见问题

| 问题 | 解决方案 |
|------|---------|
| 启动闪退 | 在 设置 → 应用 → UpRight 中授予相机权限 |
| 3D 显示 OFF | 确保正面光线充足；肩膀/髋部可见度需 > 0.3 |
| 无提示音 | 检查媒体音量是否已调大 |
| 耗电太快 | 开启省电模式，降低屏幕和处理负载 |
| 频繁误报 | 保持正确坐姿后点击"校准"重新采集基准 |
| 检测过于敏感 | 在 `PostureLogic.kt` 中调大偏差阈值 |

## 贡献

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/my-feature`)
3. 提交更改 (`git commit -m 'Add my feature'`)
4. 推送分支 (`git push origin feature/my-feature`)
5. 发起 Pull Request

## 鸣谢

### 算法与方法论

本项目实现了以下研究和框架中的生物力学姿势分析方法：

- **Google MediaPipe Pose Landmarker** — BlazePose 架构，提供 33 关键点的 3D 骨骼估计及世界坐标。使用 Full 复杂度模型，兼顾速度与 Z 轴深度精度。
  - Bazarevsky et al., "BlazePose: On-device Real-time Body Pose Tracking", CVPR Workshop on Computer Vision for Augmented and Virtual Reality, 2020
- **颅椎角 (Craniovertebral Angle, CVA)** — 量化前倾头姿势的临床生物力学指标。CVA 定义为通过 C7 的水平线与 C7 到耳屏连线的夹角。CVA 低于 50° 临床提示头部前移。
  - Ref: Silva et al., "Craniovertebral angle and forward head posture: a systematic review", *Fisioterapia em Movimento*, 2024
- **躯干倾角 (Trunk Inclination Angle)** — 脊柱向量（骨盆→C7）与垂直轴的 3D 夹角，用于量化胸椎后凸和驼背。
- **1 Euro Filter** — 自适应低通滤波器，在静态姿势时消除抖动，在姿势变化时零延迟响应。
  - Casiez et al., "1€ Filter: A Simple Speed-based Low-pass Filter for Noisy Input in Interactive Systems", ACM CHI, 2012
- **物理约束骨长优化** — 后处理管线，强制骨骼刚性约束以消除单目深度模糊。校准后的骨骼比例惩罚帧间 Z 轴拉伸，降低 3D MPJPE 约 10%。
- **仿射坐标归一化** — 通过用户校准导出的旋转矩阵补偿相机倾斜，建立个性化垂直参考。

算法方法论的详细说明见 [`algorithm/Advanced-Methodologies-for-On-Device-3D-Human-Posture-Estimation-and-Biomechanic.md`](algorithm/Advanced-Methodologies-for-On-Device-3D-Human-Posture-Estimation-and-Biomechanic.md)。

### 开源库

- [Google MediaPipe](https://developers.google.com/mediapipe) — 端侧 ML 姿态估计
- [Android CameraX](https://developer.android.com/training/camerax) — 相机 API 抽象层
- [Jetpack Compose](https://developer.android.com/jetpack/compose) — 声明式 UI 工具包

## 许可证

本项目采用 MIT 许可证 — 详见 [LICENSE](LICENSE) 文件。
