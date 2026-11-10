package jmd.shiv.mypassword.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import javax.crypto.SecretKey;

import jmd.shiv.mypassword.R;
import jmd.shiv.mypassword.database.PasswordRepository;
import jmd.shiv.mypassword.managers.ScreenshotManager;
import jmd.shiv.mypassword.models.AppSettings;
import jmd.shiv.mypassword.models.PasswordRecord;
import jmd.shiv.mypassword.security.AuthenticationManager;
import jmd.shiv.mypassword.security.EncryptionManager;
import jmd.shiv.mypassword.utils.PasswordGenerator;
import jmd.shiv.mypassword.utils.UrlValidator;

/**
 * यह एक्टिविटी नए पासवर्ड रिकॉर्ड को सुरक्षित रूप से जोड़ने 
 * अथवा मौजूदा रिकॉर्ड को संपादित (Edit) करने का कार्य करती है।
 */
public class AddPasswordActivity extends BaseSecurityActivity {

    private ImageView btnBack;
    private TextView textTitle;
    private TextInputLayout layoutWebsiteName;
    private TextInputLayout layoutWebsiteUrl;
    private TextInputLayout layoutUsername;
    private TextInputLayout layoutPassword;
    private TextInputEditText editWebsiteName;
    private TextInputEditText editWebsiteUrl;
    private TextInputEditText editUsername;
    private TextInputEditText editPassword;
    private MaterialButton btnGenerateInline;
    private AutoCompleteTextView editRole;
    private TextInputEditText editNotes;
    private SwitchMaterial switchFavorite;

    private FrameLayout layoutScreenshotPreview;
    private ImageView imgScreenshotPreview;
    private ImageView btnDeleteScreenshot;
    private MaterialButton btnChooseScreenshot;

    private MaterialButton btnCancel;
    private MaterialButton btnSave;

    private PasswordRepository repository;
    private long editingRecordId = -1;
    private PasswordRecord existingRecord = null;

