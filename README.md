<h1 align="center">UpRight</h1>

<p align="center">
  <strong>Real-time sitting posture monitor powered by on-device AI</strong>
</p>

<p align="center">
  Turn an old Android phone into a dedicated posture coach that watches your sitting habits and alerts you when you slouch, tilt, hunch, or crane your neck — all processing happens locally, no internet required.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/platform-Android-green" alt="Platform" />
  <img src="https://img.shields.io/badge/minSDK-26-orange" alt="Min SDK" />
  <img src="https://img.shields.io/badge/license-MIT-blue" alt="License" />
  <img src="https://img.shields.io/badge/MediaPipe-Pose_Full-red" alt="MediaPipe" />
</p>

---

<p align="center">
  English | <a href="README_CN.md">中文</a>
</p>

---

## Features

- **3D biomechanical analysis** — MediaPipe World Landmarks compute CVA (Craniovertebral Angle) and Trunk Inclination for clinical-grade forward head and hunchback detection
- **2D + 3D fusion** — Head tilt and shoulder asymmetry from 2D; forward head and hunchback from 3D world coordinates
- **Spatial refinement pipeline** — Affine rotation normalizes camera tilt; bone-length constancy optimization resolves Z-axis depth drift
- **User calibration** — 3-second baseline capture personalizes thresholds, bone ratios, and rotation matrix; persisted across sessions
- **Posture guidance arrows** — Animated directional arrows on camera preview show how to correct each type of bad posture
- **Positioning guide** — Real-time check that you're centered and at the right distance before monitoring starts
- **Sound alerts** — Synthesized bird chirp (posture corrected) and crow caw (persistent bad posture >1 min) with configurable interval (5/10/30/60 s)
- **Haptic feedback** — Vibration on posture state transitions for immediate tactile alerts
- **Session statistics** — Real-time good/bad posture duration with percentage score
- **History & streaks** — Weekly bar chart, daily 80% goal, consecutive-day streak, session list (Room database)
- **Animated posture indicator** — Color-coded ring with pulse animation for posture status at a glance
- **Sensitivity control** — Low / Medium / High (adjusts all thresholds via multiplier)
- **Multi-language** — Chinese and English, switchable in settings
- **Onboarding flow** — 3-page guide on first launch explaining purpose, setup tips, and controls
- **Settings screen** — Alert interval, sound/vibration toggles, sensitivity, language, calibration management, auto-resume timer
- **Eco mode** — Dark screen with 75% frame skipping for extended battery life
- **Background monitoring** — Foreground service with notification for continuous monitoring when minimized
- **100% offline** — No internet, no accounts, no data leaves your device

## How It Works

```
Front Camera → CameraX ImageAnalysis → MediaPipe Pose Landmarker (Full)
                                               ↓
                                    33 Keypoints (2D + 3D World)
                                               ↓
                                      1 Euro Filter (jitter-free)
                                               ↓
                                      Affine Rotation (camera tilt compensation)
                                               ↓
                                      Bone-Length Optimization (Z-axis correction)
                                               ↓
                                     Calibrated Analysis
                                     ┌─────────────────┐
                                     │ 3D: CVA < 48°?  │ → Forward Head
                                     │ 3D: Trunk > 20°? │ → Hunchback
                                     │ 2D: Ear Y diff?  │ → Head Tilt
                                     │ 2D: Shoulder Y?  │ → Slouch
                                     └─────────────────┘
                                               ↓
                                     State Machine (debounce)
                                               ↓
                                      Sound / Haptic / Guidance
```

### Detection Types

| Posture State | Detection Method | Clinical Metric |
|---------------|-----------------|-----------------|
| Head Tilt | 2D ear Y difference | Ear deviation from baseline |
| Shoulder Asymmetry | 2D shoulder Y difference | Shoulder level deviation |
| Forward Head | 3D CVA (Craniovertebral Angle) | CVA < 48° (or deviation > 10° from baseline) |
| Hunchback | 3D Trunk Inclination Angle | Trunk > 20° from vertical (or deviation > 10°) |

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin |
| UI Framework | Jetpack Compose + Material3 + Material Icons Extended |
| Camera | CameraX (Preview + ImageAnalysis) |
| Pose Detection | Google MediaPipe Pose Landmarker (Full model, GPU/CPU fallback) |
| Smoothing | 1 Euro Filter (adaptive jitter reduction) |
| Spatial Refinement | Bone-length constancy optimizer + affine rotation normalizer |
| Persistence | DataStore Preferences (settings & calibration) + Room (session history) |
| Sound Effects | Synthesized bird chirp / crow caw via AudioTrack PCM |
| Haptic | Android Vibrator (state-change alerts) |
| Multi-language | Centralized `S` data class with `StringsZh` / `StringsEn` |
| Background | Foreground Service with notification |
| Theme | Custom dark theme with branded UpRight color palette |
| Build System | Gradle Kotlin DSL |

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- Android device running 8.0+ (API 26) with a front camera
- USB debugging enabled on the device

