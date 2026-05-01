# Default ProGuard rules
-keepattributes *Annotation*
-keepclassmembers class * extends android.content.BroadcastReceiver {
    public <methods>;
}
