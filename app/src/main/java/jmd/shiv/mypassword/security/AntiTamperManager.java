package jmd.shiv.mypassword.security;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Debug;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;

/**
 * यह क्लास रिवर्स इंजीनियरिंग, डिबगिंग, फ्रिडा (Frida) हुकिंग, रूटेड डिवाइस 
 * और अनधिकृत छेड़छाड़ (Anti-Tampering) की रोकथाम के लिए बहुस्तरीय सुरक्षा इंजन प्रदान करती है।
 */
public class AntiTamperManager {

    private static final String[] KNOWN_ROOT_PATHS = {
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/vendor/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/data/local/su",
            "/system/app/Superuser.apk"
    };

    private static final String[] KNOWN_ROOT_PACKAGES = {
            "com.noshufou.android.su",
            "com.thirdparty.superuser",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.topjohnwu.magisk"
    };

    /**
     * जांचता है कि क्या कोई डिबगर (Debugger) वर्तमान प्रक्रिया से जुड़ा हुआ है।
     * केवल रिलीज़ बिल्ड में यह जांच की जाती है।
     *
     * @return सत्य यदि डिबगर संलग्न है
     */
    public static boolean isDebuggerAttached(Context context) {
        if (context == null) return false;

        // यदि ऐप डीबग मोड में है (डेवलपमेंट/परीक्षण), तो डीबगिंग सामान्य है
        boolean isDebuggable = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        if (isDebuggable) {
            return false;
        }

        // रिलीज़ बिल्ड में सक्रिय डीबगर की उपस्थिति जांचना
        if (Debug.isDebuggerConnected() || Debug.waitingForDebugger()) {
            return true;
        }

        return hasTracerPid();
    }

    /**
     * लिनक्स कर्नेल स्टेटस फ़ाइल पढ़कर जांचता है कि क्या कोई नेटिव प्रोसेस (जैसे GDB, IDA, Frida) ptrace से ट्रेस कर रही है।
     */
    private static boolean hasTracerPid() {
        try {
            File statusFile = new File("/proc/self/status");
            if (statusFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(statusFile)))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("TracerPid:")) {
                            String pidStr = line.substring("TracerPid:".length()).trim();
                            int tracerPid = Integer.parseInt(pidStr);
                            if (tracerPid > 0) {
                                return true;
                            }
                            break;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * जांचता है कि क्या प्रक्रिया की मेमोरी में Frida या Xposed जैसे डायनेमिक हुकिंग टूल इंजेक्ट किए गए हैं।
     *
     * @return सत्य यदि हुकिंग टूल का पता चलता है
     */
    public static boolean isHookingDetected() {
        // 1. /proc/self/maps मेमोरी मैपिंग में Frida लाइब्रेरीज़ की खोज
        try {
            File mapsFile = new File("/proc/self/maps");
            if (mapsFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(mapsFile)))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String lower = line.toLowerCase();
                        if (lower.contains("frida-gadget") ||
                                lower.contains("frida-agent") ||
                                lower.contains("libfrida") ||
                                lower.contains("xposedbridge.jar")) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // 2. XposedBridge क्लास की उपस्थिति की जांच
        try {
            Class.forName("de.robv.android.xposed.XposedBridge");
            return true;
        } catch (ClassNotFoundException ignored) {
        }

        return false;
    }

    /**
     * जांचता है कि क्या डिवाइस रूटेड (Rooted) अथवा सुपरयूज़र द्वारा समझौताग्रस्त है।
     *
     * @param context एप्लीकेशन संदर्भ
     * @return सत्य यदि रूट बाइनरी या ऐप मौजूद है
     */
    public static boolean isDeviceRooted(Context context) {
        // 1. सामान्य SU बाइनरी पाथ्स चेक
        for (String path : KNOWN_ROOT_PATHS) {
            try {
                if (new File(path).exists()) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }

        // 2. टेस्ट कीज (Test-Keys) बिल्ड टैग्स चेक
        String buildTags = Build.TAGS;
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true;
        }

        // 3. रूट मैनेजमेंट पैकेज चेक
        if (context != null) {
            PackageManager pm = context.getPackageManager();
            for (String pkg : KNOWN_ROOT_PACKAGES) {
                try {
                    pm.getPackageInfo(pkg, 0);
                    return true;
                } catch (PackageManager.NameNotFoundException ignored) {
                }
            }
        }

        return false;
    }

    /**
     * जांचता है कि क्या ऐप किसी एमुलेटर या वर्चुअल एनवायरनमेंट में चल रहा है।
     */
    public static boolean isEmulator() {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p");
    }

    /**
     * संवेदनशील डेटा (पासवर्ड, कीज) को मेमोरी से सुरक्षित मिटाने के लिए जीरो-आउट (Zeroing) करता है।
     */
    public static void wipeMemory(byte[] sensitiveBytes) {
        if (sensitiveBytes != null) {
            Arrays.fill(sensitiveBytes, (byte) 0);
        }
    }

    /**
     * संवेदनशील वर्णों (char[]) को मेमोरी से तुरंत शून्य करता है।
     */
    public static void wipeMemory(char[] sensitiveChars) {
        if (sensitiveChars != null) {
            Arrays.fill(sensitiveChars, '\0');
        }
    }

    private static boolean isDebugBuild() {
        return false;
    }
}
