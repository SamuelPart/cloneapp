# Add project specific ProGuard rules here.
-keep class com.google.android.exoplayer2.** { *; }
-dontwarn com.google.android.exoplayer2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keep class com.bumptech.glide.** { *; }