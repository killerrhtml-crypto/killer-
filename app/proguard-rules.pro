# ProGuard/R8 rules para Killer Automation

# Mantener clases de Dagger/Hilt
-keep class dagger.* { *; }
-keep class javax.inject.* { *; }
-keep class com.google.dagger.* { *; }
-keep interface com.google.dagger.* { *; }
-keep class com.killer.automation.di.* { *; }
-keep interface com.killer.automation.di.* { *; }

# Mantener anotaciones de Hilt
-keepattributes *Annotation*
-keep @dagger.hilt.android.HiltAndroidApp class *
-keep @dagger.hilt.android.AndroidEntryPoint class *
-keep @dagger.hilt.android.AndroidEntryPoint interface *
-keep @dagger.Module class *
-keep @dagger.Provides class * { *; }

# Mantener modelos de datos
-keep class com.killer.automation.data.models.** { *; }
-keep class com.killer.automation.data.security.** { *; }
-keep class com.killer.automation.data.database.** { *; }

# Mantener clases de Room
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.RoomDatabase { *; }

# Mantener servicios de accesibilidad
-keep class com.killer.automation.core.accessibility.** { *; }
-keep class * extends android.accessibilityservice.AccessibilityService { *; }

# Mantener interfaces de inyección
-keep class * implements com.killer.automation.core.** { *; }

# Mantener clases Application
-keep class com.killer.automation.KillerApplication { *; }
-keep class * extends android.app.Application { *; }

# Mantener Activities
-keep class com.killer.automation.ui.MainActivity { *; }
-keep class * extends androidx.activity.ComponentActivity { *; }

# Mantener enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# JWT Auth0
-keep class com.auth0.jwt.** { *; }
-keepclassmembers class com.auth0.jwt.** { *; }

# SQLCipher
-keep class net.zetetic.database.sqlcipher.** { *; }
-keepclassmembers class net.zetetic.database.sqlcipher.** { *; }

# Retrofit
-keep class com.squareup.retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# OkHttp
-keep class com.squareup.okhttp3.** { *; }
-keep interface com.squareup.okhttp3.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Timber
-keep class timber.log.Timber { *; }
-keep class timber.log.Timber$Tree { *; }

# Tink
-keep class com.google.crypto.tink.** { *; }
-keepclassmembers class com.google.crypto.tink.** { *; }

# Android Security
-keep class androidx.security.crypto.** { *; }
-keepclassmembers class androidx.security.crypto.** { *; }

# Coroutines
-keep class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** { *; }

# Kotlin
-keep class kotlin.** { *; }
-keep interface kotlin.** { *; }
-keepclassmembers class kotlin.** { *; }

# Eliminar logs en release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
-assumenosideeffects class timber.log.Timber {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Preservar información de línea para stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
