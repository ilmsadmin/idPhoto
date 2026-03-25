# ── ProGuard rules for IDPhoto release build ──

# Protobuf (DataStore internals)
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
  <fields>;
}

# ONNX Runtime — keep all native bindings
-keep class ai.onnxruntime.** { *; }

# ML Kit — keep public API for face detection & segmentation
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# CameraX — keep camera provider
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Kotlin Coroutines
-dontwarn kotlinx.coroutines.**

# Compose — keep runtime internals
-dontwarn androidx.compose.**

# ExifInterface
-keep class androidx.exifinterface.media.** { *; }
