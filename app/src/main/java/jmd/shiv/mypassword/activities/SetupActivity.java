package jmd.shiv.mypassword.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import jmd.shiv.mypassword.R;
import jmd.shiv.mypassword.security.AuthenticationManager;
import jmd.shiv.mypassword.server.WebServerController;

/**
 * यह एक्टिविटी पहली बार ऐप शुरू होने पर मास्टर पासवर्ड की स्थापना (Initial Setup) कराती है।
 */
public class SetupActivity extends BaseSecurityActivity {

    private TextInputLayout layoutPassword;
    private TextInputLayout layoutConfirmPassword;
    private TextInputEditText editPassword;
    private TextInputEditText editConfirmPassword;
    private TextInputEditText editSecurityQuestion;
    private TextInputEditText editSecurityAnswer;
    private MaterialButton btnCreateVault;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        applyEdgeToEdgeInsets(findViewById(R.id.layout_setup_root));

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        layoutPassword = findViewById(R.id.input_layout_setup_password);
        layoutConfirmPassword = findViewById(R.id.input_layout_setup_confirm_password);
        editPassword = findViewById(R.id.edit_setup_password);
        editConfirmPassword = findViewById(R.id.edit_setup_confirm_password);
        editSecurityQuestion = findViewById(R.id.edit_setup_security_question);
        editSecurityAnswer = findViewById(R.id.edit_setup_security_answer);
        btnCreateVault = findViewById(R.id.btn_create_vault);
    }

    private void setupClickListeners() {
        btnCreateVault.setOnClickListener(v -> processVaultSetup());
    }

    /**
     * यूज़र द्वारा डाले गए मास्टर पासवर्ड का सत्यापन और तिजोरी निर्माण की प्रक्रिया।
     */
    private void processVaultSetup() {
        String password = editPassword.getText() != null ? editPassword.getText().toString().trim() : "";
        String confirmPassword = editConfirmPassword.getText() != null ? editConfirmPassword.getText().toString().trim() : "";
        String question = editSecurityQuestion != null && editSecurityQuestion.getText() != null 
                ? editSecurityQuestion.getText().toString().trim() : "";
        String answer = editSecurityAnswer != null && editSecurityAnswer.getText() != null 
                ? editSecurityAnswer.getText().toString().trim() : "";

        layoutPassword.setError(null);
        layoutConfirmPassword.setError(null);

        if (password.length() < 6) {
            layoutPassword.setError("मास्टर पासवर्ड कम से कम 6 अक्षरों का होना चाहिए!");
            editPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            layoutConfirmPassword.setError(getString(R.string.msg_password_mismatch));
            editConfirmPassword.requestFocus();
            return;
        }

        boolean success = AuthenticationManager.getInstance().setupMasterPassword(this, password, question, answer);
        if (success) {
            WebServerController.getInstance().generateNewPort(this);
            Toast.makeText(this, "सुरक्षित तिजोरी सफलतापूर्वक तैयार हो गई!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, DashboardActivity.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "तिजोरी सेटअप में त्रुटि हुई! पुनः प्रयास करें।", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected boolean shouldEnforceSessionLock() {
        return false;
    }
}
