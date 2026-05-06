# ── ProGuard rules for IDPhoto release build ──

# ── Tối ưu kích thước ──
-optimizationpasses 5
-allowaccessmodification
-repackageclasses ''

# Protobuf (DataStore internals)
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
  <fields>;
}

# ONNX Runtime — keep JNI bindings only (not entire library)
-keep class ai.onnxruntime.OnnxRuntime { *; }
-keep class ai.onnxruntime.OrtEnvironment { *; }
-keep class ai.onnxruntime.OrtSession { *; }
-keep class ai.onnxruntime.OrtSession$* { *; }
-keep class ai.onnxruntime.OnnxTensor { *; }
-keep class ai.onnxruntime.OnnxValue { *; }
-keep class ai.onnxruntime.OnnxMap { *; }
-keep class ai.onnxruntime.OnnxSequence { *; }
-keep class ai.onnxruntime.OrtException { *; }
-keep class ai.onnxruntime.OrtUtil { *; }
-keep class ai.onnxruntime.TensorInfo { *; }
-keep class ai.onnxruntime.MapInfo { *; }
-keep class ai.onnxruntime.SequenceInfo { *; }
-keep class ai.onnxruntime.NodeInfo { *; }
-keep class ai.onnxruntime.OrtLoggingLevel { *; }
-keep class ai.onnxruntime.OrtProvider { *; }
-keepclassmembers class ai.onnxruntime.** {
    native <methods>;
}
-dontwarn ai.onnxruntime.**

# ML Kit — keep public API for face detection & segmentation
-keep class com.google.mlkit.vision.face.** { *; }
-keep class com.google.mlkit.vision.segmentation.** { *; }
-keep class com.google.mlkit.common.** { *; }
-dontwarn com.google.mlkit.**

# Kotlin Coroutines
-dontwarn kotlinx.coroutines.**

# Compose — keep runtime internals
-dontwarn androidx.compose.**

# ExifInterface
-keep class androidx.exifinterface.media.** { *; }
