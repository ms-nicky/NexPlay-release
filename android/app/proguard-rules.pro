-keep class org.webrtc.** { *; }
-keep class org.jni_zero.** { *; }
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class com.nexplay.** {
    @kotlinx.serialization.Serializable *;
}
