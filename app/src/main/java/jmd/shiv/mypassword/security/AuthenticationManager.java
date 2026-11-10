package jmd.shiv.mypassword.security;

import android.content.Context;
import android.os.SystemClock;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.util.concurrent.Executor;
import javax.crypto.SecretKey;

import jmd.shiv.mypassword.database.PasswordRepository;
import jmd.shiv.mypassword.models.SecurityData;

/**
 * यह क्लास ऑथेंटिकेशन प्रबंधन (मास्टर पासवर्ड सत्यापन, रेट लिमिटिंग डिले, 
 * बायोमेट्रिक लॉगिन और मेमोरी सुरक्षा) को नियंत्रित करती है।
 */
public class AuthenticationManager {

    private static AuthenticationManager instance;
    private boolean isUnlocked = false;
    private SecretKey activeMasterKey = null;

    // रेट लिमिटिंग चर (Rate Limiting Variables)
    private int failedAttemptsCount = 0;
    private long lockoutUntilTimestamp = 0;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MS = 30000; // 30 सेकंड डिले

    private AuthenticationManager() {
    }

    public static synchronized AuthenticationManager getInstance() {
        if (instance == null) {
            instance = new AuthenticationManager();
        }
        return instance;
    }

    /**
     * जांचता है कि क्या वर्तमान में तिजोरी खुली (Unlocked) है।
     */
    public boolean isUnlocked() {
        return isUnlocked && activeMasterKey != null;
    }

    /**
     * वर्तमान सक्रिय AES-256 मास्टर कुंजी प्राप्त करना।
     */
    public SecretKey getActiveMasterKey() {
        return activeMasterKey;
    }

    /**
     * जांचता है कि क्या रेट लिमिटिंग के कारण लॉगिन अस्थायी रूप से ब्लॉक है।
     *
     * @return शेष सेकंड (यदि ब्लॉक है), अन्यथा 0
     */
    public int getRemainingLockoutSeconds() {
        long now = SystemClock.elapsedRealtime();
        if (now < lockoutUntilTimestamp) {
            return (int) Math.ceil((lockoutUntilTimestamp - now) / 1000.0);
        }
        return 0;
    }

    /**
     * पहली बार मास्टर पासवर्ड सेटअप करता है।
     */
    public boolean setupMasterPassword(Context context, String masterPassword) {
        return setupMasterPassword(context, masterPassword, null, null);
    }

