# Add project specific ProGuard rules here.
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
  <fields>;
}
-keep class ai.onnxruntime.** { *; }
