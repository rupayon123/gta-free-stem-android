# Keep Kotlin serialization-generated serializers used by the public feed.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$$serializer INSTANCE;
}
-keep,includedescriptorclasses class **$$serializer { *; }
-keepclassmembers class ** {
    *** Companion;
}
# WorkManager persists this worker's class name and creates it reflectively.
-keep class com.rupayonhaldar.gtafreestem.platform.alerts.OpportunityAlertWorker { <init>(...); }
