# PostureGuard - CLAUDE.md

## Project Overview

PostureGuard is a native Android app that uses the front-facing camera to monitor sitting posture in real time. It runs entirely on-device (MediaPipe Pose Landmarker Full model) with no network dependency, making it suitable for repurposing an old phone as a dedicated posture monitor.

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material3 + Material Icons Extended
- **Theme**: Custom dark theme with branded PostureGuard color palette
- **Architecture**: ViewModel + StateFlow (unidirectional data flow)
- **Camera**: CameraX (ImageAnalysis + Preview)
- **Pose Detection**: Google MediaPipe Pose Landmarker (Full model, GPU with CPU fallback)
- **Smoothing**: 1 Euro Filter for jitter-free landmark tracking
- **Spatial Refinement**: Bone-length constancy optimization + affine coordinate normalization
- **Persistence**: DataStore Preferences (calibration profiles)
- **Background**: Foreground Service with notification for continuous monitoring
- **Alerts**: Android TextToSpeech (Chinese) with varied messages + haptic vibration feedback
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34

## Project Structure

```
app/src/main/java/com/example/postureguard/
├── MainActivity.kt            # Activity, Compose UI with animated posture ring, skeleton overlay, session stats
├── PostureGuardViewModel.kt   # ViewModel: TTS, calibration, state machine, haptics, session tracking, throttled UI
├── PoseDetector.kt            # MediaPipe PoseLandmarker wrapper (LIVE_STREAM, GPU/CPU fallback)
├── PostureLogic.kt            # Biomechanical analysis (CVA, trunk angle, head tilt, shoulder asymmetry)
│                              # CalibrationProfile, PostureStateMachine, calibrated analysis
├── OneEuroFilter.kt           # Adaptive temporal smoothing for landmark coordinates
├── SpatialRefinement.kt       # Bone-length constancy optimizer, affine rotation normalizer
├── CalibrationStore.kt        # DataStore persistence for calibration profiles (including bone lengths + rotation matrix)
├── PostureMonitorService.kt   # Foreground service for background monitoring
├── PostureDiagnosis.kt        # Data class for analysis results
└── ui/theme/Theme.kt          # Compose Material3 theme

app/src/test/java/com/example/postureguard/
└── PostureLogicTest.kt        # Unit tests (2D/3D analysis, state machine, filter, spatial refinement)
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

- **ViewModel pattern**: All state flows through `PostureGuardViewModel` → `UiState` StateFlow → Compose UI
- PoseDetector wraps MediaPipe in LIVE_STREAM mode with async result callbacks
- FrameProcessor handles bitmap rotation, FPS tracking, and token-based async result matching
- PostureLogic uses both 2D (normalized) and 3D (World Landmarks) analysis:
  - **2D**: head tilt (ear Y diff), shoulder asymmetry
  - **3D**: CVA (Craniovertebral Angle) for forward head, Trunk Inclination for hunchback
- **Spatial Refinement Pipeline** (applied before analysis):
  1. Affine rotation normalizes camera tilt using calibration-derived rotation matrix
  2. Bone-length optimizer corrects Z-axis drift by enforcing calibrated skeletal proportions
- CVA uses 3D midpoint of both ears with sagittal plane (YZ) projection
- PostureStateMachine debounces state changes (3 frames bad→alert, 5 frames good→clear)
- 1 Euro Filter adapts smoothing based on movement velocity
- User calibration captures personalized baseline including bone ratios and rotation matrix
- Calibration profiles persisted via DataStore including bone lengths and rotation matrix (survives app restart)
- Foreground service enables continuous monitoring when app is minimized
- TTS alerts have a 5-second cooldown with varied messages to reduce annoyance
- Haptic vibration (150ms) on posture state change to bad
- Eco mode skips 3/4 frames and hides preview for battery saving
- UI updates throttled to ~30fps to reduce unnecessary recompositions
- Session statistics track good/bad posture duration with percentage display
- Animated posture ring indicator with pulse effect on bad posture
- Custom dark theme with PostureGuard brand colors (green/red/blue/gray)
- Gradient overlays on top and bottom for better readability over camera feed
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
- `app/proguard-rules.pro` — ProGuard keep rules for MediaPipe and data classes
- `app/src/main/AndroidManifest.xml` — permissions (CAMERA, WAKE_LOCK, FOREGROUND_SERVICE), portrait lock, foreground service
