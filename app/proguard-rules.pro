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
