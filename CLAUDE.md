# PostureGuard - CLAUDE.md

## Project Overview

PostureGuard is a native Android app that uses the front-facing camera to monitor sitting posture in real time. It runs entirely on-device (MediaPipe Pose Landmarker Full model) with no network dependency, making it suitable for repurposing an old phone as a dedicated posture monitor.

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material3 + Material Icons Extended
- **Theme**: Custom dark theme with branded PostureGuard color palette
- **Architecture**: ViewModel + StateFlow (unidirectional data flow), screen-based navigation (no compose-navigation)
- **Camera**: CameraX (ImageAnalysis + Preview)
- **Pose Detection**: Google MediaPipe Pose Landmarker (Full model, GPU with CPU fallback)
- **Smoothing**: 1 Euro Filter for jitter-free landmark tracking
- **Spatial Refinement**: Bone-length constancy optimization + affine coordinate normalization
- **Persistence**: DataStore Preferences (calibration profiles + user settings), Room (session history)
- **Background**: Foreground Service with notification for continuous monitoring
- **Alerts**: Synthesized sound effects (bird chirp for correction, crow caw for persistent bad posture >1 min) via AudioTrack PCM + haptic vibration feedback
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34

## Project Structure

```
app/src/main/java/com/example/postureguard/
├── MainActivity.kt            # Activity, Compose UI with animated posture ring, skeleton overlay, session stats
├── PostureGuardViewModel.kt   # ViewModel: sound effects, calibration, state machine, haptics, session tracking, throttled UI, pause, settings, navigation
├── PoseDetector.kt            # MediaPipe PoseLandmarker wrapper (LIVE_STREAM, GPU/CPU fallback)
├── PostureLogic.kt            # Biomechanical analysis (CVA, trunk angle, head tilt, shoulder asymmetry) with sensitivity multiplier
│                              # CalibrationProfile, PostureStateMachine, calibrated analysis
├── OneEuroFilter.kt           # Adaptive temporal smoothing for landmark coordinates
├── SpatialRefinement.kt       # Bone-length constancy optimizer, affine rotation normalizer
├── CalibrationStore.kt        # DataStore persistence for calibration profiles (including bone lengths + rotation matrix)
├── SettingsStore.kt           # DataStore persistence for user settings (alert interval, sensitivity, language, etc.)
├── AppDatabase.kt             # Room database singleton for session history
├── SessionDao.kt              # Room DAO for session queries (daily, weekly, streak)
├── SessionEntity.kt           # Room entity for posture session data
├── Strings.kt                 # Multi-language strings (S data class, StringsZh/StringsEn, stringsFor())
├── OnboardingScreen.kt        # 3-page onboarding (purpose, setup tips, get started)
├── SettingsScreen.kt          # Settings page (alerts, sensitivity, language, calibration management)
├── HistoryScreen.kt           # History page (weekly chart, daily goal, streak, session list)
├── PostureGuidance.kt         # Animated directional arrows overlay for posture correction
├── SoundEffects.kt            # Synthesized bird chirp and crow caw via AudioTrack PCM
├── PostureMonitorService.kt   # Foreground service for background monitoring
├── PostureDiagnosis.kt        # Data class for analysis results
└── ui/theme/Theme.kt          # Compose Material3 theme

app/src/test/java/com/example/postureguard/
└── PostureLogicTest.kt        # Unit tests (2D/3D analysis, state machine, filter, spatial refinement, sensitivity)
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
- **Screen navigation**: `Screen` enum (ONBOARDING, MAIN, SETTINGS, HISTORY) in UiState, rendered via `when` expression in `PostureGuardApp`
- **Onboarding**: 3-page flow shown on first launch, persisted via SettingsStore
- PoseDetector wraps MediaPipe in LIVE_STREAM mode with async result callbacks
- FrameProcessor handles bitmap rotation, FPS tracking, and token-based async result matching
- PostureLogic uses both 2D (normalized) and 3D (World Landmarks) analysis with configurable sensitivity multiplier
- **Spatial Refinement Pipeline** (applied before analysis):
  1. Affine rotation normalizes camera tilt using calibration-derived rotation matrix
  2. Bone-length optimizer corrects Z-axis drift by enforcing calibrated skeletal proportions
- CVA uses 3D midpoint of both ears with sagittal plane (YZ) projection
- PostureStateMachine debounces state changes (3 frames bad→alert, 5 frames good→clear)
- 1 Euro Filter adapts smoothing based on movement velocity
- User calibration captures personalized baseline including bone ratios and rotation matrix
- Calibration aborts if person leaves during countdown
- **Settings**: Alert interval (5/10/30/60s), sound/vibration toggles, sensitivity (LOW/MEDIUM/HIGH), language (ZH/EN), auto-resume timer, calibration management
- **Multi-language**: Centralized `S` data class in Strings.kt with `StringsZh`/`StringsEn` instances; all UI screens use `stringsFor(lang)` to get localized strings based on `AlertLanguage` setting
- **Pause/Resume**: Auto-resume countdown, skips frame processing and stats when paused
- **Posture guidance**: Animated directional arrows on camera preview for each bad posture type
- **History**: Room database with daily sessions, weekly bar chart, daily goal (80%), streak counter
- Session saved on Activity ON_PAUSE and ViewModel onCleared with double-save prevention
- Foreground service enables continuous monitoring when app is minimized
- Notification updates with current posture state in real time
- Sound alerts: bird chirp once on posture correction, crow caw after bad posture persists >1 min at configurable interval
- Haptic vibration (150ms) on transition from good to bad posture
- Eco mode skips 3/4 frames and hides preview for battery saving
- UI updates throttled to ~30fps to reduce unnecessary recompositions
- NO_PERSON timeout (2 min) suggests pausing monitoring
- Thread-safe calibration sample collection (CopyOnWriteArrayList)
- Custom dark theme with PostureGuard brand colors (green/red/blue/gray/orange)

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

### Sensitivity Levels
- LOW: multiplier 1.5 (harder to trigger, fewer alerts)
- MEDIUM: multiplier 1.0 (default)
- HIGH: multiplier 0.7 (easier to trigger, more alerts)

## Key Files

- `app/src/main/assets/pose_landmarker_full.task` — MediaPipe Full model (~9 MB)
- `app/build.gradle.kts` — dependencies and build config (includes Room, kotlin-kapt)
- `app/proguard-rules.pro` — ProGuard keep rules for MediaPipe, data classes, Room
- `app/src/main/AndroidManifest.xml` — permissions (CAMERA, WAKE_LOCK, FOREGROUND_SERVICE), landscape support, foreground service
- `app/src/main/res/drawable/ic_notification.xml` — Custom shield notification icon
