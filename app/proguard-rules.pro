# AshTube ProGuard / R8 Shrinking & Hardening Rules

# 1. AndroidX Room SQLite DAOs and Entities
-keep class androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class com.ashrin.ashtube.data.local.entities.** { *; }
-keep interface com.ashrin.ashtube.data.local.dao.** { *; }

# 2. AndroidX Media3 / ExoPlayer
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.ui.** { *; }
-keep class androidx.media3.common.** { *; }
-dontwarn androidx.media3.**

# 3. AndroidX WorkManager
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class com.ashrin.ashtube.download.DownloadWorker { *; }

# 4. Kotlin Coroutines & DataStore
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }
-keepclassmembers class * extends kotlinx.coroutines.CoroutineScope { *; }
-keep class androidx.datastore.** { *; }

# 5. Rust UniFFI / JNI Native Interface Scaffolding
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class uniffi.** { *; }
-keep class com.ashrin.ashtube.rust.** { *; }

# 6. Hardening: Strip Sensitive Logging & Telemetry in Release Builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
