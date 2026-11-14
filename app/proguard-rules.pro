# Project-specific R8/ProGuard rules.
# Retrofit 2.9 inspects annotations and generic signatures for suspend API methods at runtime.
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault
# R8 full mode otherwise removes the generic Continuation signature used by Retrofit suspend calls.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

-keep class com.myiptv.clean.data.model.** { *; }
