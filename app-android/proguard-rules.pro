# kotlinx.serialization: keep generated serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.app.platform.language.** {
  *** Companion;
}
-keepclasseswithmembers class com.app.platform.language.** {
  kotlinx.serialization.KSerializer serializer(...);
}

# Ktor references JVM classes that do not exist on Android
-dontwarn java.lang.management.**
-dontwarn org.slf4j.**
