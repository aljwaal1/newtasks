# Smart Tasks release R8 rules.
# Android/Compose/Google Mobile Ads dependencies provide their own consumer rules.
# Keep metadata that improves crash deobfuscation while allowing R8 to optimize app code.
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod
