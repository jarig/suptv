# Add project specific ProGuard rules here.
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep ExoPlayer classes
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep Ktor classes for networking
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Keep Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.suptv.**$$serializer { *; }
-keepclassmembers class com.suptv.** {
    *** Companion;
}
-keepclasseswithmembers class com.suptv.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Compose classes
-keep class androidx.compose.** { *; }
-keep class androidx.tv.** { *; }

# Keep models
-keep class com.suptv.shared.model.** { *; }
