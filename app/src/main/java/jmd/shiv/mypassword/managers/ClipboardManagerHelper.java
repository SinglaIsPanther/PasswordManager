package jmd.shiv.mypassword.managers;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;

/**
 * यह क्लास क्लिपबोर्ड प्रबंधन और पासवर्ड को कॉपी करने के बाद 
 * स्वचालित रूप से क्लिपबोर्ड साफ़ करने की सुविधा देती है।
 */
public class ClipboardManagerHelper {

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static Runnable clearClipboardRunnable = null;
    private static final long DEFAULT_CLEAR_DELAY_MS = 30000; // 30 सेकंड बाद साफ़

    /**
     * संवेदनशील टेक्स्ट (पासवर्ड/यूजरनेम) को क्लिपबोर्ड में कॉपी करता है 
     * और निर्धारित समय बाद उसे साफ़ करने का टाइमर प्रारंभ करता है।
     *
     * @param context   एप्लीकेशन संदर्भ
     * @param label     कॉपी किए गए डेटा का शीर्षक
     * @param text      कॉपी किया जाने वाला टेक्स्ट
     * @param autoClear क्या 30 सेकंड बाद क्लिपबोर्ड स्वतः साफ़ करना है
     */
    public static void copyToClipboard(Context context, String label, String text, boolean autoClear) {
        if (context == null || text == null) {
            return;
        }

        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            return;
        }

        ClipData clip = ClipData.newPlainText(label, text);

        // Android 13+ (API 33) पर क्लिपबोर्ड ओवरले प्रिव्यू को संवेदनशील चिह्नित करें ताकि स्क्रीन पर न दिखे
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PersistableBundle extras = new PersistableBundle();
            extras.putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true);
            clip.getDescription().setExtras(extras);
        }

        clipboard.setPrimaryClip(clip);

        if (autoClear) {
            // पूर्व टाइमर को रद्द करें
            if (clearClipboardRunnable != null) {
                mainHandler.removeCallbacks(clearClipboardRunnable);
            }

            clearClipboardRunnable = () -> {
                try {
                    ClipData emptyClip = ClipData.newPlainText("", "");
                    clipboard.setPrimaryClip(emptyClip);
                } catch (Exception ignored) {
                }
            };

            mainHandler.postDelayed(clearClipboardRunnable, DEFAULT_CLEAR_DELAY_MS);
        }
    }
}
