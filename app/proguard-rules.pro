-keep class com.movieroulette.app.** { *; }
-keep class io.github.jan.supabase.** { *; }
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.movieroulette.app.**$$serializer { *; }
-keepclassmembers class com.movieroulette.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.movieroulette.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
