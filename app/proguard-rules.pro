# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

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
# MeiZuFingerprint
-dontwarn com.fingerprints.service.**
-keep class com.fingerprints.service.** { *; }

# SmsungFingerprint
-dontwarn com.samsung.android.sdk.**
-keep class com.samsung.android.sdk.** { *; }

################################################################
################### Global config     ##########################
################################################################
# 代码混淆压缩比，在0和7之间，默认为5，一般不需要改
-optimizationpasses 5

# 混淆时不使用大小写混合，混淆后的类名为小写
#这个是给Microsoft Windows用户的，因为ProGuard假定使用的操作系统是能区分两个只是大小写不同的文件名，但是Microsoft Windows不是这样的操作系统，所以必须为ProGuard指定-dontusemixedcaseclassnames选项
-dontusemixedcaseclassnames

# 指定不去忽略非公共的库的类的成员
-dontskipnonpubliclibraryclassmembers

# 指定不去忽略非公共的库类
# 用于告诉ProGuard，不要跳过对非公开类的处理。默认情况下是跳过的，因为程序中不会引用它们，有些情况下人们编写的代码与类库中的类在同一个包下，并且对包中内容加以引用，此时需要加入此条声明
-dontskipnonpubliclibraryclasses

# 不做预校验，preverify是proguard的4个步骤之一
# Android不需要preverify，去掉这一步可加快混淆速度
-dontpreverify

-repackageclasses ''
-allowaccessmodification

# 指定混淆时采用的算法，后面的参数是一个过滤器
# 这个过滤器是谷歌推荐的算法，一般不改变
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# 有了verbose这句话，混淆后就会生成映射文件
# 包含有类名->混淆后类名的映射关系
# 然后使用printmapping指定映射文件的名称
-verbose
-printmapping proguardMapping.txt

# 保护代码中的Annotation不被混淆，这在JSON实体映射时非常重要，比如fastJson
-keepattributes *Annotation*

# 避免混淆泛型，这在JSON实体映射时非常重要，比如fastJson
-keepattributes Signature

# 抛出异常时保留代码行号，在异常分析中可以方便定位
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# 保留所有的本地native方法不被混淆
-keepclasseswithmembernames class * {
    native <methods>;
}

-ignorewarnings

# Activity,Application, Service, BroadcastReceiver, ContentProvider,BackupAgentHelper,Preference,ILicensingService 的子类不被混淆
# 因为它们在Manifest中声明，Android framework需要通过反射读取它们
#-keep public class * extends android.app.Activity
#-keep public class * extends android.app.Application
#-keep public class * extends android.app.Service
#-keep public class * extends android.content.BroadcastReceiver
#-keep public class * extends android.content.ContentProvider
#-keep public class * extends android.app.backup.BackupAgentHelper
#-keep public class * extends android.preference.Preference
#-keep public class * extends android.view.View
#-keep public class com.android.vending.licensing.ILicensingService
#-keep public class * extends android.app.Fragment

# 引用android-support-v4.jar包
-dontwarn android.support.**
-keep public class * extends android.support.v4.**
-keep class android.support.** { *; }
-keep interface android.support.v4.app.** { *; }

-keep class android.os.**{*;}

# 保留自定义控件（继承自View）不被混淆
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    *** get*();
    void set*(***);
}

-keepclassmembers class * implements android.os.Parcelable {
    static android.os.Parcelable$Creator CREATOR;
}

# 自定义控件类不被混淆
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

## 社区加载自定义动画不被混淆
#-keep class com.wang.avi.Indicator { *; }
#-keep public class * extends com.wang.avi.Indicator { *; }

# R文件
-keep class **.R$* {*;}
-keepclassmembers class **.R$* {
    public static <fields>;
}

# 对于带有回调函数onXXEvent的，不能被混淆
-keepclassmembers class * {
    void *(**On*Event);
}

-assumenosideeffects class android.util.Log {
    public static *** e(...);
    public static *** w(...);
    public static *** wtf(...);
    public static *** d(...);
    public static *** v(...);
}

-keepclasseswithmembers class * {
    native <methods>;
}

-keepclassmembers class * {
    public void *ButtonClicked(android.view.View);
}




