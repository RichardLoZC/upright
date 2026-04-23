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
-keep class com.example.upright.Landmark3D { *; }
-keep class com.example.upright.PostureDiagnosis { *; }
-keep class com.example.upright.PostureState { *; }
-keep class com.example.upright.CalibrationProfile { *; }
-keep class com.example.upright.BoneLengths { *; }
-keep class com.example.upright.RotationMatrix { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Settings
-keep class com.example.upright.SettingsProfile { *; }
-keep class com.example.upright.SensitivityLevel { *; }
-keep class com.example.upright.AlertLanguage { *; }

# Session data classes
-keep class com.example.upright.SessionEntity { *; }
-keep class com.example.upright.DailySummary { *; }
-keep class com.example.upright.Screen { *; }

# Auto-value annotations (compile-time only)
-dontwarn javax.annotation.processing.**
-dontwarn javax.lang.model.**
