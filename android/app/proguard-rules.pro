# Proguard rules for Aetheria AI
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