    /**
     * मास्टर पासवर्ड और सुरक्षा प्रश्न/उत्तर सेटअप करता है।
     *
     * @param context          एप्लीकेशन संदर्भ
     * @param masterPassword   मास्टर पासवर्ड
     * @param securityQuestion सुरक्षा प्रश्न (वैकल्पिक)
     * @param securityAnswer   सुरक्षा उत्तर (वैकल्पिक)
     * @return सत्य यदि सेटअप सफल हुआ
     */
    public boolean setupMasterPassword(Context context, String masterPassword, String securityQuestion, String securityAnswer) {
        if (masterPassword == null || masterPassword.length() < 6) {
            return false;
        }

        try {
            byte[] salt = HashUtils.generateSecureSalt(32);
            String hash = HashUtils.hashPasswordWithSalt(masterPassword, salt);

            SecurityData securityData = new SecurityData();
            securityData.setMasterPasswordHash(hash);
            securityData.setMasterPasswordSalt(salt);

            if (securityQuestion != null && !securityQuestion.trim().isEmpty() &&
                    securityAnswer != null && !securityAnswer.trim().isEmpty()) {
                byte[] ansSalt = HashUtils.generateSecureSalt(32);
                String ansHash = HashUtils.hashPasswordWithSalt(securityAnswer.trim().toLowerCase(), ansSalt);
                securityData.setSecurityQuestion(securityQuestion.trim());
                securityData.setSecurityAnswerHash(ansHash);
                securityData.setSecurityAnswerSalt(ansSalt);
            }

            PasswordRepository repository = new PasswordRepository(context);
            boolean saved = repository.saveSecurityData(securityData);

            if (saved) {
                this.activeMasterKey = KeyStoreHelper.getOrCreateMasterKey();
                this.isUnlocked = true;
                this.failedAttemptsCount = 0;
                this.lockoutUntilTimestamp = 0;
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    /**
     * पंजीकृत सुरक्षा प्रश्न प्राप्त करता है।
     */
    public String getSecurityQuestion(Context context) {
        PasswordRepository repository = new PasswordRepository(context);
        SecurityData data = repository.getSecurityData();
        return (data != null && data.getSecurityQuestion() != null) ? data.getSecurityQuestion() : null;
    }

    /**
     * जांचता है कि क्या सुरक्षा प्रश्न व उत्तर पहले से पंजीकृत है।
     */
    public boolean hasSecurityQuestion(Context context) {
        PasswordRepository repository = new PasswordRepository(context);
        SecurityData data = repository.getSecurityData();
        return data != null && data.getSecurityQuestion() != null && data.getSecurityAnswerHash() != null;
    }

    /**
     * सुरक्षा उत्तर का सत्यापन करता है।
     */
    public boolean verifySecurityAnswer(Context context, String enteredAnswer) {
        if (enteredAnswer == null || enteredAnswer.trim().isEmpty()) {
            return false;
        }
        PasswordRepository repository = new PasswordRepository(context);
        SecurityData data = repository.getSecurityData();
        if (data == null || data.getSecurityAnswerHash() == null || data.getSecurityAnswerSalt() == null) {
            return false;
        }

        return HashUtils.verifyPassword(
                enteredAnswer.trim().toLowerCase(),
                data.getSecurityAnswerSalt(),
                data.getSecurityAnswerHash()
        );
    }

    /**
     * सुरक्षा उत्तर का सत्यापन करके बिना डेटा खोए नया मास्टर पासवर्ड सेट करता है।
     * सभी सहेजे गए पासवर्ड 100% सुरक्षित रहते हैं।
     */
    public boolean resetMasterPasswordWithAnswer(Context context, String enteredAnswer, String newMasterPassword) {
        if (newMasterPassword == null || newMasterPassword.length() < 6) {
            return false;
        }

        if (!verifySecurityAnswer(context, enteredAnswer)) {
            return false;
        }

        try {
            PasswordRepository repository = new PasswordRepository(context);
            SecurityData data = repository.getSecurityData();
            if (data == null) {
                return false;
            }

            byte[] newSalt = HashUtils.generateSecureSalt(32);
            String newHash = HashUtils.hashPasswordWithSalt(newMasterPassword, newSalt);

            data.setMasterPasswordHash(newHash);
            data.setMasterPasswordSalt(newSalt);
            boolean updated = repository.saveSecurityData(data);

            if (updated) {
                this.activeMasterKey = KeyStoreHelper.getOrCreateMasterKey();
                this.isUnlocked = true;
                this.failedAttemptsCount = 0;
                this.lockoutUntilTimestamp = 0;
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    /**
     * मास्टर पासवर्ड का सत्यापन करता है और रेट लिमिटिंग लागू करता है।
     *
     * @param context        एप्लीकेशन संदर्भ
     * @param masterPassword यूज़र द्वारा इनपुट किया गया पासवर्ड
     * @return सत्य यदि पासवर्ड सही है, अन्यथा असत्य
     */
    public boolean verifyMasterPassword(Context context, String masterPassword) {
        if (getRemainingLockoutSeconds() > 0) {
            return false;
        }

        PasswordRepository repository = new PasswordRepository(context);
        SecurityData securityData = repository.getSecurityData();
        if (securityData == null) {
            return false;
        }

        boolean isValid = HashUtils.verifyPassword(
                masterPassword,
                securityData.getMasterPasswordSalt(),
                securityData.getMasterPasswordHash()
        );

        if (isValid) {
            try {
                this.activeMasterKey = KeyStoreHelper.getOrCreateMasterKey();
                this.isUnlocked = true;
                this.failedAttemptsCount = 0;
                this.lockoutUntilTimestamp = 0;
                return true;
            } catch (Exception e) {
                return false;
            }
        } else {
            failedAttemptsCount++;
            if (failedAttemptsCount >= MAX_FAILED_ATTEMPTS) {
                lockoutUntilTimestamp = SystemClock.elapsedRealtime() + LOCKOUT_DURATION_MS;
                failedAttemptsCount = 0; // टाइमर पूरा होने पर रीसेट
            }
            return false;
        }
    }

    /**
     * बायोमेट्रिक प्रमाणीकरण सफल होने पर सक्रिय कुंजी अनलॉक करना।
     */
    public boolean unlockWithBiometric() {
        try {
            this.activeMasterKey = KeyStoreHelper.getOrCreateMasterKey();
            this.isUnlocked = true;
            this.failedAttemptsCount = 0;
            this.lockoutUntilTimestamp = 0;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * डिवाइस पर बायोमेट्रिक हार्डवेयर की उपलब्धता की जांच करता है।
     */
    public boolean isBiometricAvailable(Context context) {
        BiometricManager biometricManager = BiometricManager.from(context);
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.BIOMETRIC_WEAK)
                == BiometricManager.BIOMETRIC_SUCCESS;
    }

    /**
     * मास्टर पासवर्ड बदलने का सुरक्षित कार्य।
     */
    public boolean changeMasterPassword(Context context, String oldPassword, String newPassword) {
        if (!verifyMasterPassword(context, oldPassword)) {
            return false;
        }
        if (newPassword == null || newPassword.length() < 6) {
            return false;
        }

        try {
            byte[] newSalt = HashUtils.generateSecureSalt(32);
            String newHash = HashUtils.hashPasswordWithSalt(newPassword, newSalt);

            PasswordRepository repository = new PasswordRepository(context);
            SecurityData securityData = repository.getSecurityData();
            if (securityData == null) {
                securityData = new SecurityData();
            }
            securityData.setMasterPasswordHash(newHash);
            securityData.setMasterPasswordSalt(newSalt);

            return repository.saveSecurityData(securityData);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * मास्टर पासवर्ड सत्यापित करके सुरक्षा प्रश्न और उत्तर को सेट या अपडेट करना।
     */
    public boolean updateSecurityQuestion(Context context, String masterPassword, String newQuestion, String newAnswer) {
        if (!verifyMasterPassword(context, masterPassword)) {
            return false;
        }
        try {
            PasswordRepository repository = new PasswordRepository(context);
            SecurityData securityData = repository.getSecurityData();
            if (securityData == null) {
                return false;
            }
            if (newQuestion != null && !newQuestion.trim().isEmpty() && newAnswer != null && !newAnswer.trim().isEmpty()) {
                byte[] ansSalt = HashUtils.generateSecureSalt(32);
                String ansHash = HashUtils.hashPasswordWithSalt(newAnswer.trim().toLowerCase(), ansSalt);
                securityData.setSecurityQuestion(newQuestion.trim());
                securityData.setSecurityAnswerHash(ansHash);
                securityData.setSecurityAnswerSalt(ansSalt);
            } else {
                securityData.setSecurityQuestion(null);
                securityData.setSecurityAnswerHash(null);
                securityData.setSecurityAnswerSalt(null);
            }
            return repository.saveSecurityData(securityData);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * यूज़र को सुरक्षित लॉगआउट करता है और मेमोरी से सभी संवेदनशील कुंजियों को हटाता है।
     */
    public void logout() {
        this.isUnlocked = false;
        this.activeMasterKey = null;
    }

    /**
     * तिजोरी को तुरंत लॉक करता है।
     */
    public void lockVault() {
        logout();
    }
}
