package jmd.shiv.mypassword.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import javax.crypto.SecretKey;

import jmd.shiv.mypassword.R;
import jmd.shiv.mypassword.database.PasswordRepository;
import jmd.shiv.mypassword.managers.ClipboardManagerHelper;
import jmd.shiv.mypassword.managers.ScreenshotManager;
import jmd.shiv.mypassword.models.PasswordRecord;
import jmd.shiv.mypassword.security.AuthenticationManager;
import jmd.shiv.mypassword.security.EncryptionManager;
import jmd.shiv.mypassword.utils.DateUtils;
import jmd.shiv.mypassword.utils.UrlValidator;

/**
 * यह एक्टिविटी चुने गए खाते का पूर्ण विवरण, पासवर्ड प्रकटीकरण (Show/Hide), 
 * स्क्रीनशॉट और संपादन/हटाने के विकल्प प्रस्तुत करती है।
 */
public class PasswordDetailsActivity extends BaseSecurityActivity {

    private ImageView btnBack;
    private ImageView btnEdit;
    private ImageView btnDelete;
    private TextView textWebsiteName;
    private ImageView btnFavorite;
    private LinearLayout layoutUrl;
    private TextView textWebsiteUrl;
    private TextView textUsername;
    private MaterialButton btnCopyUsername;
    private TextView textPassword;
    private MaterialButton btnTogglePassword;
    private MaterialButton btnCopyPassword;
    private TextView textRole;
    private LinearLayout layoutNotes;
    private TextView textNotes;

    private MaterialCardView cardScreenshot;
    private ImageView imgScreenshot;
    private MaterialButton btnViewFullscreen;

    private TextView textCreatedAt;
    private TextView textUpdatedAt;

    private PasswordRepository repository;
    private long recordId = -1;
    private PasswordRecord record;

