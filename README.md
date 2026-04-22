<h1 align="center">PostureGuard</h1>

<p align="center">
  <strong>Real-time sitting posture monitor powered by on-device AI</strong>
</p>

<p align="center">
  Turn an old Android phone into a dedicated posture coach that watches your sitting habits and alerts you when you slouch, tilt, or hunch — all processing happens locally, no internet required.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/platform-Android-green" alt="Platform" />
  <img src="https://img.shields.io/badge/minSDK-26-orange" alt="Min SDK" />
  <img src="https://img.shields.io/badge/license-MIT-blue" alt="License" />
  <img src="https://img.shields.io/badge/MediaPipe-Pose_Landmarker-red" alt="MediaPipe" />
</p>

---

## Features

- **Real-time pose detection** — MediaPipe Pose Landmarker (Lite) identifies 33 body keypoints at camera framerate
- **Posture analysis** — Detects head tilt (left/right), shoulder asymmetry, and slouching
- **Voice alerts** — Chinese TTS reminds you to sit up straight when bad posture is sustained
- **Eco mode** — Black screen mode saves battery while monitoring continues in the background
- **100% offline** — No internet, no accounts, no data leaves your device

## How It Works

```
Front Camera → CameraX ImageAnalysis → MediaPipe Pose Landmarker
                                               ↓
                                       33 Keypoints
                                               ↓
                                      PostureLogic Analysis
                                     (head tilt + shoulder level)
                                               ↓
                                    PostureState Enum
                                  (GOOD / TILT / SLOUCH)
                                               ↓
                                    TTS Voice Alert (if bad)
```

The app uses a subset of MediaPipe's 33 landmarks:

| Keypoint      | Index | Used For            |
|---------------|-------|---------------------|
| Nose          | 0     | Reference point     |
| Left Ear      | 7     | Head tilt detection |
| Right Ear     | 8     | Head tilt detection |
| Left Shoulder | 11    | Shoulder symmetry   |
| Right Shoulder| 12    | Shoulder symmetry   |

## Tech Stack

| Component         | Technology                          |
|-------------------|-------------------------------------|
| Language          | Kotlin                              |
| UI Framework      | Jetpack Compose + Material3         |
| Camera            | CameraX (Preview + ImageAnalysis)   |
| Pose Detection    | Google MediaPipe Pose Landmarker    |
| Voice Output      | Android TextToSpeech                |
| Build System      | Gradle Kotlin DSL                   |

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- Android device running 8.0+ (API 26) with a front camera
- USB debugging enabled on the device

### Build & Install

```bash
# Clone the repository
git clone https://github.com/RichardLoZC/posture_detect.git
cd posture_detect

# Build and install on connected device
./gradlew installDebug
```

Or open the project in Android Studio, sync Gradle, and hit **Run**.

### First Launch

1. Grant camera permission when prompted
2. Prop the phone upright in front of you (portrait orientation)
3. Ensure good lighting for best detection accuracy
4. The status indicator at the bottom shows your current posture state

## Project Structure

```
posture_detect/
├── app/
│   ├── build.gradle.kts              # App-level dependencies and config
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/
│       │   └── pose_landmarker_lite.task   # MediaPipe model (~5.8 MB)
│       ├── java/com/example/postureguard/
│       │   ├── MainActivity.kt       # Entry point, camera UI, TTS
│       │   ├── PoseDetector.kt       # MediaPipe wrapper
│       │   ├── PostureLogic.kt       # Posture classification rules
│       │   └── ui/theme/Theme.kt     # Material3 theme
│       └── res/
├── build.gradle.kts                  # Root build config
├── settings.gradle.kts
├── CLAUDE.md                         # AI development context
└── README.md
```

## Configuration

### Detection Thresholds

Thresholds are defined in `PostureLogic.kt` and can be tuned:

| Parameter              | Default | Description                         |
|------------------------|---------|-------------------------------------|
| `TILT_THRESHOLD`       | 0.05    | Ear Y-difference for head tilt (5%) |
| `SHOULDER_LEVEL_THRESHOLD` | 0.04 | Shoulder Y-difference for slouch (4%) |

### Alert Cooldown

TTS alerts have a 5-second cooldown to prevent repetitive notifications. This is set in `MainActivity.kt`.

## Roadmap

- [ ] Forward head (turtle neck) detection
- [ ] User calibration flow for personalized baselines
- [ ] Spine angle analysis using hip and shoulder keypoints
- [ ] Skeleton overlay visualization
- [ ] Session tracking with statistics and history
- [ ] MVVM architecture with ViewModel + Room database
- [ ] Haptic feedback option
- [ ] Adjustable sensitivity levels
- [ ] Background service for continuous monitoring

## Troubleshooting

| Issue | Solution |
|-------|----------|
| App crashes on start | Grant camera permission in Settings → Apps → PostureGuard |
| No pose detection | Ensure good front-facing lighting; Lite model trades accuracy for speed |
| TTS is silent | Check that Google TTS engine is installed and volume is up |
| High battery drain | Enable Eco mode (black screen) to reduce display power consumption |

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/my-feature`)
3. Commit your changes (`git commit -m 'Add my feature'`)
4. Push to the branch (`git push origin feature/my-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
