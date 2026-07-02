# Killer Automation ProGuard Rules

# Firebase
-keep class com.firebase.** { *; }
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Kotlin
-keep class kotlin.** { *; }
-keep interface kotlin.** { *; }
-dontwarn kotlin.**
-dontwarn kotlin.reflect.**

# Keep your application classes
-keep class com.killer.automation.** { *; }
-keep class com.killer.automation.manager.** { *; }
-keep class com.killer.automation.engine.** { *; }
-keep class com.killer.automation.data.** { *; }
-keep class com.killer.automation.ui.** { *; }
-keep class com.killer.automation.service.** { *; }
-keep class com.killer.automation.utils.** { *; }

# Androidx
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Remove logging
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
