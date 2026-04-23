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

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Settings
-keep class com.example.postureguard.SettingsProfile { *; }
-keep class com.example.postureguard.SensitivityLevel { *; }
-keep class com.example.postureguard.AlertLanguage { *; }

# Session data classes
-keep class com.example.postureguard.SessionEntity { *; }
-keep class com.example.postureguard.DailySummary { *; }
-keep class com.example.postureguard.Screen { *; }

# Auto-value annotations (compile-time only)
-dontwarn javax.annotation.processing.**
-dontwarn javax.lang.model.**
