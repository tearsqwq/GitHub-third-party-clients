# ============================================
# R8 混淆配置 - GitHub Client App
# ============================================

# ---------- 基本配置 ----------
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations

# ---------- Kotlin ----------
-keep class kotlin.Metadata { *; }
-keepattributes kotlin.Metadata
-keep class kotlin.Unit { *; }
-keep class kotlin.collections.** { *; }
-keep class kotlin.jvm.internal.** { *; }
-keepclassmembers class kotlin.jvm.internal.Lambda {
    <methods>;
}
-keepclassmembers class * {
    ** lambda$***;
}

# ---------- 应用入口点 ----------
-keep class com.kun.github.GitHubApplication { *; }
-keep class com.kun.github.MainActivity { *; }
-keep class com.kun.github.ui.screens.** { *; }

# ---------- Kotlinx Serialization ----------
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.** { *; }

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}

-keepclassmembers class kotlinx.serialization.builtins.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keepclasseswithmembers class ** {
    @kotlinx.serialization.Serializable <fields>;
}

-keep class **$$serializer { *; }

-keepclassmembers class **$$serializer {
    kotlinx.serialization.descriptors.SerialDescriptor getDescriptor();
    kotlinx.serialization.KSerializer[] childSerializers();
}

-keep @kotlinx.serialization.Serializable class * { *; }

-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
    @kotlinx.serialization.Serializable <methods>;
}

# ---------- Data Model ----------
-keep class com.kun.github.data.model.** { *; }
-keepclassmembers class com.kun.github.data.model.** { *; }
-keep class com.kun.github.data.remote.dto.** { *; }
-keepclassmembers class com.kun.github.data.remote.dto.** { *; }

# ---------- Retrofit ----------
-keep interface com.kun.github.data.remote.api.** { *; }
-keep class com.kun.github.data.remote.api.** { *; }
-keep,allowobfuscation,allowshrinking class * extends retrofit2.Call
-keep,allowobfuscation,allowshrinking interface retrofit2.http.** { *; }
-keep class retrofit2.Response { *; }
-keepclassmembers class retrofit2.Response { *; }
-keep class retrofit2.Retrofit { *; }
-keepclassmembers class retrofit2.Retrofit { *; }
-keep class retrofit2.HttpException { *; }
-keep class retrofit2.Platform { *; }

# Retrofit Kotlinx Serialization Converter (jakewharton)
-keep class com.jakewharton.retrofit2.converter.kotlinx.serialization.** { *; }
-keepclassmembers class com.jakewharton.retrofit2.converter.kotlinx.serialization.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }

# OkHttp Logging Interceptor
-dontwarn okhttp3.logging.**
-keep class okhttp3.logging.** { *; }
-keepclassmembers class okhttp3.logging.** { *; }

# ---------- Jetpack Compose ----------
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ---------- ViewModel ----------
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class com.kun.github.presentation.**.ViewModel { *; }
-keep class com.kun.github.presentation.**.**ViewModel { *; }

# ---------- DataStore ----------
-keep class androidx.datastore.** { *; }
-keep class * extends androidx.datastore.core.Serializer { *; }

# ---------- 协程 ----------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ---------- Android组件 ----------
-keep class com.kun.github.service.** { *; }
-keep class * extends android.content.BroadcastReceiver { *; }

# ---------- AndroidX Browser / CustomTabs ----------
-keep class androidx.browser.** { *; }
-keep class androidx.browser.customtabs.** { *; }
-dontwarn androidx.browser.**

# ---------- 配置类（含 JNI Native 方法，绝对不能混淆） ----------
-keep class com.kun.github.config.** { *; }
-keepclassmembers class com.kun.github.config.** { *; }

# ---------- 拦截器 ----------
-keep class com.kun.github.data.remote.interceptor.** { *; }

# ---------- Repository ----------
-keep class com.kun.github.data.repository.** { *; }

# ---------- Preferences ----------
-keep class com.kun.github.data.local.preferences.** { *; }
-keep class com.kun.github.data.local.preferences.GitHubAccount { *; }

# ---------- 工具类 ----------
-keep class com.kun.github.utils.** { *; }
-keep class com.kun.github.presentation.utils.** { *; }

# ---------- 导航 ----------
-keep class com.kun.github.presentation.navigation.** { *; }

# ---------- 主题 ----------
-keep class com.kun.github.presentation.theme.** { *; }

# ---------- 组件 ----------
-keep class com.kun.github.presentation.components.** { *; }

# ---------- 设置 ----------
-keep class com.kun.github.presentation.settings.** { *; }

# ---------- DI ----------
-keep class com.kun.github.di.** { *; }

# ---------- Coil ----------
-keep class coil.** { *; }
-keep interface coil.** { *; }

# ---------- Kyant Backdrop ----------
-keep class com.kyant.backdrop.** { *; }
-keep class com.kyant.shapes.** { *; }
-dontwarn com.kyant.**

# ---------- Kotlin Reflect ----------
-keep class kotlin.reflect.** { *; }
-keep interface kotlin.reflect.** { *; }

# ---------- Native JNI ----------
-keepclasseswithmembernames class * {
    native <methods>;
}

# ---------- 枚举 ----------
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---------- Parcelable ----------
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# ---------- Serializable ----------
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ---------- 通用优化 ----------
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/variable

# ---------- 不警告缺失的引用 ----------
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.Platform$Java8
-dontwarn kotlinx.serialization.**
