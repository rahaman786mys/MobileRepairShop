# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the Android SDK tools proguard configuration.

# Keep Room entities
-keep class com.app.muzzutech.data.model.** { *; }

# Keep ML Kit
-keep class com.google.mlkit.** { *; }

# Keep MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }

# Keep Glide
-keep class com.bumptech.glide.** { *; }

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Google Sign-In
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.auth.GoogleAuthUtil { *; }
-keep class com.google.android.gms.auth.UserRecoverableAuthException { *; }
-keep class com.google.android.gms.common.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }

# Kotlin Coroutines
-dontwarn kotlinx.coroutines.**

# Keep VersionInfo for Gson deserialization
-keep class com.app.muzzutech.utils.update.VersionInfo { *; }

# --- Security hardening ---
# Strip verbose/debug/info logs from release builds so sensitive values
# (phones, tokens, ids) never reach logcat on shipped builds. Warnings/errors
# are kept for crash diagnostics.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
