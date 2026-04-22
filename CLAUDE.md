# PostureGuard - CLAUDE.md

## Project Overview

PostureGuard is a native Android app that uses the front-facing camera to monitor sitting posture in real time. It runs entirely on-device (MediaPipe Pose Landmarker) with no network dependency, making it suitable for repurposing an old phone as a dedicated posture monitor.

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material3
- **Camera**: CameraX (ImageAnalysis + Preview)
- **Pose Detection**: Google MediaPipe Pose Landmarker (Lite model, GPU delegate)
- **Alerts**: Android TextToSpeech
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34

## Project Structure

```
app/src/main/java/com/example/postureguard/
├── MainActivity.kt      # Activity, TTS, permission handling, Compose UI
├── PoseDetector.kt      # MediaPipe PoseLandmarker wrapper (LIVE_STREAM mode)
├── PostureLogic.kt      # Posture analysis rules (head tilt + shoulder level)
└── ui/theme/Theme.kt    # Compose Material3 theme
```

## Build & Run

```bash
# Install on connected device
./gradlew installDebug

# Or open in Android Studio, sync Gradle, and run
```

Requires a physical device with a front camera. Camera permission is requested at first launch.

## Architecture Notes

- PoseDetector wraps MediaPipe in LIVE_STREAM mode with async result callbacks
- PostureLogic is a stateless object that maps MediaPipe landmarks to a PostureState enum
- TTS alerts have a 5-second cooldown to prevent spam
- "Eco mode" hides the camera preview (black screen) while detection continues

## Key Files

- `app/src/main/assets/pose_landmarker_lite.task` — MediaPipe model (~5.8 MB)
- `app/build.gradle.kts` — dependencies and build config
- `app/src/main/AndroidManifest.xml` — permissions (CAMERA, WAKE_LOCK)

## Current Limitations

- Only detects head tilt (left/right) and shoulder asymmetry
- No forward-head detection
- No user calibration
- No data persistence or session history
- No ViewModel or DI framework
