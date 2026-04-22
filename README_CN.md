<h1 align="center">PostureGuard 坐姿卫士</h1>

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
- **用户校准** — 3 秒采集个人基准姿势，个性化设定检测阈值
- **骨骼叠加** — 调试模式下在摄像头画面上实时显示检测到的关节点和骨骼连线
- **语音提醒** — 中文 TTS 语音提醒纠正坐姿（5 秒冷却，避免频繁打扰）
- **省电模式** — 黑屏 + 跳过 75% 帧率，大幅延长续航
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
                                  TTS 语音提醒 (异常时)
```

### 检测类型

| 坐姿状态 | 检测方式 | 检测指标 |
|----------|---------|---------|
| 头部左倾/右倾 | 2D 耳朵 Y 坐标差 | 耳朵偏离基准线 |
| 肩膀不平 | 2D 肩膀 Y 坐标差 | 肩膀水平偏差 |
| 探颈（龟壳颈） | 3D CVA 颅椎角 | CVA < 48°（或偏离基准 > 10°） |
| 驼背 | 3D 躯干倾角 | 躯干角 > 20°（或偏离基准 > 10°） |

## 技术栈

| 组件 | 技术 |
|------|------|
| 语言 | Kotlin |
| UI 框架 | Jetpack Compose + Material3 |
| 相机 | CameraX (Preview + ImageAnalysis) |
| 姿态检测 | Google MediaPipe Pose Landmarker (Full 模型, GPU/CPU 自动切换) |
| 信号平滑 | 1 Euro Filter (自适应去抖) |
| 语音输出 | Android TextToSpeech (中文) |
| 构建系统 | Gradle Kotlin DSL |

## 快速开始

### 环境要求

- Android Studio Hedgehog (2023.1.1) 或更新版本
- Android 8.0+ (API 26) 设备，带前置摄像头
- 设备已开启 USB 调试

### 构建安装

```bash
# 克隆仓库
git clone https://github.com/RichardLoZC/posture_detect.git
cd posture_detect

# 构建并安装到设备
./gradlew installDebug

# 授予相机权限（首次安装）
adb shell pm grant com.example.postureguard android.permission.CAMERA
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
app/src/main/java/com/example/postureguard/
├── MainActivity.kt        # 主界面、TTS、相机 UI、校准流程
├── PoseDetector.kt        # MediaPipe 姿态检测封装 (实时流, GPU/CPU)
├── PostureLogic.kt        # 生物力学分析、校准、状态机
├── OneEuroFilter.kt       # 关键点自适应时序平滑滤波
├── PostureDiagnosis.kt    # 诊断结果数据类
└── ui/theme/Theme.kl      # Material3 主题

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

## 开发路线

- [x] 探颈检测（CVA 颅椎角）
- [x] 驼背检测（躯干倾角）
- [x] 用户校准流程
- [x] 骨骼叠加可视化
- [x] 3D 生物力学分析（World Landmarks）
- [x] 状态机防抖
- [ ] 会话记录与历史统计
- [ ] MVVM 架构（ViewModel + Room）
- [ ] 振动反馈
- [ ] 后台保活服务
- [ ] Wear OS 配套应用

## 常见问题

| 问题 | 解决方案 |
|------|---------|
| 启动闪退 | 在 设置 → 应用 → PostureGuard 中授予相机权限 |
| 3D 显示 OFF | 确保正面光线充足；肩膀/髋部可见度需 > 0.3 |
| 语音无声音 | 检查是否安装了 Google TTS 引擎，并调大音量 |
| 耗电太快 | 开启省电模式，降低屏幕和处理负载 |
| 频繁误报 | 保持正确坐姿后点击"校准"重新采集基准 |
| 检测过于敏感 | 在 `PostureLogic.kt` 中调大偏差阈值 |

## 贡献

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/my-feature`)
3. 提交更改 (`git commit -m 'Add my feature'`)
4. 推送分支 (`git push origin feature/my-feature`)
5. 发起 Pull Request

## 许可证

本项目采用 MIT 许可证 — 详见 [LICENSE](LICENSE) 文件。
