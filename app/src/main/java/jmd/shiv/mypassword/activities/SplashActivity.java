package jmd.shiv.mypassword.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import jmd.shiv.mypassword.R;
import jmd.shiv.mypassword.database.PasswordRepository;

/**
 * यह एक्टिविटी ऐप की पहली स्क्रीन है, जो डेटाबेस की जांच करती है 
 * कि क्या मास्टर पासवर्ड पहले से सेटअप है अथवा नया सेटअप शुरू करना है।
 */
public class SplashActivity extends BaseSecurityActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        applyEdgeToEdgeInsets(findViewById(R.id.layout_splash_root));

        new Handler(Looper.getMainLooper()).postDelayed(this::checkVaultStatusAndNavigate, 600);
    }

    /**
     * तिजोरी की स्थिति जांचकर उपयुक्त स्क्रीन (SetupActivity या LoginActivity) पर भेजता है।
     */
    private void checkVaultStatusAndNavigate() {
        PasswordRepository repository = new PasswordRepository(this);
        Intent intent;
        if (repository.hasSecuritySetup()) {
            intent = new Intent(this, LoginActivity.class);
        } else {
            intent = new Intent(this, SetupActivity.class);
        }
        startActivity(intent);
        finish();
    }

    @Override
    protected boolean shouldEnforceSessionLock() {
        return false;
    }
}
