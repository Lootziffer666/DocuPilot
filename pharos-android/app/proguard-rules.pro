# Pharos ProGuard Rules

# Keep Gson serialization classes
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.pharos.app.network.model.** { *; }
-keep class com.pharos.app.data.db.entity.** { *; }

# Keep Room entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# PDFBox
-dontwarn com.tom_roush.pdfbox.**
-keep class com.tom_roush.pdfbox.** { *; }
