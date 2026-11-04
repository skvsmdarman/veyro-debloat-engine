# Veyro Debloat Engine — R8 full-mode rules

# ---- Shizuku ----
# Shizuku talks to its provider through IPC and reflection; keep the public API surface.
-keep class rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }
-keep class dev.rikka.shizuku.** { *; }
-keepclassmembers class * implements android.os.IInterface { *; }

# Shizuku provider is referenced from AndroidManifest by class name.
-keep class rikka.shizuku.ShizukuProvider { *; }

# AIDL interfaces are resolved reflectively at runtime.
-keep class * extends android.os.Binder { *; }
-keep interface * extends android.os.IInterface { *; }

# ---- kotlinx.serialization ----
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.veyro.debloatengine.**$$serializer { *; }
-keepclassmembers class com.veyro.debloatengine.** { *** Companion; }
-keepclasseswithmembers class com.veyro.debloatengine.** { kotlinx.serialization.KSerializer serializer(...); }

# ---- Jetpack Compose ----
-dontwarn org.jetbrains.annotations.**

# ---- Lottie (okio references jsr305 annotations not present at runtime) ----
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.*
