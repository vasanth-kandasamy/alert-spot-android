# Proguard rules for AlertSpot

# Keep Gson serialized models
-keep class com.alertspot.model.** { *; }

# Google Maps
-keep class com.google.android.gms.maps.** { *; }