    private String rawDecryptedPassword = "";
    private String rawDecryptedUsername = "";
    private byte[] decryptedScreenshotBlob = null;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_details);
        applyEdgeToEdgeInsets(findViewById(R.id.layout_details_root));

        repository = new PasswordRepository(this);
        recordId = getIntent().getLongExtra("RECORD_ID", -1);
        if (recordId == -1) {
            finish();
            return;
        }

        initializeViews();
        setupClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAndDisplayRecordDetails();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btn_details_back);
        btnEdit = findViewById(R.id.btn_details_edit);
        btnDelete = findViewById(R.id.btn_details_delete);
        textWebsiteName = findViewById(R.id.text_details_website_name);
        btnFavorite = findViewById(R.id.btn_details_favorite);
        layoutUrl = findViewById(R.id.layout_details_url);
        textWebsiteUrl = findViewById(R.id.text_details_website_url);
        textUsername = findViewById(R.id.text_details_username);
        btnCopyUsername = findViewById(R.id.btn_details_copy_username);
        textPassword = findViewById(R.id.text_details_password);
        btnTogglePassword = findViewById(R.id.btn_details_toggle_password);
        btnCopyPassword = findViewById(R.id.btn_details_copy_password);
        textRole = findViewById(R.id.text_details_role);
        layoutNotes = findViewById(R.id.layout_details_notes);
        textNotes = findViewById(R.id.text_details_notes);

        cardScreenshot = findViewById(R.id.card_details_screenshot);
        imgScreenshot = findViewById(R.id.img_details_screenshot);
        btnViewFullscreen = findViewById(R.id.btn_details_view_fullscreen);

        textCreatedAt = findViewById(R.id.text_details_created_at);
        textUpdatedAt = findViewById(R.id.text_details_updated_at);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddPasswordActivity.class);
            intent.putExtra("RECORD_ID", recordId);
            startActivity(intent);
        });

        btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog());

        btnFavorite.setOnClickListener(v -> {
            if (record != null) {
                boolean newFav = record.getIsFavorite() == 0;
                repository.toggleFavorite(record.getId(), newFav);
                record.setIsFavorite(newFav ? 1 : 0);
                btnFavorite.setImageResource(newFav ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
                jmd.shiv.mypassword.server.WebSocketManager.getInstance().broadcastDataChange("UPDATE", record.getId(), record.getWebsiteName());
            }
        });

        layoutUrl.setOnClickListener(v -> openWebsiteUrlInBrowser());

        btnCopyUsername.setOnClickListener(v -> {
            if (!rawDecryptedUsername.isEmpty()) {
                ClipboardManagerHelper.copyToClipboard(this, "Username", rawDecryptedUsername, false);
                Toast.makeText(this, "यूजरनेम कॉपी किया गया!", Toast.LENGTH_SHORT).show();
            }
        });

        btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());

        btnCopyPassword.setOnClickListener(v -> {
            if (!rawDecryptedPassword.isEmpty()) {
                ClipboardManagerHelper.copyToClipboard(this, "Password", rawDecryptedPassword, true);
                Toast.makeText(this, getString(R.string.msg_copied), Toast.LENGTH_LONG).show();
            }
        });

        btnViewFullscreen.setOnClickListener(v -> {
            if (decryptedScreenshotBlob != null) {
                Intent intent = new Intent(this, ScreenshotViewerActivity.class);
                intent.putExtra("RECORD_ID", recordId);
                startActivity(intent);
            }
        });

        imgScreenshot.setOnClickListener(v -> {
            if (decryptedScreenshotBlob != null) {
                Intent intent = new Intent(this, ScreenshotViewerActivity.class);
                intent.putExtra("RECORD_ID", recordId);
                startActivity(intent);
            }
        });
    }

    /**
     * डेटाबेस से रिकॉर्ड प्राप्त करके AES-256-GCM से डिक्रिप्ट करता है और स्क्रीन पर भरता है।
     */
    private void loadAndDisplayRecordDetails() {
        record = repository.getPasswordRecordById(recordId);
        if (record == null) {
            finish();
            return;
        }

        SecretKey activeKey = AuthenticationManager.getInstance().getActiveMasterKey();
        if (activeKey == null) {
            finish();
            return;
        }

        textWebsiteName.setText(record.getWebsiteName());

        String url = record.getWebsiteUrl();
        if (url != null && !url.trim().isEmpty()) {
            textWebsiteUrl.setText(url);
            layoutUrl.setVisibility(View.VISIBLE);
        } else {
            layoutUrl.setVisibility(View.GONE);
        }

        btnFavorite.setImageResource(record.getIsFavorite() == 1 ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);

        // रोल
        String role = record.getRole();
        if (role != null && !role.trim().isEmpty()) {
            textRole.setText(role);
            textRole.setVisibility(View.VISIBLE);
        } else {
            textRole.setVisibility(View.GONE);
        }

        // डिक्रिप्शन
        try {
            rawDecryptedUsername = EncryptionManager.decryptBytesToString(record.getUsernameEncrypted(), activeKey);
            rawDecryptedPassword = EncryptionManager.decryptBytesToString(record.getPasswordEncrypted(), activeKey);
            String notes = EncryptionManager.decryptBytesToString(record.getNotesEncrypted(), activeKey);

            textUsername.setText(rawDecryptedUsername != null ? rawDecryptedUsername : "");

            // पासवर्ड डिफॉल्ट रूप से छिपा हुआ
            isPasswordVisible = false;
            updatePasswordDisplay();

            if (notes != null && !notes.trim().isEmpty()) {
                textNotes.setText(notes);
                layoutNotes.setVisibility(View.VISIBLE);
            } else {
                layoutNotes.setVisibility(View.GONE);
            }

            // स्क्रीनशॉट
            if (record.getScreenshotBlob() != null) {
                decryptedScreenshotBlob = EncryptionManager.decryptData(record.getScreenshotBlob(), activeKey);
                if (decryptedScreenshotBlob != null) {
                    Bitmap bmp = ScreenshotManager.decodeBlobToBitmap(decryptedScreenshotBlob);
                    if (bmp != null) {
                        imgScreenshot.setImageBitmap(bmp);
                        cardScreenshot.setVisibility(View.VISIBLE);
                    } else {
                        cardScreenshot.setVisibility(View.GONE);
                    }
                } else {
                    cardScreenshot.setVisibility(View.GONE);
                }
            } else {
                cardScreenshot.setVisibility(View.GONE);
            }

            textCreatedAt.setText(getString(R.string.created_at_label) + " " + DateUtils.formatFriendlyDate(record.getCreatedAt()));
            textUpdatedAt.setText(getString(R.string.updated_at_label) + " " + DateUtils.formatFriendlyDate(record.getUpdatedAt()));

        } catch (Exception e) {
            Toast.makeText(this, "संवेदनशील डेटा डिक्रिप्ट करने में त्रुटि!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * पासवर्ड की दृश्यता (Show / Hide) टॉगल करना।
     */
    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        updatePasswordDisplay();
    }

    private void updatePasswordDisplay() {
        if (isPasswordVisible) {
            textPassword.setText(rawDecryptedPassword);
            btnTogglePassword.setText("छिपाएं");
        } else {
            textPassword.setText("••••••••••••");
            btnTogglePassword.setText("दिखाएं");
        }
    }

    /**
     * सुरक्षित URL सत्यापन और ब्राउज़र में वेबसाइट खोलना।
     */
    private void openWebsiteUrlInBrowser() {
        if (record == null) return;
        String rawUrl = record.getWebsiteUrl();
        String validUrl = UrlValidator.sanitizeAndValidateUrl(rawUrl);

        if (validUrl != null) {
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(validUrl));
                startActivity(Intent.createChooser(browserIntent, "वेबसाइट खोलें"));
            } catch (Exception e) {
                Toast.makeText(this, "वेबसाइट खोलने के लिए कोई ब्राउज़र नहीं मिला!", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, R.string.msg_invalid_url, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * रिकॉर्ड हटाने से पहले सुरक्षा पुष्टि डायलॉग।
     */
    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("खाता हटाएं")
                .setMessage(R.string.msg_confirm_delete)
                .setIcon(R.drawable.ic_delete)
                .setPositiveButton("हां, हटाएं", (dialog, which) -> {
                    String siteName = (record != null) ? record.getWebsiteName() : "";
                    repository.deletePasswordRecord(recordId);
                    repository.insertAuditLog(recordId, "DELETE", "MOBILE_DEVICE");
                    jmd.shiv.mypassword.server.WebSocketManager.getInstance().broadcastDataChange("DELETE", recordId, siteName);
                    Toast.makeText(this, R.string.msg_deleted_success, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("रद्द करें", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // संवेदनशील डेटा मेमोरी से साफ़ करना
        rawDecryptedPassword = "";
        rawDecryptedUsername = "";
        decryptedScreenshotBlob = null;
    }
}
