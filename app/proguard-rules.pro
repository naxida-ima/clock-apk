# clock-apk ProGuard/R8 规则
# 默认规则 proguard-android-optimize.txt 已自动保留 Manifest 引用的类。
# 以下仅补充可能被反射或动态引用的条目。

# 保留 SntpClient 返回的数据类（可能被 Kotlin 内联影响）
-keep class com.example.clock.SntpClient$SntpResult { *; }

# 保留 SharedPreferences 中存储的序列化相关
-keepclassmembers class * {
    @org.json.JSONObject <methods>;
}
