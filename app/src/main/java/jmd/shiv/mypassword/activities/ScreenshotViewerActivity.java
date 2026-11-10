package jmd.shiv.mypassword.activities;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import javax.crypto.SecretKey;

import jmd.shiv.mypassword.R;
import jmd.shiv.mypassword.database.PasswordRepository;
import jmd.shiv.mypassword.managers.ScreenshotManager;
import jmd.shiv.mypassword.models.PasswordRecord;
import jmd.shiv.mypassword.security.AuthenticationManager;
import jmd.shiv.mypassword.security.EncryptionManager;

/**
 * यह एक्टिविटी सहेजे गए वेबसाइट लॉगिन स्क्रीनशॉट को सुरक्षित रूप से 
 * फुल स्क्रीन (Full Screen) में प्रदर्शित करती है।
 */
public class ScreenshotViewerActivity extends BaseSecurityActivity {

    private ImageView btnBack;
    private TextView textTitle;
    private ImageView imgPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screenshot_viewer);
        applyEdgeToEdgeInsets(findViewById(R.id.layout_viewer_root));

        btnBack = findViewById(R.id.btn_viewer_back);
        textTitle = findViewById(R.id.text_viewer_title);
        imgPreview = findViewById(R.id.img_fullscreen_preview);

        btnBack.setOnClickListener(v -> finish());

        long recordId = getIntent().getLongExtra("RECORD_ID", -1);
        if (recordId != -1) {
            loadScreenshot(recordId);
        } else {
            finish();
        }
    }

    /**
     * डेटाबेस से एन्क्रिप्टेड BLOB प्राप्त करके डिक्रिप्ट करता है और फुलस्क्रीन पर रेंडर करता है।
     */
    private void loadScreenshot(long recordId) {
        PasswordRepository repository = new PasswordRepository(this);
        PasswordRecord record = repository.getPasswordRecordById(recordId);
        if (record == null || record.getScreenshotBlob() == null) {
            Toast.makeText(this, "स्क्रीनशॉट उपलब्ध नहीं है!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (record.getWebsiteName() != null) {
            textTitle.setText(record.getWebsiteName() + " - स्क्रीनशॉट");
        }

        SecretKey activeKey = AuthenticationManager.getInstance().getActiveMasterKey();
        if (activeKey == null) {
            finish();
            return;
        }

        try {
            byte[] decryptedBlob = EncryptionManager.decryptData(record.getScreenshotBlob(), activeKey);
            if (decryptedBlob != null) {
                Bitmap bitmap = ScreenshotManager.decodeBlobToBitmap(decryptedBlob);
                if (bitmap != null) {
                    imgPreview.setImageBitmap(bitmap);
                } else {
                    Toast.makeText(this, "इमेज डिकोड करने में त्रुटि!", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else {
                Toast.makeText(this, "डिक्रिप्शन असफल!", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            Toast.makeText(this, "स्क्रीनशॉट लोड करने में त्रुटि!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
