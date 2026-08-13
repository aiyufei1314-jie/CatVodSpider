# 基础优化
-keepattributes *Annotation*, Signature, EnclosingMethod, InnerClasses
-keepattributes SourceFile, LineNumberTable
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, RuntimeVisibleTypeAnnotations
-keepattributes AnnotationDefault, Exceptions
-flattenpackagehierarchy com.github.catvod.spider.merge


# AndroidX
-keep class androidx.core.** { *; }
-keep class androidx.concurrent.** { *; }
-keep class androidx.javascriptengine.** { *; }


# Spider
-keep class com.github.catvod.crawler.* { *; }
-keep class com.github.catvod.spider.* { public <methods>; }


# OkHttp
-dontwarn okhttp3.**
-keep class okio.** { *; }
-keep class okhttp3.** { *; }


# Gson
-dontwarn com.google.gson.**
-keep class com.google.gson.** { *; }


# QuickJS
-dontwarn com.whl.quickjs.**
-keep class com.whl.quickjs.** { *; }