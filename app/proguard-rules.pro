# MediaPipe native libs and task models
-keep class com.google.mediapipe.** { *; }
-keep class com.google.mediapipe.tasks.** { *; }
-dontwarn com.google.mediapipe.**

# CameraX
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# DataStore
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# Keep data classes used in serialization
-keep class com.example.postureguard.Landmark3D { *; }
-keep class com.example.postureguard.PostureDiagnosis { *; }
-keep class com.example.postureguard.PostureState { *; }
-keep class com.example.postureguard.CalibrationProfile { *; }
-keep class com.example.postureguard.BoneLengths { *; }
-keep class com.example.postureguard.RotationMatrix { *; }
