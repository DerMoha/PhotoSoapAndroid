# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.photosoap.android.data.remote.dto.** { *; }

# Kotlinx Serialization
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.photosoap.android.**$$serializer { *; }
-keepclassmembers class com.photosoap.android.** {
    *** Companion;
}
-keepclasseswithmembers class com.photosoap.android.** {
    kotlinx.serialization.KSerializer serializer(...);
}