#保持Activity类的子类成员
# 保留在Activity中的方法参数是view的方法，
# 从而我们在layout里面编写onClick就不会被影响
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}

# 保持枚举 enum 类不被混淆
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保持 Parcelable 不被混淆
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

#序列化
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# add this
-dontwarn android.net.**
-keep class android.net.SSLCertificateSocketFactory {*;}

################annotation###############
-keep class android.support.annotation.** { *; }
-keep interface android.support.annotation.** { *; }

##************************************************************
##****************第三方框架***********************************
##************************************************************

# Bugly
-dontwarn com.tencent.bugly.**
-keep class com.tencent.bugly.** {*;}

## Keep glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# Dagger2
-dontwarn com.google.errorprone.annotations.*

################espresso###############
-keep class android.support.test.espresso.** { *; }
-keep interface android.support.test.espresso.** { *; }

# FastJson
-dontwarn com.alibaba.fastjson.**
-keep class com.alibaba.fastjson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Jackson
-dontwarn org.codehaus.jackson.**
-dontwarn com.fasterxml.jackson.databind.**
-keep class org.codehaus.jackson.** { *;}
-keep class com.fasterxml.jackson.** { *; }
-keepattributes *Annotation*,EnclosingMethod,Signature
-keepclassmembers public final enum com.fasterxml.jackson.annotation.JsonAutoDetect$Visibility {
        public static final com.fasterxml.jackson.annotation.JsonAutoDetect$Visibility *;
}

# OkHttp3
-dontwarn okhttp3.logging.**
-keep class okhttp3.internal.**{*;}
-dontwarn okio.**
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.ParametersAreNonnullByDefault
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
# JSR 305 annotations are for embedding nullability information.
-dontwarn javax.annotation.**
# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
# Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
-dontwarn org.codehaus.mojo.animal_sniffer.*
# OkHttp platform used only on JVM and when Conscrypt dependency is available.
-dontwarn okhttp3.internal.platform.ConscryptPlatform

#RxJava and RxAndroid
-dontwarn org.mockito.**
-dontwarn org.junit.**
-dontwarn org.robolectric.**

-keep class io.reactivex.** { *; }
-keep interface io.reactivex.** { *; }

-keepattributes Signature
-keepattributes *Annotation*
-keep class com.squareup.okhttp.** { *; }
-dontwarn okio.**
-keep interface com.squareup.okhttp.** { *; }
-dontwarn com.squareup.okhttp.**
-dontwarn io.reactivex.**
-dontwarn retrofit.**
-keep class retrofit.** { *; }
-keepclasseswithmembers class * {
    @retrofit.http.* <methods>;
}
-keep class sun.misc.Unsafe { *; }
-dontwarn java.lang.invoke.*
-keep class io.reactivex.schedulers.Schedulers {
    public static <methods>;
}
-keep class io.reactivex.schedulers.ImmediateScheduler {
    public <methods>;
}
-keep class io.reactivex.schedulers.TestScheduler {
    public <methods>;
}
-keep class io.reactivex.schedulers.Schedulers {
    public static ** test();
}
-keepclassmembers class io.reactivex.internal.util.unsafe.*ArrayQueue*Field* {
    long producerIndex;
    long consumerIndex;
}
-keepclassmembers class io.reactivex.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
    long producerNode;
    long consumerNode;
}
-keepclassmembers class io.reactivex.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
    io.reactivex.internal.util.atomic.LinkedQueueNode producerNode;
}
-keepclassmembers class io.reactivex.internal.util.unsafe.BaseLinkedQueueConsumerNodeRef {
    io.reactivex.internal.util.atomic.LinkedQueueNode consumerNode;
}
-dontwarn io.reactivex.internal.util.unsafe.**

# OrmLite
-dontwarn com.j256.**
-keepattributes *DatabaseField*
-keepattributes *DatabaseTable*
-keepattributes *SerializedName*
-keep class com.j256.**
-keepclassmembers class com.j256.** { *; }
-keep enum com.j256.**
-keepclassmembers enum com.j256.** { *; }
-keep interface com.j256.**
-keepclassmembers interface com.j256.** { *; }
-keepclassmembers class * {
@com.j256.ormlite.field.DatabaseField *;
}