### Build & Install

```bash
# Clone the repository
git clone https://github.com/RichardLoZC/upright.git
cd upright

# Build and install on connected device
./gradlew installDebug

# Grant camera permission (first time only)
adb shell pm grant com.example.upright android.permission.CAMERA
```

Or open the project in Android Studio, sync Gradle, and hit **Run**.

### First Launch

1. Grant camera permission when prompted
2. Follow the 3-page onboarding guide
3. Prop the phone upright in front of you (portrait orientation)
4. Ensure good lighting for best detection accuracy
5. Wait for the positioning guide to confirm you're centered
6. Tap **Calibrate** while sitting with good posture to personalize thresholds
7. The status indicator shows your current posture state in real time

## Project Structure

```
app/src/main/java/com/example/upright/
├── MainActivity.kt            # Activity, Compose UI: posture ring, skeleton overlay, session stats, screen routing
├── UpRightViewModel.kt        # ViewModel: sound, calibration, state machine, haptics, session, pause, settings
├── PoseDetector.kt            # MediaPipe PoseLandmarker (LIVE_STREAM, GPU/CPU fallback)
├── PostureLogic.kt            # Biomechanical analysis (CVA, trunk angle, head tilt, shoulder asymmetry)
│                              # CalibrationProfile, PostureStateMachine, calibrated analysis
├── OneEuroFilter.kt           # Adaptive temporal smoothing for landmark coordinates
├── SpatialRefinement.kt       # Bone-length constancy optimizer, affine rotation normalizer
├── CalibrationStore.kt        # DataStore persistence for calibration profiles
├── SettingsStore.kt           # DataStore persistence for user settings
├── AppDatabase.kt             # Room database singleton
├── SessionDao.kt              # Room DAO for session queries (daily, weekly, streak)
├── SessionEntity.kt           # Room entity for posture session data
├── Strings.kt                 # Multi-language strings (S data class, StringsZh/StringsEn)
├── OnboardingScreen.kt        # 3-page onboarding (purpose, setup tips, get started)
├── SettingsScreen.kt          # Settings page (alerts, sensitivity, language, calibration)
├── HistoryScreen.kt           # History page (weekly chart, daily goal, streak, session list)
├── PostureGuidance.kt         # Animated directional arrows for posture correction
├── PostureDiagnosis.kt        # Data class for analysis results
├── SoundEffects.kt            # Synthesized bird chirp and crow caw via AudioTrack PCM
├── PostureMonitorService.kt   # Foreground service for background monitoring
└── ui/theme/Theme.kt          # Compose Material3 theme

app/src/main/assets/
└── pose_landmarker_full.task   # MediaPipe Full model (~9 MB)

app/src/test/java/com/example/upright/
└── PostureLogicTest.kt        # Unit tests (2D/3D analysis, state machine, filter, spatial refinement)
```

## Configuration

### Detection Thresholds

| Parameter | Default | Description |
|-----------|---------|-------------|
| `TILT_THRESHOLD` | 0.05 | Ear Y-difference for head tilt (absolute mode) |
| `SHOULDER_LEVEL_THRESHOLD` | 0.04 | Shoulder Y-difference for slouch (absolute mode) |
| `CVA_THRESHOLD_DEG` | 48° | CVA below this = forward head |
| `TRUNK_INCLINATION_THRESHOLD_DEG` | 20° | Trunk angle above this = hunchback |
| `TILT_DEVIATION` | 0.03 | Ear deviation from calibrated baseline |
| `SHOULDER_DEVIATION` | 0.03 | Shoulder deviation from calibrated baseline |
| `CVA_DEVIATION_DEG` | 10° | CVA deviation from calibrated baseline |
| `TRUNK_DEVIATION_DEG` | 10° | Trunk angle deviation from calibrated baseline |

### Sensitivity Levels

| Level | Multiplier | Effect |
|-------|-----------|--------|
| Low | 1.5× | Harder to trigger, fewer alerts |
| Medium | 1.0× | Default balance |
| High | 0.7× | Easier to trigger, more alerts |

### State Machine

Debounce mechanism to prevent rapid state flickering:
- **3 consecutive bad frames** required before triggering an alert
- **5 consecutive good frames** required before clearing an alert

### Eco Mode

Skips 3 out of 4 frames, lowering analysis from ~16 FPS to ~4 FPS while maintaining detection reliability.