    private byte[] pendingScreenshotBytes = null;
    private String pendingScreenshotMimeType = null;
    private boolean isScreenshotRemoved = false;

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            this::handleSelectedImageUri
    );

    private final ActivityResultLauncher<Intent> generatorLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    String pw = result.getData().getStringExtra("GENERATED_PASSWORD");
                    if (pw != null && !pw.isEmpty()) {
                        editPassword.setText(pw);
                        editPassword.setSelection(pw.length());
                        Toast.makeText(this, "पासवर्ड लागू किया गया: " + pw, Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_password);
        applyEdgeToEdgeInsets(findViewById(R.id.layout_add_root));

        repository = new PasswordRepository(this);
        initializeViews();
        setupRoleDropdown();
        setupClickListeners();
        checkIfEditingMode();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btn_add_back);
        textTitle = findViewById(R.id.text_add_screen_title);
        layoutWebsiteName = findViewById(R.id.input_layout_website_name);
        layoutWebsiteUrl = findViewById(R.id.input_layout_website_url);
        layoutUsername = findViewById(R.id.input_layout_username);
        layoutPassword = findViewById(R.id.input_layout_password);

        editWebsiteName = findViewById(R.id.edit_website_name);
        editWebsiteUrl = findViewById(R.id.edit_website_url);
        editUsername = findViewById(R.id.edit_username);
        editPassword = findViewById(R.id.edit_password);
        btnGenerateInline = findViewById(R.id.btn_generate_inline);
        editRole = findViewById(R.id.edit_role);
        editNotes = findViewById(R.id.edit_notes);
        switchFavorite = findViewById(R.id.switch_favorite);

        layoutScreenshotPreview = findViewById(R.id.layout_screenshot_preview);
        imgScreenshotPreview = findViewById(R.id.img_screenshot_preview);
        btnDeleteScreenshot = findViewById(R.id.btn_delete_screenshot);
        btnChooseScreenshot = findViewById(R.id.btn_choose_screenshot);

        btnCancel = findViewById(R.id.btn_add_cancel);
        btnSave = findViewById(R.id.btn_add_save);
    }

    private void setupRoleDropdown() {
        String[] predefinedRoles = new String[]{
                "Computer Operator",
                "Senior Operator",
                "Administrator",
                "Data Entry Operator",
                "Clerk",
                "Accountant",
                "Office Assistant",
                "Supervisor",
                "Other"
        };
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, predefinedRoles);
        editRole.setAdapter(roleAdapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnCancel.setOnClickListener(v -> finish());

        btnGenerateInline.setOnClickListener(v -> {
            AppSettings settings = repository.getAppSettings();
            String generated = PasswordGenerator.generatePassword(
                    settings.getGenLength(),
                    settings.getGenIncludeUppercase() == 1,
                    settings.getGenIncludeLowercase() == 1,
                    settings.getGenIncludeNumbers() == 1,
                    settings.getGenIncludeSymbols() == 1
            );
            editPassword.setText(generated);
            editPassword.setSelection(generated.length());
            Toast.makeText(this, "नया पासवर्ड बनाया गया: " + generated, Toast.LENGTH_LONG).show();
        });

        // बटन को दबाकर रखने पर पूर्ण पासवर्ड जनरेटर सेटिंग विंडो खुलेगी
        btnGenerateInline.setOnLongClickListener(v -> {
            Intent intent = new Intent(this, PasswordGeneratorActivity.class);
            generatorLauncher.launch(intent);
            return true;
        });

        btnChooseScreenshot.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        btnDeleteScreenshot.setOnClickListener(v -> {
            pendingScreenshotBytes = null;
            pendingScreenshotMimeType = null;
            isScreenshotRemoved = true;
            layoutScreenshotPreview.setVisibility(View.GONE);
            btnChooseScreenshot.setText(R.string.btn_choose_screenshot);
        });

        btnSave.setOnClickListener(v -> saveOrUpdateRecord());
    }

    /**
     * चयनित इमेज का संपीड़न और प्रिव्यू तैयार करना।
     */
    private void handleSelectedImageUri(Uri uri) {
        if (uri == null) {
            return;
        }

        ScreenshotManager.ProcessedImage processed = ScreenshotManager.processAndCompressImage(this, uri);
        if (processed != null && processed.imageBytes != null) {
            pendingScreenshotBytes = processed.imageBytes;
            pendingScreenshotMimeType = processed.mimeType;
            isScreenshotRemoved = false;

            Bitmap bitmap = ScreenshotManager.decodeBlobToBitmap(pendingScreenshotBytes);
            if (bitmap != null) {
                imgScreenshotPreview.setImageBitmap(bitmap);
                layoutScreenshotPreview.setVisibility(View.VISIBLE);
                btnChooseScreenshot.setText(R.string.btn_change_screenshot);
            }
        } else {
            Toast.makeText(this, "इमेज लोड करने में त्रुटि!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * जांचता है कि क्या यह संपादन (Edit) मोड है और मौजूदा मान भरता है।
     */
    private void checkIfEditingMode() {
        if (getIntent().hasExtra("RECORD_ID")) {
            editingRecordId = getIntent().getLongExtra("RECORD_ID", -1);
            if (editingRecordId != -1) {
                textTitle.setText(R.string.title_edit_password);
                loadExistingRecordData(editingRecordId);
            }
        }
    }

    private void loadExistingRecordData(long recordId) {
        existingRecord = repository.getPasswordRecordById(recordId);
        if (existingRecord == null) {
            finish();
            return;
        }

        SecretKey activeKey = AuthenticationManager.getInstance().getActiveMasterKey();
        if (activeKey == null) {
            finish();
            return;
        }

        editWebsiteName.setText(existingRecord.getWebsiteName());
        editWebsiteUrl.setText(existingRecord.getWebsiteUrl());
        editRole.setText(existingRecord.getRole(), false);
        switchFavorite.setChecked(existingRecord.getIsFavorite() == 1);

        try {
            String username = EncryptionManager.decryptBytesToString(existingRecord.getUsernameEncrypted(), activeKey);
            String password = EncryptionManager.decryptBytesToString(existingRecord.getPasswordEncrypted(), activeKey);
            String notes = EncryptionManager.decryptBytesToString(existingRecord.getNotesEncrypted(), activeKey);

            editUsername.setText(username != null ? username : "");
            editPassword.setText(password != null ? password : "");
            editNotes.setText(notes != null ? notes : "");

            if (existingRecord.getScreenshotBlob() != null) {
                byte[] decryptedImg = EncryptionManager.decryptData(existingRecord.getScreenshotBlob(), activeKey);
                if (decryptedImg != null) {
                    pendingScreenshotBytes = decryptedImg;
                    pendingScreenshotMimeType = existingRecord.getScreenshotMimeType();
                    Bitmap bitmap = ScreenshotManager.decodeBlobToBitmap(decryptedImg);
                    if (bitmap != null) {
                        imgScreenshotPreview.setImageBitmap(bitmap);
                        layoutScreenshotPreview.setVisibility(View.VISIBLE);
                        btnChooseScreenshot.setText(R.string.btn_change_screenshot);
                    }
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "डेटा लोड करने में त्रुटि!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * फ़ॉर्म डेटा का सत्यापन, AES-256-GCM एन्क्रिप्शन और डेटाबेस में सुरक्षित सहेजना।
     */
    private void saveOrUpdateRecord() {
        String websiteName = editWebsiteName.getText() != null ? editWebsiteName.getText().toString().trim() : "";
        String websiteUrl = editWebsiteUrl.getText() != null ? editWebsiteUrl.getText().toString().trim() : "";
        String username = editUsername.getText() != null ? editUsername.getText().toString().trim() : "";
        String password = editPassword.getText() != null ? editPassword.getText().toString().trim() : "";
        String role = editRole.getText() != null ? editRole.getText().toString().trim() : "";
        String notes = editNotes.getText() != null ? editNotes.getText().toString().trim() : "";
        boolean isFavorite = switchFavorite.isChecked();

        layoutWebsiteName.setError(null);
        layoutUsername.setError(null);
        layoutPassword.setError(null);

        if (websiteName.isEmpty()) {
            layoutWebsiteName.setError("वेबसाइट का नाम आवश्यक है!");
            editWebsiteName.requestFocus();
            return;
        }

        if (username.isEmpty()) {
            layoutUsername.setError("यूजरनेम आवश्यक है!");
            editUsername.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            layoutPassword.setError("पासवर्ड आवश्यक है!");
            editPassword.requestFocus();
            return;
        }

        // URL सैनिटाइजेशन
        if (!websiteUrl.isEmpty()) {
            String sanitized = UrlValidator.sanitizeAndValidateUrl(websiteUrl);
            if (sanitized != null) {
                websiteUrl = sanitized;
            }
        }

        SecretKey activeKey = AuthenticationManager.getInstance().getActiveMasterKey();
        if (activeKey == null) {
            Toast.makeText(this, "सत्र समाप्त! कृपया पुनः लॉगिन करें।", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            PasswordRecord record = (existingRecord != null) ? existingRecord : new PasswordRecord();
            record.setWebsiteName(websiteName);
            record.setWebsiteUrl(websiteUrl);
            record.setRole(role);
            record.setIsFavorite(isFavorite ? 1 : 0);

            // संवेदनशील डेटा AES-256-GCM एन्क्रिप्ट करना
            record.setUsernameEncrypted(EncryptionManager.encryptStringToBytes(username, activeKey));
            record.setPasswordEncrypted(EncryptionManager.encryptStringToBytes(password, activeKey));
            record.setNotesEncrypted(EncryptionManager.encryptStringToBytes(notes, activeKey));

            // स्क्रीनशॉट BLOB एन्क्रिप्शन
            if (pendingScreenshotBytes != null) {
                byte[] encryptedImg = EncryptionManager.encryptData(pendingScreenshotBytes, activeKey);
                record.setScreenshotBlob(encryptedImg);
                record.setScreenshotMimeType(pendingScreenshotMimeType != null ? pendingScreenshotMimeType : "image/jpeg");
            } else if (isScreenshotRemoved) {
                record.setScreenshotBlob(null);
                record.setScreenshotMimeType(null);
            }

            if (editingRecordId != -1) {
                record.setId(editingRecordId);
                int rows = repository.updatePasswordRecord(record);
                if (rows > 0) {
                    repository.insertAuditLog(editingRecordId, "UPDATE", "MOBILE_DEVICE");
                    jmd.shiv.mypassword.server.WebSocketManager.getInstance().broadcastDataChange("UPDATE", editingRecordId, websiteName);
                    Toast.makeText(this, R.string.msg_updated_success, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "अपडेट करने में त्रुटि!", Toast.LENGTH_SHORT).show();
                }
            } else {
                long newId = repository.savePasswordRecord(record);
                if (newId != -1) {
                    repository.insertAuditLog(newId, "CREATE", "MOBILE_DEVICE");
                    jmd.shiv.mypassword.server.WebSocketManager.getInstance().broadcastDataChange("CREATE", newId, websiteName);
                    Toast.makeText(this, R.string.msg_saved_success, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "डेटाबेस में सेव करने में त्रुटि!", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            android.util.Log.e("AddPasswordActivity", "Error saving password record", e);
            Toast.makeText(this, "सुरक्षित सेव करने में त्रुटि: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
