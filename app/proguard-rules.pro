# Keep kotlinx.serialization generated serializers
-keepattributes *Annotation*, InnerClasses, Signature
-dontnote kotlinx.serialization.AnnotationsKt

# Serializable classes — companion + generated serializer lookup
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-keepclasseswithmembers class com.bepngon.merge.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable <methods>;
}
-keep,includedescriptorclasses class com.bepngon.merge.data.**$$serializer { *; }
-keepclasseswithmembers,includedescriptorclasses class com.bepngon.merge.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# DataStore preferences
-keep class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite { *; }
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}
