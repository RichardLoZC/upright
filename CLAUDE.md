# PostureGuard - CLAUDE.md

## Project Overview

PostureGuard is a native Android app that uses the front-facing camera to monitor sitting posture in real time. It runs entirely on-device (MediaPipe Pose Landmarker Full model) with no network dependency, making it suitable for repurposing an old phone as a dedicated posture monitor.

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material3
- **Camera**: CameraX (ImageAnalysis + Preview)
- **Pose Detection**: Google MediaPipe Pose Landmarker (Full model, GPU with CPU fallback)
- **Smoothing**: 1 Euro Filter for jitter-free landmark tracking
- **Alerts**: Android TextToSpeech (Chinese)
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34

## Project Structure

```
app/src/main/java/com/example/postureguard/
├── MainActivity.kt      # Activity, TTS, permission handling, Compose UI, camera binding
├── PoseDetector.kt      # MediaPipe PoseLandmarker wrapper (LIVE_STREAM, GPU/CPU fallback)
├── PostureLogic.kt      # Biomechanical analysis (CVA, trunk angle, head tilt, shoulder asymmetry)
│                         # CalibrationProfile, PostureStateMachine, calibrated analysis
├── OneEuroFilter.kt     # Adaptive temporal smoothing for landmark coordinates
├── PostureDiagnosis.kt  # Data class for analysis results
└── ui/theme/Theme.kt    # Compose Material3 theme
```

## Build & Run

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=$HOME/Library/Android/sdk
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell pm grant com.example.postureguard android.permission.CAMERA
adb shell am start -n com.example.postureguard/.MainActivity
```

Requires a physical device with a front camera. Camera permission is requested at first launch.

## Architecture Notes

- PoseDetector wraps MediaPipe in LIVE_STREAM mode with async result callbacks
- PostureLogic uses both 2D (normalized) and 3D (World Landmarks) analysis:
  - **2D**: head tilt (ear Y diff), shoulder asymmetry
  - **3D**: CVA (Craniovertebral Angle) for forward head, Trunk Inclination for hunchback
- PostureStateMachine debounces state changes (3 frames bad→alert, 5 frames good→clear)
- 1 Euro Filter adapts smoothing based on movement velocity
- User calibration captures personalized baseline for deviation-based detection
- TTS alerts have a 5-second cooldown to prevent spam
- Eco mode skips 3/4 frames and hides preview for battery saving
- Skeleton overlay shows detected pose landmarks when debug mode is on

## Detection Thresholds

### Absolute (no calibration)
- Head tilt: ear Y diff > 0.05
- Shoulder asymmetry: shoulder Y diff > 0.04
- Forward head: CVA < 48°
- Hunchback: Trunk inclination > 20°

### Calibrated (after user calibration)
- Head tilt deviation > 0.03 from baseline
- Shoulder deviation > 0.03 from baseline
- CVA deviation > 10° from baseline
- Trunk angle deviation > 10° from baseline

## Key Files

- `app/src/main/assets/pose_landmarker_full.task` — MediaPipe Full model (~9 MB)
- `app/build.gradle.kts` — dependencies and build config
- `app/src/main/AndroidManifest.xml` — permissions (CAMERA, WAKE_LOCK), portrait lock