#bouncycastle
-dontwarn javax.naming.**

#leancloud
-dontwarn com.avos.avoscloud.okio.**
-keep public class com.avos.avoscloud.okio.DeflaterSink{ *; }
-keep public class com.avos.avoscloud.okio.Okio{ *; }

#ETH
-keep class org.ethereum.geth.** { *; }

#密码强度
-keep class com.nulabinc.zxcvbn.** {*;}

#rxBus
-keep class com.quarkonium.qpocket.rx.** { *; }
-keep interface com.quarkonium.qpocket.rx.** { *; }
-keepclassmembers class * {
    @com.quarkonium.qpocket.rx.Subscribe <methods>;
}
-keepattributes *Annotation*

# zxing
-dontwarn com.google.zxing.common.BitMatrix

#钱包数据
-keep class com.quarkonium.qpocket.crypto.WalletFile{ *; }
-keep class com.quarkonium.qpocket.crypto.WalletFile$Crypto{ *; }
-keep class com.quarkonium.qpocket.crypto.WalletFile$CipherParams{ *; }
-keep class com.quarkonium.qpocket.crypto.WalletFile$Aes128CtrKdfParams{ *; }
-keep class com.quarkonium.qpocket.crypto.WalletFile$ScryptKdfParams{ *; }
-keep class com.quarkonium.qpocket.crypto.WalletFile$KdfParamsDeserialiser{ *; }
-keep interface com.quarkonium.qpocket.crypto.WalletFile$KdfParams{ *; }

#leancloud
-keepattributes Signature
-dontwarn com.jcraft.jzlib.**
-keep class com.jcraft.jzlib.**  { *;}
-dontwarn sun.misc.**
-keep class sun.misc.** { *;}
-dontwarn com.alibaba.fastjson.**
-keep class com.alibaba.fastjson.** { *;}
-dontwarn sun.security.**
-keep class sun.security.** { *; }
-dontwarn com.google.**
-keep class com.google.** { *;}
-dontwarn com.avos.**
-keep class com.avos.** { *;}
-keep public class android.net.http.SslError
-keep public class android.webkit.WebViewClient
-dontwarn android.webkit.WebView
-dontwarn android.net.http.SslError
-dontwarn android.webkit.WebViewClient
-dontwarn android.support.**
-dontwarn org.apache.**
-keep class org.apache.** { *;}
-dontwarn org.jivesoftware.smack.**
-keep class org.jivesoftware.smack.** { *;}
-dontwarn com.loopj.**
-keep class com.loopj.** { *;}
-dontwarn com.squareup.okhttp.**
-keep class com.squareup.okhttp.** { *;}
-keep interface com.squareup.okhttp.** { *; }
-dontwarn okio.**
-dontwarn org.xbill.**
-keep class org.xbill.** { *;}
-keepattributes *Annotation*

# To prevent cases of reflection causing issues
-keepattributes InnerClasses

#web3j
-keep class com.quarkonium.qpocket.jsonrpc.protocol.core.Request{ *;}
-keep class com.quarkonium.qpocket.jsonrpc.protocol.core.Response{ *;}
-keep class com.quarkonium.qpocket.abi.datatypes.Function{ *;}
-keep class com.quarkonium.qpocket.jsonrpc.protocol.methods.** { *; }
-keep class com.quarkonium.qpocket.jsonrpc.protocol.methods.request.CallRequest{ *;}
-keep class com.quarkonium.qpocket.jsonrpc.protocol.methods.request.TokenBalance{ *;}

#打点
-dontwarn com.tendcloud.tenddata.**
-keep class com.tendcloud.** {*;}
-keep public class com.tendcloud.tenddata.** { public protected *;}
-keepclassmembers class com.tendcloud.tenddata.**{
public void *(***);
}
-keep class com.talkingdata.sdk.TalkingDataSDK {public *;}
-keep class com.apptalkingdata.** {*;}
-keep class dice.** {*; }
-dontwarn dice.**

#js 混淆
-keepattributes *Annotation*
-keepattributes *JavascriptInterface*
-keepclassmembers class * {
   @android.webkit.JavascriptInterface <methods>;
}

