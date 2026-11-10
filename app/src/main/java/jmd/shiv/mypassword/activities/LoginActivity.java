package jmd.shiv.mypassword.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.concurrent.Executor;

import jmd.shiv.mypassword.R;
import jmd.shiv.mypassword.database.PasswordRepository;
import jmd.shiv.mypassword.models.AppSettings;
import jmd.shiv.mypassword.security.AuthenticationManager;
import jmd.shiv.mypassword.security.KeyStoreHelper;
import jmd.shiv.mypassword.server.WebServerController;

/**
 * यह एक्टिविटी मास्टर पासवर्ड और बायोमेट्रिक ऑथेंटिकेशन के माध्यम से 
 * तिजोरी को अनलॉक करने का कार्य करती है।
 */
public class LoginActivity extends BaseSecurityActivity {

    private TextInputLayout layoutPassword;
    private TextInputEditText editPassword;
    private MaterialButton btnUnlock;
    private MaterialButton btnBiometric;
    private TextView textLockoutWarning;
    private TextView textForgotPassword;

    private final Handler countdownHandler = new Handler(Looper.getMainLooper());
    private Runnable countdownRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        applyEdgeToEdgeInsets(findViewById(R.id.layout_login_root));

        initializeViews();
        setupClickListeners();
        checkBiometricAvailability();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkLockoutStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
        }
    }

    private void initializeViews() {
        layoutPassword = findViewById(R.id.input_layout_login_password);
        editPassword = findViewById(R.id.edit_login_password);
        btnUnlock = findViewById(R.id.btn_login_unlock);
        btnBiometric = findViewById(R.id.btn_login_biometric);
        textLockoutWarning = findViewById(R.id.text_login_lockout_warning);
        textForgotPassword = findViewById(R.id.text_forgot_password);
    }

    private void setupClickListeners() {
        btnUnlock.setOnClickListener(v -> processMasterPasswordLogin());
        btnBiometric.setOnClickListener(v -> showBiometricPrompt());
        textForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    /**
     * जांचता है कि क्या बायोमेट्रिक हार्डवेयर उपलब्ध है और सेटिंग्स में सक्षम किया गया है।
     */
    private void checkBiometricAvailability() {
        PasswordRepository repository = new PasswordRepository(this);
        AppSettings settings = repository.getAppSettings();

        boolean isHardwareAvailable = AuthenticationManager.getInstance().isBiometricAvailable(this);
        boolean isEnabledInSettings = settings.getBiometricEnabled() == 1;

        if (isHardwareAvailable && isEnabledInSettings) {
            btnBiometric.setVisibility(View.VISIBLE);
            // यूज़र जब बायोमेट्रिक बटन पर क्लिक करेगा तभी प्रॉम्प्ट खुलेगा
        } else {
            btnBiometric.setVisibility(View.GONE);
        }
    }

    /**
     * रेट लिमिटिंग (लगातार गलत पासवर्ड डालने पर अस्थायी रोक) की स्थिति जांचना।
     */
    private void checkLockoutStatus() {
        int remainingSeconds = AuthenticationManager.getInstance().getRemainingLockoutSeconds();
        if (remainingSeconds > 0) {
            btnUnlock.setEnabled(false);
            btnBiometric.setEnabled(false);
            textLockoutWarning.setVisibility(View.VISIBLE);
            updateLockoutCountdown(remainingSeconds);
        } else {
            btnUnlock.setEnabled(true);
            btnBiometric.setEnabled(true);
            textLockoutWarning.setVisibility(View.GONE);
        }
    }

    private void updateLockoutCountdown(int seconds) {
        if (seconds <= 0) {
            checkLockoutStatus();
            return;
        }

        textLockoutWarning.setText(getString(R.string.msg_rate_limit, seconds));
        countdownRunnable = () -> updateLockoutCountdown(seconds - 1);
        countdownHandler.postDelayed(countdownRunnable, 1000);
    }

    /**
     * मास्टर पासवर्ड के आधार पर लॉगिन का प्रयास।
     */
    private void processMasterPasswordLogin() {
        String password = editPassword.getText() != null ? editPassword.getText().toString().trim() : "";
        layoutPassword.setError(null);

        if (password.isEmpty()) {
            layoutPassword.setError("कृपया मास्टर पासवर्ड दर्ज करें!");
            editPassword.requestFocus();
            return;
        }

        boolean success = AuthenticationManager.getInstance().verifyMasterPassword(this, password);
        if (success) {
            editPassword.setText("");
            navigateToDashboard();
        } else {
            int remaining = AuthenticationManager.getInstance().getRemainingLockoutSeconds();
            if (remaining > 0) {
                checkLockoutStatus();
            } else {
                layoutPassword.setError(getString(R.string.msg_wrong_password));
            }
        }
    }

    /**
     * Android BiometricPrompt के माध्यम से फिंगरप्रिंट ऑथेंटिकेशन।
     */
    private void showBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                boolean unlocked = AuthenticationManager.getInstance().unlockWithBiometric();
                if (unlocked) {
                    navigateToDashboard();
                } else {
                    Toast.makeText(LoginActivity.this, "बायोमेट्रिक से कुंजी लोड करने में त्रुटि!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                // यूज़र ने कैंसिल किया या कोई त्रुटि
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("बायोमेट्रिक लॉगिन")
                .setSubtitle("तिजोरी अनलॉक करने के लिए अपनी फिंगरप्रिंट लगाएं")
                .setNegativeButtonText("पासवर्ड का उपयोग करें")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void navigateToDashboard() {
        // मोबाइल पासवर्ड लॉगिन होने पर प्रत्येक बार नया सुरक्षित रैंडम पोर्ट जनरेट करना
        WebServerController.getInstance().generateNewPort(this);

        Intent intent = new Intent(this, DashboardActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * मास्टर पासवर्ड भूल जाने की स्थिति में सुरक्षा प्रश्न के माध्यम से सुरक्षित पासवर्ड रीसेट।
     * इससे यूज़र का कोई भी सहेजा गया पासवर्ड या डेटा डिलीट नहीं होता।
     */
    private void showForgotPasswordDialog() {
        if (AuthenticationManager.getInstance().hasSecurityQuestion(this)) {
            showSecurityQuestionResetDialog();
        } else {
            showNoSecurityQuestionDialog();
        }
    }

    /**
     * सुरक्षा प्रश्न और उत्तर के माध्यम से नया मास्टर पासवर्ड बनाने का संवाद।
     * सहेजे गए सभी पासवर्ड 100% सुरक्षित रहते हैं।
     */
    private void showSecurityQuestionResetDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_forgot_password, null);

        TextView textQuestion = dialogView.findViewById(R.id.text_dialog_security_question);
        TextInputLayout layoutAnswer = dialogView.findViewById(R.id.layout_dialog_security_answer);
        TextInputEditText editAnswer = dialogView.findViewById(R.id.edit_dialog_security_answer);
        TextInputLayout layoutNewPassword = dialogView.findViewById(R.id.layout_dialog_new_password);
        TextInputEditText editNewPassword = dialogView.findViewById(R.id.edit_dialog_new_password);
        TextInputLayout layoutConfirmPassword = dialogView.findViewById(R.id.layout_dialog_confirm_password);
        TextInputEditText editConfirmPassword = dialogView.findViewById(R.id.edit_dialog_confirm_password);

        String question = AuthenticationManager.getInstance().getSecurityQuestion(this);
        textQuestion.setText(question != null && !question.isEmpty() ? question : "आपका सुरक्षा प्रश्न");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("पासवर्ड रीसेट करें", null)
                .setNegativeButton("रद्द करें", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnPositive.setOnClickListener(v -> {
                String answer = editAnswer.getText() != null ? editAnswer.getText().toString().trim() : "";
                String newPass = editNewPassword.getText() != null ? editNewPassword.getText().toString().trim() : "";
                String confirmPass = editConfirmPassword.getText() != null ? editConfirmPassword.getText().toString().trim() : "";

                layoutAnswer.setError(null);
                layoutNewPassword.setError(null);
                layoutConfirmPassword.setError(null);

                if (answer.isEmpty()) {
                    layoutAnswer.setError("कृपया सुरक्षा उत्तर दर्ज करें!");
                    editAnswer.requestFocus();
                    return;
                }

                if (newPass.length() < 6) {
                    layoutNewPassword.setError("मास्टर पासवर्ड कम से कम 6 अक्षरों का होना चाहिए!");
                    editNewPassword.requestFocus();
                    return;
                }

                if (!newPass.equals(confirmPass)) {
                    layoutConfirmPassword.setError("दोनों पासवर्ड मेल नहीं खाते!");
                    editConfirmPassword.requestFocus();
                    return;
                }

                boolean success = AuthenticationManager.getInstance().resetMasterPasswordWithAnswer(this, answer, newPass);
                if (success) {
                    dialog.dismiss();
                    Toast.makeText(this, "मास्टर पासवर्ड सफलतापूर्वक बदल दिया गया! आपका समस्त डेटा सुरक्षित है।", Toast.LENGTH_LONG).show();
                    navigateToDashboard();
                } else {
                    layoutAnswer.setError("सुरक्षा उत्तर गलत है! कृपया सही उत्तर दर्ज करें।");
                    editAnswer.requestFocus();
                }
            });
        });

        dialog.show();
    }

    /**
     * यदि कोई सुरक्षा प्रश्न पंजीकृत नहीं है, तो यूज़र को सूचना व सुरक्षित विकल्प प्रदान करना।
     */
    private void showNoSecurityQuestionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("सुरक्षा प्रश्न उपलब्ध नहीं है")
                .setMessage("इस तिजोरी के लिए कोई सुरक्षा प्रश्न व उत्तर सेट नहीं किया गया है।\n\nजीरो-नॉलेज एन्क्रिप्शन के कारण बिना मास्टर पासवर्ड अथवा सुरक्षा उत्तर के तिजोरी नहीं खोली जा सकती।\n\nयदि आप मास्टर पासवर्ड बिल्कुल याद नहीं कर पा रहे हैं, तो केवल डेटा रीसेट करके ही नया सेटअप किया जा सकता है।\n(चेतावनी: इससे सहेजा गया पुराना डेटा मिट जाएगा)")
                .setIcon(R.drawable.ic_shield_lock)
                .setPositiveButton("डेटा रीसेट और नया सेटअप", (dialog, which) -> {
                    PasswordRepository repository = new PasswordRepository(this);
                    repository.resetAllData();
                    KeyStoreHelper.deleteMasterKey();
                    AuthenticationManager.getInstance().logout();

                    Toast.makeText(this, "डेटाबेस रीसेट कर दिया गया। नया सेटअप करें।", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(this, SetupActivity.class);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("वापस (पासवर्ड याद करें)", null)
                .show();
    }

    @Override
    protected boolean shouldEnforceSessionLock() {
        return false;
    }
}
