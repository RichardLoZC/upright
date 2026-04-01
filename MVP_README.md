# PostureGuard MVP - Native Android Implementation

This project is a native Android application designed to monitor sitting posture using the front camera of an idle phone.

## Features (MVP)
1.  **Real-time Pose Detection**: Uses MediaPipe Pose Landmarker (Lite model).
2.  **Posture Analysis**: Detects head tilt (left/right) and shoulder level.
3.  **Voice Alerts**: Uses Android TTS to speak warnings ("Head tilted left", "Sit straight").
4.  **Eco Mode**: Black screen mode to save battery while monitoring continues.
5.  **Privacy**: All processing is local (on-device). No internet required.

## Project Structure
- `app/src/main/java/com/example/postureguard/`
  - `MainActivity.kt`: Main UI (Compose) and CameraX setup.
  - `PoseDetector.kt`: MediaPipe wrapper.
  - `PostureLogic.kt`: Simple geometric analysis.
  - `ui/theme/`: Compose theme.
- `app/src/main/assets/`: Contains `pose_landmarker_lite.task`.

## Requirements
- Android Studio Hedgehog or newer.
- Android Device (Android 8.0+ recommended).
- Internet permission is NOT used, but Camera permission IS required.

## How to Build & Run
1.  Open this folder in Android Studio.
2.  Sync Gradle.
3.  Connect your Android device via USB.
4.  Run the `app` configuration.
5.  Grant Camera permission when prompted.
6.  Place the phone in front of you (portrait mode).

## Troubleshooting
- **Crash on Start**: Ensure camera permission is granted. Check Logcat for MediaPipe errors.
- **No Detection**: Ensure good lighting. The model is `Lite`, so it trades accuracy for speed.
- **TTS Silent**: Check if your phone has a TTS engine installed (Google TTS) and volume is up.

## Next Steps
- Add calibration (record "good" posture state).
- Improve "forward head" detection logic (currently basic).
- Add visual skeleton overlay for debugging.