#java注入类
-keep class com.just.agentweb.** {
    *;
}
-dontwarn com.just.agentweb.**
-keepclassmembers class com.dapp.bean.SignCallbackJSInterface{ *; }

#友盟
-keep class com.umeng.** {*;}
-keepclassmembers class * {
   public <init> (org.json.JSONObject);
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keep public class com.quarkonium.qpocket.R$*{
    public static final int *;
}
#友盟推送
-dontwarn com.umeng.**
-dontwarn com.taobao.**
-dontwarn anet.channel.**
-dontwarn anetwork.channel.**
-dontwarn org.android.**
-dontwarn org.apache.thrift.**
-dontwarn com.xiaomi.**
-dontwarn com.huawei.**
-dontwarn com.meizu.**

-keepattributes *Annotation*

-keep class com.taobao.** {*;}
-keep class org.android.** {*;}
-keep class anet.channel.** {*;}
-keep class com.umeng.** {*;}
-keep class com.xiaomi.** {*;}
-keep class com.huawei.** {*;}
-keep class com.meizu.** {*;}
-keep class org.apache.thrift.** {*;}

-keep class com.alibaba.sdk.android.**{*;}
-keep class com.ut.**{*;}
-keep class com.ta.**{*;}

-keep public class **.R$*{
   public static final int *;
}

#数据库不混淆
-keep class com.quarkonium.qpocket.api.db.table.** { *; }

#tron钱包数据
-keep class com.quarkonium.qpocket.tron.keystore.WalletFile{ *; }
-keep class com.quarkonium.qpocket.tron.keystore.WalletFile$Crypto{ *; }
-keep class com.quarkonium.qpocket.tron.keystore.WalletFile$CipherParams{ *; }
-keep class com.quarkonium.qpocket.tron.keystore.WalletFile$Aes128CtrKdfParams{ *; }
-keep class com.quarkonium.qpocket.tron.keystore.WalletFile$ScryptKdfParams{ *; }
-keep class com.quarkonium.qpocket.tron.keystore.WalletFile$KdfParamsDeserialiser{ *; }
-keep interface com.quarkonium.qpocket.tron.keystore.WalletFile$KdfParams{ *; }

#tron私钥
-keep class com.quarkonium.qpocket.tron.keystore.ECKeyFactory{ *; }
-keep class com.quarkonium.qpocket.tron.keystore.ECKeyFactory$Holder{ *; }
-keep class com.quarkonium.qpocket.tron.crypto.TronCastleProvider{ *; }
-keep class com.quarkonium.qpocket.tron.crypto.TronCastleProvider$Holder{ *; }
-keep class com.quarkonium.qpocket.tron.crypto.cryptohash.Keccak256{ *; }
-keep class com.quarkonium.qpocket.tron.crypto.cryptohash.Keccak512{ *; }

#spongycastle
-keep class org.spongycastle.**
-dontwarn org.spongycastle.jce.provider.X509LDAPCertStoreSpi
-dontwarn org.spongycastle.x509.util.LDAPStoreHelper

#trc20
-keep class com.quarkonium.qpocket.tron.TrxRequest{ *; }

#cookie
-dontwarn com.franmontiel.persistentcookiejar.**
-keep class com.franmontiel.persistentcookiejar.**
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

#网址信息拉取
-dontwarn org.jsoup.**
-keep class org.jsoup.**{*;}

#eventBus
-keepattributes *Annotation*
-keepclassmembers class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# Only required if you use AsyncExecutor
-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}

#dapp java bean混淆
-keep class com.dapp.bean.SignCallbackJSInterface$TrustProviderTypedData{ *; }

#微信分享
-keep class com.tencent.mm.opensdk.** {
    *;
}

-keep class com.tencent.wxop.** {
    *;
}

-keep class com.tencent.mm.sdk.** {
    *;
}

#gas手续费javaBean
-keep class com.quarkonium.qpocket.model.transaction.bean.EthGas{ *; }

#bitcoin钱包数据
-keep class com.quarkonium.qpocket.btc.keystore.Keystore{ *; }
-keep class com.quarkonium.qpocket.btc.keystore.WalletKeystore{ *; }
-keep class com.quarkonium.qpocket.btc.keystore.IMTKeystore{ *; }
-keep class com.quarkonium.qpocket.btc.keystore.V3Keystore{ *; }
-keep class com.quarkonium.qpocket.btc.keystore.Metadata{ *; }
-keep interface com.quarkonium.qpocket.btc.keystore.ExportableKeystore{ *; }

