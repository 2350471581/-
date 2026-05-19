# ── 防编译 / 混淆规则 ──

# 保留行号（方便调试崩溃，但不暴露源码）
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute ""

# 移除所有日志输出
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# ── Room 数据库 ──
-keep class com.jizhang.tracker.data.TransactionEntity { *; }
-keep class com.jizhang.tracker.data.TransactionType { *; }
-keep class com.jizhang.tracker.data.TransactionSource { *; }
-keep class com.jizhang.tracker.data.ParsedBill { *; }
-keep class com.jizhang.tracker.data.AppDatabase { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# ── 数据模型 ──
-keep class com.jizhang.tracker.data.UpdateInfo { *; }
-keep class com.jizhang.tracker.data.UpdateSource { *; }
-keep class com.jizhang.tracker.data.UpdateResult { *; }
-keep class com.jizhang.tracker.data.DownloadResult { *; }
-keep class com.jizhang.tracker.data.UpdateResult* { *; }
-keep class com.jizhang.tracker.data.DownloadResult* { *; }
-keep class com.jizhang.tracker.data.CustomPlan { *; }
-keep class com.jizhang.tracker.data.PlanDataType { *; }

# ── 自定义分类 (org.json 序列化) ──
-keep class com.jizhang.tracker.data.CustomCategory { *; }

# ── AI / 解析数据类 ──
-keep class com.jizhang.tracker.data.AIParseResult { *; }
-keep class com.jizhang.tracker.data.AIChatResult { *; }
-keep class com.jizhang.tracker.data.AIBillException { *; }
-keep class com.jizhang.tracker.data.SmsMessage { *; }
-keep class com.jizhang.tracker.data.NotificationMessage { *; }
-keep class com.jizhang.tracker.data.NotificationParseResult { *; }

# ── 图表数据类 ──
-keep class com.jizhang.tracker.ui.ChartLabel { *; }

# ── Hilt / Dagger ──
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-dontwarn dagger.hilt.**

# ── Moshi ──
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class * { @com.squareup.moshi.Json <fields>; }
-dontwarn com.squareup.moshi.**

# ── Navigation ──
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# ── 主题配置 (JSON 序列化) ──
-keep class com.jizhang.tracker.ui.CustomThemeConfig { *; }
-keep class com.jizhang.tracker.ui.ThemePalette { *; }

# ── JSON ──
-keep class org.json.** { *; }
-dontwarn org.json.**

# ── Kotlin 序列化 ──
-keepattributes *Annotation*
-keepattributes InnerClasses
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# ── Compose ──
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ── OkHttp ──
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# ── ML Kit ──
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# ── Palette ──
-keep class androidx.palette.** { *; }

# ── Parcelable / Serializable ──
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}
-keep class * implements java.io.Serializable { *; }

# ── ViewModel / LiveData ──
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }

# ── ApiKeyManager (Keystore-based, fully obfuscated) ──

# ── 去除调试信息 ──
-keepattributes Exceptions,Signature,Deprecated
-keepattributes *Annotation*

# ── 混淆类名使用短名称 ──
-repackageclasses
-overloadaggressively

# ── 通用 ──
-dontusemixedcaseclassnames
-verbose
