package jmd.shiv.mypassword.managers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import jmd.shiv.mypassword.activities.LoginActivity;
import jmd.shiv.mypassword.database.PasswordRepository;
import jmd.shiv.mypassword.models.AppSettings;
import jmd.shiv.mypassword.security.AuthenticationManager;

/**
 * यह क्लास ऐप के बैकग्राउंड में जाने पर ऑटो-लॉक समय सीमा का परीक्षण 
 * और तिजोरी को स्वतः लॉक करने की प्रक्रिया को नियंत्रित करती है।
 */
public class AutoLockManager {

    private static AutoLockManager instance;
    private long backgroundTimestamp = 0;
    private boolean isAppInBackground = false;

    private AutoLockManager() {
    }

    public static synchronized AutoLockManager getInstance() {
        if (instance == null) {
            instance = new AutoLockManager();
        }
        return instance;
    }

    /**
     * जब एप्लीकेशन बैकग्राउंड में जाती है तो समय दर्ज करता है।
     */
    public void onAppBackgrounded(Context context) {
        isAppInBackground = true;
        backgroundTimestamp = SystemClock.elapsedRealtime();
    }

    /**
     * जब एप्लीकेशन फोरग्राउंड में वापस आती है तो समय की गणना करके आवश्यकता होने पर लॉक करता है।
     */
    public void onAppForegrounded(Context context) {
        if (!isAppInBackground) {
            return;
        }
        isAppInBackground = false;

        if (backgroundTimestamp == 0) {
            return;
        }

        PasswordRepository repository = new PasswordRepository(context);
        AppSettings settings = repository.getAppSettings();
        int lockMinutes = settings.getAutoLockMinutes();

        long elapsedMinutes = (SystemClock.elapsedRealtime() - backgroundTimestamp) / (60 * 1000);

        if (elapsedMinutes >= lockMinutes) {
            AuthenticationManager.getInstance().logout();
        }
    }

    /**
     * जांचता है कि क्या वर्तमान गतिविधि (Activity) को लॉक स्क्रीन पर पुनर्निर्देशित (Redirect) करना है।
     */
    public boolean checkAndEnforceLock(Activity activity) {
        if (!AuthenticationManager.getInstance().isUnlocked()) {
            Intent intent = new Intent(activity, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(intent);
            activity.finish();
            return true;
        }
        return false;
    }
}
