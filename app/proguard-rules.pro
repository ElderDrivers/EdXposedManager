# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\Rikka\AppData\Local\Android\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-optimizationpasses 7
#指定混淆是采用的算法，后面的参数是一个过滤器，这个过滤器是谷歌推荐的算法，一般不做更改
-optimizations !code/simplification/cast,!field/*,!class/merging/*
#混合时不使用大小写混合，混合后的类名为小写,windows下必须使用该选项
-dontusemixedcaseclassnames
#指定不去忽略非公共库的类和成员
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers

#输出详细信息
-verbose
#输出类名->混淆后类名的映射关系
-printmapping map.txt
#不做预校验，preverify是proguard的四个步骤之一，Android不需要preverify，去掉这一步能够加快混淆速度。
-dontpreverify
#保留Annotation不混淆
-keepattributes *Annotation*,InnerClasses
#避免混淆泛型
-keepattributes Signature
#抛出异常时保留代码行号
-keepattributes SourceFile,LineNumberTable
#保留本地native方法不被混淆
-keepclasseswithmembernames class * {
    native <methods>;
}

#保留枚举类不被混淆
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}


-keep class android.** { *; }
-keep class androidx.** { *; }
-dontwarn android.**
-keep class com.android.internal.** { *; }
-dontwarn com.android.internal.**
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View
-keep public class com.android.vending.licensing.ILicensingService

-keep class rxhttp.** { *; }
-keep class com.rxjava.** { *; }
-keep class rx.** { *; }
-keep class **.R$* {*;}
-keep class de.robv.android.xposed.installer.util.** { *; }
-keep class de.robv.android.xposed.installer.XposedApp {
    public *;
    private *;
}
-keep class com.solohsu.android.edxp.manager.** { *; }

-keep class androidx.constraintlayout.** { *; }
-keep class com.afollestad.material-dialogs.** { *; }
-keep class com.github.mtotschnig.** { *; }
-keep class eu.chainfire.** { *; }
-keep class com.github.topjohnwu.libsu.** { *; }
-keep class com.squareup.picasso.** { *; }
-keep class de.psdev.licensesdialog.** { *; }
-keep class com.annimon.** { *; }
-keep class com.google.code.gson.** { *; }
-keep class com.google.android.material.** { *; }
-keep class com.github.coxylicacid.** { *; }
-keep class com.jpardogo.googleprogressbar.** { *; }
-keep class com.github.stealthcopter.** { *; }
-keep class com.facebook.shimmer.** { *; }

-keep class com.sergivonavi.** { *; }
-keep class com.github.lihangleo2.** { *; }
-keep class com.shuhart.stepview.** { *; }
-keep class com.getkeepsafe.taptargetview.** { *; }
-keep class com.scottyab.** { *; }
-keep class com.github.coxylicacid.takagi.** { *; }

-dontwarn com.yalantis.ucrop**
-keep class com.yalantis.ucrop** { *; }
-keep interface com.yalantis.ucrop** { *; }

-keepclassmembers class * {
    void *(**On*Event);
}

-dontwarn android.os.FileUtils
-dontwarn com.emilsjolander.components.stickylistheaders.**
-dontwarn com.squareup.picasso.**

-obfuscationdictionary tcn2.txt
-classobfuscationdictionary tcn2.txt
-packageobfuscationdictionary tcn2.txt