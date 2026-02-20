# Proguard rules for AlertSpot

# Keep Gson serialized models
-keep class com.alertspot.model.** { *; }

# osmdroid
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# Google Play Services Location
-keep class com.google.android.gms.location.** { *; }
