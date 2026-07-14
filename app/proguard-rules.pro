# clock-apk ProGuard/R8 规则 —— 激进混淆
# 默认规则 proguard-android-optimize.txt 已自动保留 Manifest 引用的类。

# === 混淆字典：类名、方法名用随机乱码 ===
-obfuscationdictionary proguard-dictionary.txt
-classobfuscationdictionary class-dictionary.txt
-packageobfuscationdictionary proguard-dictionary.txt

# === 保留不能被混淆的 ===

# ⚠️ JNI native 类：类名与 native 方法名必须原样保留，否则静态注册找不到
# Java_com_example_clock_Obf_decode / Java_com_example_clock_Obf_matchSeq 符号
# → UnsatisfiedLinkError → 启动闪退。这是 native 化后闪退的根因。
-keep class com.example.clock.Obf { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# SntpClient 返回的数据类（Kotlin data class）
-keep class com.example.clock.SntpClient$SntpResult { *; }

# JSON 操作相关
-keepclassmembers class * {
    @org.json.JSONObject <methods>;
}

# XML PullParser 相关
-keep class org.xmlpull.v1.** { *; }

# === 删除日志和调试信息 ===
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
