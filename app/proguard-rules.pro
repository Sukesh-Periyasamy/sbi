# AnteClick ProGuard Rules for Play Store Release

# ── Keep application entry points ────────────────────────────────────────────
-keep public class com.anteclick.app.MainActivity
-keep public class com.anteclick.app.service.AnteClickAccessibilityService
-keep public class com.anteclick.app.warnings.WarningActivity

# ── Keep Accessibility Service ───────────────────────────────────────────────
-keep class com.anteclick.app.service.AnteClickAccessibilityService {
    public <methods>;
}

# ── Keep Retrofit API interfaces ─────────────────────────────────────────────
-keep interface com.anteclick.app.backend.ThreatApi { *; }
-keep class com.anteclick.app.models.** { *; }

# ── Keep Gson models ──────────────────────────────────────────────────────────
-keepclassmembers class com.anteclick.app.models.** {
    <fields>;
    <init>(...);
}

# ── Keep Compose runtime ──────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-keep class androidx.lifecycle.** { *; }

# ── Strip verbose and debug logs in release (preserve info/warn/error for diagnostics) ─
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
}

# ── Retrofit + OkHttp ─────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }

# ── Kotlin coroutines ─────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ── General optimization ──────────────────────────────────────────────────────
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# ── Keep line numbers for crash reports ───────────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
