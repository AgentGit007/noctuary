# Room generates implementations reflectively referenced at runtime.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**

# kotlinx.serialization ships its own consumer rules; these cover the
# generated serializers for our @Serializable backup models explicitly.
-keepclassmembers class io.github.agentgit007.noctuary.backup.** {
    *** Companion;
}
-keepclasseswithmembers class io.github.agentgit007.noctuary.backup.** {
    kotlinx.serialization.KSerializer serializer(...);
}
