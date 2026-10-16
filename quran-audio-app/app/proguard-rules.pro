# kotlinx.serialization: keep generated serializers for @Serializable classes (incl. navigation routes).
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class **$$serializer { *; }
-dontnote kotlinx.serialization.**

# Retrofit interfaces are accessed reflectively.
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# Network DTOs and Retrofit APIs are reached only through reflection (Retrofit proxies +
# kotlinx.serialization lookup by type). Keep them, their companions and generated serializers
# by name. The REA APK audit (.github/workflows/quran-apk-audit.yml) checks these survive R8.
-keep @kotlinx.serialization.Serializable class app.quranaudio.** { *; }
-keep class app.quranaudio.**$$serializer { *; }
-keep class app.quranaudio.**$Companion { *; }
-keep interface app.quranaudio.data.network.** { *; }
