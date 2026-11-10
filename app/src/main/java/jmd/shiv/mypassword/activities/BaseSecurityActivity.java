package jmd.shiv.mypassword.activities;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import jmd.shiv.mypassword.managers.AutoLockManager;

/**
 * सभी संवेदनशील गतिविधियों (Activities) के लिए आधार क्लास।
 * यह स्क्रीन रिकॉर्डिंग/स्क्रीनशॉट रोकथाम (FLAG_SECURE), Edge-to-Edge 
 * और ऑटो-लॉक सुरक्षा को स्वचालित रूप से लागू करती है।
 */
public abstract class BaseSecurityActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // स्क्रीनशॉट और स्क्रीन रिकॉर्डिंग रोकथाम
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
        );
        super.onCreate(savedInstanceState);
    }

    /**
     * रूट लेआउट पर सुरक्षित पैडिंग (Insets) लागू करने का नियम।
     */
    protected void applyEdgeToEdgeInsets(View rootView) {
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // केवल रिलीज़ मोड में ही डिबगर या अनधिकृत हुकिंग का पता चलने पर तिजोरी लॉक करें
        boolean isDebuggable = (getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        if (!isDebuggable) {
            if (jmd.shiv.mypassword.security.AntiTamperManager.isHookingDetected() ||
                    jmd.shiv.mypassword.security.AntiTamperManager.isDebuggerAttached(this)) {
                jmd.shiv.mypassword.security.AuthenticationManager.getInstance().lockVault();
            }
        }

        if (shouldEnforceSessionLock()) {
            AutoLockManager.getInstance().checkAndEnforceLock(this);
        }
    }

    /**
     * उप-क्लास इस विधि को ओवरराइड करके सेशन लॉक की आवश्यकता नियंत्रित कर सकती हैं।
     */
    protected boolean shouldEnforceSessionLock() {
        return true;
    }
}
