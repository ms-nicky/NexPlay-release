-keep class org.webrtc.** { *; }
-keep class org.jni_zero.** { *; }
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class com.nexplay.** {
    @kotlinx.serialization.Serializable *;
}
-keep class com.pedro.** { *; }
-keep class com.pedro.library.** { *; }
-keep class com.pedro.encoder.** { *; }
-keep class com.pedro.common.** { *; }
-dontwarn com.pedro.**