## Tested On

| Platform | Device | OS | Result |
|----------|--------|----|--------|
| Physical Device | Xiaomi 14 Pro | Android 16 | Full pass |
| Pose Detection | MediaPipe Pose Landmarker Full (GPU) | — | ~16 FPS |
| Landscape (Left/Right) | — | — | Verified |
| Multi-language (EN/ZH) | — | — | Verified |

Unit tests: `./gradlew testDebugUnitTest` — all pass.

## Roadmap

- [x] Forward head (turtle neck) detection via CVA
- [x] Hunchback detection via trunk inclination
- [x] User calibration flow for personalized baselines
- [x] Skeleton overlay visualization
- [x] 3D biomechanical analysis with World Landmarks
- [x] State machine debounce for stable detection
- [x] MVVM architecture with ViewModel + StateFlow
- [x] Background service for continuous monitoring
- [x] Physics-informed bone-length optimization
- [x] Affine coordinate normalization for camera tilt
- [x] Calibration persistence across sessions
- [x] Unit tests for core logic
- [x] Session tracking with statistics and history
- [x] Haptic feedback
- [x] Multi-language support (Chinese / English)
- [x] Onboarding flow for new users
- [x] Settings screen (alert interval, sensitivity, language)
- [x] Posture guidance arrows
- [x] Positioning guide
- [x] History with weekly chart, daily goal, and streaks
- [ ] Wear OS companion app

## Troubleshooting

| Issue | Solution |
|-------|----------|
| App crashes on start | Grant camera permission in Settings → Apps → UpRight |
| 3D shows OFF | Ensure good front-facing lighting; hips/shoulders need visibility > 0.3 |
| No sound alerts | Check media volume is turned up |
| High battery drain | Enable Eco mode to reduce display and processing load |
| Frequent false alerts | Run calibration while sitting with good posture |
| Detection too sensitive | Lower sensitivity in Settings (or increase deviation thresholds in code) |

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/my-feature`)
3. Commit your changes (`git commit -m 'Add my feature'`)
4. Push to the branch (`git push origin feature/my-feature`)
5. Open a Pull Request

## Acknowledgments

### Algorithms & Methods

This project implements biomechanical posture analysis methods from the following research and frameworks:

- **Google MediaPipe Pose Landmarker** — BlazePose architecture providing 33-keypoint 3D skeleton estimation with World Landmarks. The "Full" complexity model is used for its balance of speed and Z-axis depth accuracy.
  - Bazarevsky et al., "BlazePose: On-device Real-time Body Pose Tracking", CVPR Workshop on Computer Vision for Augmented and Virtual Reality, 2020
- **Craniovertebral Angle (CVA)** — Clinical biomechanical metric for quantifying Forward Head Posture. CVA is defined as the angle between a horizontal line through C7 and the line from C7 to the tragus of the ear. A CVA below 50° is clinically indicative of forward head translation.
  - Ref: Silva et al., "Craniovertebral angle and forward head posture: a systematic review", *Fisioterapia em Movimento*, 2024
- **Trunk Inclination Angle** — 3D vector angle between the spine (pelvis→C7) and the vertical axis, used to quantify thoracic kyphosis and slouching.
- **1 Euro Filter** — Adaptive low-pass filter for real-time signal processing that eliminates jitter during static poses while maintaining zero-lag response during movement.
  - Casiez et al., "1€ Filter: A Simple Speed-based Low-pass Filter for Noisy Input in Interactive Systems", ACM CHI, 2012
- **Physics-Informed Bone-Length Optimization** — Post-processing pipeline that enforces skeletal rigidity constraints to resolve monocular depth ambiguities. Calibrated bone ratios penalize Z-axis stretching across frames, reducing 3D MPJPE by ~10%.
- **Affine Coordinate Normalization** — Camera-tilt compensation via rotation matrix derived from user calibration, establishing a personalized vertical reference using Rodrigues' rotation formula.

The algorithm methodology is further detailed in [`algorithm/Advanced-Methodologies-for-On-Device-3D-Human-Posture-Estimation-and-Biomechanic.md`](algorithm/Advanced-Methodologies-for-On-Device-3D-Human-Posture-Estimation-and-Biomechanic.md).

### Open Source Libraries

- [Google MediaPipe](https://developers.google.com/mediapipe) — On-device ML pose estimation
- [Android CameraX](https://developer.android.com/training/camerax) — Camera API abstraction
- [Jetpack Compose](https://developer.android.com/jetpack/compose) — Declarative UI toolkit
- [Room](https://developer.android.com/training/data-storage/room) — Local database for session history
- [DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore) — Key-value persistence for settings and calibration

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