-keep class com.quarkonium.qpocket.btc.crypto.Crypto{ *; }
-keep class com.quarkonium.qpocket.btc.crypto.PBKDF2Crypto{ *; }
-keep class com.quarkonium.qpocket.btc.crypto.SCryptCrypto{ *; }

-keep interface com.quarkonium.qpocket.btc.crypto.KDFParams{ *; }
-keep class com.quarkonium.qpocket.btc.crypto.PBKDF2Params{ *; }
-keep class com.quarkonium.qpocket.btc.crypto.SCryptParams{ *; }
-keep class com.quarkonium.qpocket.btc.crypto.CipherParams{ *; }


#androidX
-keep class com.google.android.material.** {*;}
-keep class androidx.** {*;}
-keep public class * extends androidx.**
-keep interface androidx.** {*;}
-dontwarn com.google.android.material.**
-dontnote com.google.android.material.**
-dontwarn androidx.**

-keep class androidx.annotation.** { *; }
-keep interface androidx.annotation.** { *; }

-keep class androidx.test.espresso.** { *; }
-keep interface androidx.test.espresso.** { *; }

#glide
-keep public class * implements com.bumptech.glide.module.AppGlideModule
-keep public class * implements com.bumptech.glide.module.LibraryGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Basic ProGuard rules for Firebase Android SDK 2.0.0+
-keep class com.firebase.** { *; }
-keep class org.apache.** { *; }
-keepnames class com.fasterxml.jackson.** { *; }
-keepnames class javax.servlet.** { *; }
-keepnames class org.ietf.jgss.** { *; }
-dontwarn org.apache.**
-dontwarn org.w3c.dom.**

################React Native相关混淆 START##########################
# React Native

# Keep our interfaces so they can be used by other ProGuard rules.
# See http://sourceforge.net/p/proguard/bugs/466/
-keep,allowobfuscation @interface com.facebook.proguard.annotations.DoNotStrip
-keep,allowobfuscation @interface com.facebook.proguard.annotations.KeepGettersAndSetters
-keep,allowobfuscation @interface com.facebook.common.internal.DoNotStrip
-keep,allowobfuscation @interface com.facebook.jni.annotations.DoNotStrip

# Do not strip any method/class that is annotated with @DoNotStrip
-keep @com.facebook.proguard.annotations.DoNotStrip class *
-keep @com.facebook.common.internal.DoNotStrip class *
-keep @com.facebook.jni.annotations.DoNotStrip class *
-keepclassmembers class * {
    @com.facebook.proguard.annotations.DoNotStrip *;
    @com.facebook.common.internal.DoNotStrip *;
    @com.facebook.jni.annotations.DoNotStrip *;
}

-keepclassmembers @com.facebook.proguard.annotations.KeepGettersAndSetters class * {
  void set*(***);
  *** get*();
}

-keep class * implements com.facebook.react.bridge.JavaScriptModule { *; }
-keep class * implements com.facebook.react.bridge.NativeModule { *; }
-keepclassmembers,includedescriptorclasses class * { native <methods>; }
-keepclassmembers class *  { @com.facebook.react.uimanager.annotations.ReactProp <methods>; }
-keepclassmembers class *  { @com.facebook.react.uimanager.annotations.ReactPropGroup <methods>; }

-dontwarn com.facebook.react.**
-keep,includedescriptorclasses class com.facebook.react.bridge.** { *; }

-keep class com.facebook.react.devsupport.DevSupportManagerImpl { *; }
-keep interface com.facebook.react.devsupport.interfaces.DevSupportManager { *; }
-keep class com.quarkonium.qpocket.btc.bean.UTXOTransaction  { *; }

# hermes
-keep class com.facebook.jni.** { *; }

# okio
-keep class sun.misc.Unsafe { *; }
-dontwarn java.nio.file.*
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn okio.**

-keep class com.quarkonium.qpocket.model.ledger.LedgerModule { *; }
-keep class com.polidea.reactnativeble.** { *; }
