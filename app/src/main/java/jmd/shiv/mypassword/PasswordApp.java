package jmd.shiv.mypassword;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import jmd.shiv.mypassword.managers.AutoLockManager;

/**
 * मुख्य एप्लीकेशन क्लास जो ऐप के बैकग्राउंड/फोरग्राउंड चक्र और ऑटो-लॉक टाइमर 
 * की वैश्विक निगरानी करती है।
 */
public class PasswordApp extends Application implements Application.ActivityLifecycleCallbacks {

    private int activeActivityCount = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        if (activeActivityCount == 0) {
            // ऐप बैकग्राउंड से वापस फोरग्राउंड में आया
            AutoLockManager.getInstance().onAppForegrounded(getApplicationContext());
        }
        activeActivityCount++;
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        activeActivityCount--;
        if (activeActivityCount == 0) {
            // ऐप पूरी तरह से बैकग्राउंड में चला गया
            AutoLockManager.getInstance().onAppBackgrounded(getApplicationContext());
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}

    @Override
    public void onActivityResumed(@NonNull Activity activity) {}

    @Override
    public void onActivityPaused(@NonNull Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {}
}
