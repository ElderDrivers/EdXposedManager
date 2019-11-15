-dontobfuscate

# These are mostly picked from proguard-android-optimize.txt
-optimizations !code/allocation/variable,!code/simplification/cast,!field/*,!class/merging/*,!method/propagation/returnvalue,!method/inlining/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# For enumeration classes, see http://proguard.sourceforge.net/manual/examples.html#enumerations
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保留R下面的资源
-keep class **.R$* {*;}
# 保留四大组件，自定义的Application等这些类不被混淆
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Appliction
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.preference.Preference
-keep public class com.android.vending.licensing.ILicensingService

-keepattributes EnclosingMethod
-keep class io.haobi.wallet.beans.** { *; }
-keep class sun.misc.Unsafe { *; }

# 保留Parcelable序列化类不被混淆
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

-keepclassmembers class * implements java.io.Serializable {
   static final long serialVersionUID;
   private static final java.io.ObjectStreamField[]   serialPersistentFields;
   private void writeObject(java.io.ObjectOutputStream);
   private void readObject(java.io.ObjectInputStream);
   java.lang.Object writeReplace();
   java.lang.Object readResolve();
}
#assume no side effects:删除android.util.Log输出的日志
#-assumenosideeffects class android.util.Log {
#    public static *** v(...);
#    public static *** d(...);
#    public static *** i(...);
#    public static *** w(...);
#    public static *** e(...);
#}

-keep,allowobfuscation @interface android.support.annotation.Keep
-keep @android.support.annotation.Keep class *
-keepclassmembers class * {
    @android.support.annotation.Keep *;
}

-keep class rxhttp.**{*;}
-keep class androidx.**{*;}
#-keep class com.github.**{*;}
-keep class com.google.**{*;}
-keep class com.squareup.picasso.**{*;}
-keep class eu.chainfire.**{*;}
#-keep class com.afollestad.**{*;}
-keep class io.**{*;}
#-keep class com.annimon.**{*;}
-keep class de.psdev.licensesdialog.**{*;}

# The support library contains references to newer platform versions.
# Don't warn about those in case this app is linking against an older
# platform version.  We know about them, and they are safe.
-dontwarn android.support.**
-dontwarn androidx.appcompat.**

-dontwarn okhttp3.logging.**
-keep class okhttp3.internal.**{*;}
-dontwarn okio.**

# These are ok as well
-dontwarn android.os.FileUtils
-dontwarn com.emilsjolander.components.stickylistheaders.**

#-obfuscationdictionary dic.txt
#
#-classobfuscationdictionary dic.txt
#
#-packageobfuscationdictionary dic.txt