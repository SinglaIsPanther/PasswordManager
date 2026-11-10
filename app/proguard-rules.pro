# ==============================================================================
# SECURE PASSWORD MANAGER - MAXIMUM ANTI-REVERSE-ENGINEERING & R8 OBFUSCATION
# Package: jmd.shiv.mypassword
# ==============================================================================

# 1. Bytecode Optimization & Aggressive Renaming
-optimizationpasses 5
-allowaccessmodification
-overloadaggressively
-repackageclasses ""
-flattenpackagehierarchy ""
-dontusemixedcaseclassnames

# 2. Stripping Source Information & Stacktrace Fingerprints
-renamesourcefileattribute ""
-keepattributes Exceptions,InnerClasses,Signature,*Annotation*

# 3. Strip all Logging statements in Release Build (Anti-Information Leakage)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int println(...);
}

# 4. Anti-Tamper & Security Engine preservation
-keep class jmd.shiv.mypassword.security.AntiTamperManager {
    public *;
}

# 5. Keep Data Models (Required for Serialization and Reflection)
-keep class jmd.shiv.mypassword.models.** { *; }

# 6. Preserve Native Methods (Keystore & System Callbacks)
-keepclasseswithmembernames class * {
    native <methods>;
}

# 7. Preserve Custom Views & Activities needed by AndroidManifest / XML
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# 8. NanoHTTPD Embedded Web Server rules
-keep class fi.iki.elonen.** { *; }
-dontwarn fi.iki.elonen.**
-dontwarn java.awt.**

# 9. ZXing QR Code Library rules
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# 10. Android Keystore & Cryptographic Providers
-keepclassmembers class * extends java.security.KeyStore {
    *;
}
-keepclassmembers class * extends javax.crypto.Cipher {
    *;
}
