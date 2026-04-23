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
- **空间优化管线** — 仿射旋转变换补偿相机倾斜；骨长约束优化修正 Z 轴深度漂移
- **用户校准** — 3 秒采集个人基准姿势，个性化设定检测阈值、骨骼比例和旋转矩阵，校准数据跨会话持久保存
- **姿势引导箭头** — 在摄像头画面上显示动态方向箭头，指引纠正不良姿势
- **定位引导** — 实时检测用户是否居中且距离合适，确认后才启动监测
- **音效提醒** — 合成鸟鸣（姿势纠正时）和乌鸦叫（不良姿势持续 >1 分钟），可配置提醒间隔（5/10/30/60 秒）
- **振动反馈** — 姿势状态变化时振动提醒，即时触觉感知
- **会话统计** — 实时追踪良好/不良坐姿时长，显示百分比得分
- **历史记录与连续天数** — 周图表、每日 80% 目标、连续达标天数、会话列表（Room 数据库）
- **动画姿态指示器** — 带脉冲动画的彩色圆环，一目了然显示坐姿状态
- **灵敏度调节** — 低 / 中 / 高 三档（通过乘数调节全部检测阈值）
- **多语言支持** — 中文和英文，在设置中切换
- **新手引导** — 首次启动时 3 页引导，介绍用途、摆放技巧和操作方法
- **设置页面** — 提醒间隔、声音/振动开关、灵敏度、语言、校准管理、自动恢复计时器
- **省电模式** — 暗屏 + 跳过 75% 帧率，大幅延长续航
- **后台监测** — 前台服务 + 通知，应用最小化后持续运行
- **完全离线** — 无需网络、无需注册、数据不离开设备

## 工作原理

```
前置摄像头 → CameraX 图像分析 → MediaPipe 姿态检测 (Full 模型)
                                         ↓
                               33 个关键点 (2D + 3D 世界坐标)
                                         ↓
                                    1 Euro 滤波 (去抖动)
                                         ↓
                                    仿射旋转变换 (相机倾斜补偿)
                                         ↓
                                    骨长约束优化 (Z 轴修正)
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
                                  声音 / 振动 / 引导箭头
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
| UI 框架 | Jetpack Compose + Material3 + Material Icons Extended |
| 相机 | CameraX (Preview + ImageAnalysis) |
| 姿态检测 | Google MediaPipe Pose Landmarker (Full 模型, GPU/CPU 自动切换) |
| 信号平滑 | 1 Euro Filter (自适应去抖) |
| 空间优化 | 骨长约束优化器 + 仿射旋转变换归一化 |
| 数据持久化 | DataStore Preferences（设置与校准）+ Room（会话历史） |
| 音效输出 | 合成鸟鸣 / 乌鸦叫声 (AudioTrack PCM) |
| 振动 | Android Vibrator (状态变化提醒) |
| 多语言 | 中央化 `S` 数据类，含 `StringsZh` / `StringsEn` |
| 后台运行 | 前台服务 + 通知 |
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
2. 跟随 3 页新手引导了解操作方式
3. 将手机竖放在面前（竖屏方向）
4. 确保光线充足，以获得最佳检测精度
5. 等待定位引导确认你已居中
6. 保持正确坐姿，点击 **校准** 按钮进行 3 秒基准采集
7. 状态指示器会实时显示当前坐姿状态

## 项目结构

```
app/src/main/java/com/example/upright/
├── MainActivity.kt            # Activity，Compose UI：姿态环、骨骼叠加、会话统计、屏幕路由
├── UpRightViewModel.kt        # ViewModel：音效、校准、状态机、振动、会话、暂停、设置
├── PoseDetector.kt            # MediaPipe 姿态检测封装 (实时流, GPU/CPU 自动切换)
├── PostureLogic.kt            # 生物力学分析 (CVA, 躯干角, 歪头, 高低肩)
│                              # CalibrationProfile, PostureStateMachine, 校准分析
├── OneEuroFilter.kt           # 关键点自适应时序平滑滤波
├── SpatialRefinement.kt       # 骨长约束优化器, 仿射旋转变换归一化
├── CalibrationStore.kt        # DataStore 校准数据持久化
├── SettingsStore.kt           # DataStore 设置数据持久化
├── AppDatabase.kt             # Room 数据库单例
├── SessionDao.kt              # Room DAO (日/周/连续天数查询)
├── SessionEntity.kt           # Room 会话数据实体
├── Strings.kt                 # 多语言字符串 (S 数据类, StringsZh/StringsEn)
├── OnboardingScreen.kt        # 3 页新手引导 (用途, 摆放技巧, 开始使用)
├── SettingsScreen.kt          # 设置页 (提醒, 灵敏度, 语言, 校准管理)
├── HistoryScreen.kt           # 历史页 (周图表, 每日目标, 连续天数, 会话列表)
├── PostureGuidance.kt         # 姿势纠正动态方向箭头
├── PostureDiagnosis.kt        # 诊断结果数据类
├── SoundEffects.kt            # 合成鸟鸣和乌鸦叫声 (AudioTrack PCM)
├── PostureMonitorService.kt   # 前台服务（后台持续监测）
└── ui/theme/Theme.kt          # Material3 主题

app/src/main/assets/
└── pose_landmarker_full.task   # MediaPipe Full 模型 (~9 MB)

app/src/test/java/com/example/upright/
└── PostureLogicTest.kt        # 单元测试 (2D/3D 分析, 状态机, 滤波, 空间优化)
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

### 灵敏度档位

| 档位 | 乘数 | 效果 |
|------|------|------|
| 低 | 1.5× | 更难触发，提醒更少 |
| 中 | 1.0× | 默认平衡 |
| 高 | 0.7× | 更易触发，提醒更多 |

### 状态机

防抖机制避免状态频繁跳变：
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
- [x] MVVM 架构（ViewModel + StateFlow）
- [x] 后台保活前台服务
- [x] 物理约束骨长优化
- [x] 仿射坐标归一化（相机倾斜补偿）
- [x] 校准数据跨会话持久化
- [x] 核心逻辑单元测试
- [x] 会话记录与历史统计
- [x] 振动反馈
- [x] 多语言支持（中文 / 英文）
- [x] 新手引导流程
- [x] 设置页面（提醒间隔、灵敏度、语言）
- [x] 姿势引导箭头
- [x] 定位引导
- [x] 历史周图表、每日目标、连续天数
- [ ] Wear OS 配套应用

## 常见问题

| 问题 | 解决方案 |
|------|---------|
| 启动闪退 | 在 设置 → 应用 → UpRight 中授予相机权限 |
| 3D 显示 OFF | 确保正面光线充足；肩膀/髋部可见度需 > 0.3 |
| 无提示音 | 检查媒体音量是否已调大 |
| 耗电太快 | 开启省电模式，降低屏幕和处理负载 |
| 频繁误报 | 保持正确坐姿后点击"校准"重新采集基准 |
| 检测过于敏感 | 在设置中降低灵敏度（或在代码中调大偏差阈值） |

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
- **仿射坐标归一化** — 通过用户校准导出的旋转矩阵补偿相机倾斜，利用 Rodrigues 旋转公式建立个性化垂直参考。

算法方法论的详细说明见 [`algorithm/Advanced-Methodologies-for-On-Device-3D-Human-Posture-Estimation-and-Biomechanic.md`](algorithm/Advanced-Methodologies-for-On-Device-3D-Human-Posture-Estimation-and-Biomechanic.md)。

### 开源库

- [Google MediaPipe](https://developers.google.com/mediapipe) — 端侧 ML 姿态估计
- [Android CameraX](https://developer.android.com/training/camerax) — 相机 API 抽象层
- [Jetpack Compose](https://developer.android.com/jetpack/compose) — 声明式 UI 工具包
- [Room](https://developer.android.com/training/data-storage/room) — 本地数据库，用于会话历史
- [DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore) — 键值持久化，用于设置和校准数据

## 许可证

本项目采用 MIT 许可证 — 详见 [LICENSE](LICENSE) 文件。
