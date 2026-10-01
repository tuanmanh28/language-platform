# kotlinx.serialization giữ serializer được sinh ra
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.app.platform.language.** {
    *** Companion;
}
-keepclasseswithmembers class com.app.platform.language.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor tham chiếu class JVM không có trên Android
-dontwarn java.lang.management.**
-dontwarn org.slf4j.**
