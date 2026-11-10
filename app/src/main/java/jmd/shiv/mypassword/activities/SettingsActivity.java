package jmd.shiv.mypassword.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.InputStream;
import java.io.OutputStream;

import jmd.shiv.mypassword.R;
import jmd.shiv.mypassword.database.PasswordRepository;
import jmd.shiv.mypassword.managers.BackupManager;
import jmd.shiv.mypassword.models.AppSettings;
import jmd.shiv.mypassword.security.AuthenticationManager;

/**
 * यह एक्टिविटी पासवर्ड मैनेजर की संपूर्ण सेटिंग्स (मास्टर पासवर्ड बदलाव, 
 * बायोमेट्रिक टॉगल, ऑटो-लॉक टाइमर, एन्क्रिप्टेड बैकअप/रीस्टोर और प्राइवेसी पॉलिसी) को नियंत्रित करती है।
 */
public class SettingsActivity extends BaseSecurityActivity {

    private ImageView btnBack;
    private LinearLayout layoutChangePassword;
    private SwitchMaterial switchBiometric;
    private LinearLayout layoutAutoLock;
    private TextView textAutoLockVal;
    private SwitchMaterial switchClipboard;

    private LinearLayout layoutExportBackup;
    private LinearLayout layoutRestoreBackup;

    private LinearLayout layoutDbInfo;
    private TextView textDbCount;
    private LinearLayout layoutGithubRepo;
    private LinearLayout layoutPrivacy;
    private LinearLayout layoutAbout;
    private MaterialButton btnLogout;

    private PasswordRepository repository;
    private AppSettings settings;

    // बैकअप फ़ाइल एक्सपोर्ट के लिए SAF लॉन्चर
    private final ActivityResultLauncher<String> createBackupLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/octet-stream"),
            this::performExportToUri
    );

    // बैकअप फ़ाइल इम्पोर्ट के लिए SAF लॉन्चर
    private final ActivityResultLauncher<String[]> openBackupLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            this::performRestoreFromUri
    );

    private String pendingBackupPassword = null;
    private Uri pendingRestoreUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        applyEdgeToEdgeInsets(findViewById(R.id.layout_settings_root));

        repository = new PasswordRepository(this);
        settings = repository.getAppSettings();

        initializeViews();
        loadSettingsState();
        setupClickListeners();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btn_settings_back);
        layoutChangePassword = findViewById(R.id.layout_setting_change_password);
        switchBiometric = findViewById(R.id.switch_setting_biometric);
        layoutAutoLock = findViewById(R.id.layout_setting_auto_lock);
        textAutoLockVal = findViewById(R.id.text_setting_auto_lock_val);
        switchClipboard = findViewById(R.id.switch_setting_clipboard);

        layoutExportBackup = findViewById(R.id.layout_setting_export_backup);
        layoutRestoreBackup = findViewById(R.id.layout_setting_restore_backup);

        layoutDbInfo = findViewById(R.id.layout_setting_db_info);
        textDbCount = findViewById(R.id.text_setting_db_count);
        layoutGithubRepo = findViewById(R.id.layout_setting_github_repo);
        layoutPrivacy = findViewById(R.id.layout_setting_privacy);
        layoutAbout = findViewById(R.id.layout_setting_about);
        btnLogout = findViewById(R.id.btn_setting_logout);
    }

    private void loadSettingsState() {
        switchBiometric.setChecked(settings.getBiometricEnabled() == 1);
        switchClipboard.setChecked(settings.getClipboardClearEnabled() == 1);
        textAutoLockVal.setText(settings.getAutoLockMinutes() + " मिनट");

        int count = repository.getTotalPasswordCount();
        textDbCount.setText(count + " खाते");
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        layoutChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !AuthenticationManager.getInstance().isBiometricAvailable(this)) {
                switchBiometric.setChecked(false);
                Toast.makeText(this, "डिवाइस पर बायोमेट्रिक हार्डवेयर उपलब्ध या कॉन्फ़िगर नहीं है!", Toast.LENGTH_SHORT).show();
                return;
            }
            settings.setBiometricEnabled(isChecked ? 1 : 0);
            repository.saveAppSettings(settings);
        });

        layoutAutoLock.setOnClickListener(v -> showAutoLockOptionsDialog());

        switchClipboard.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.setClipboardClearEnabled(isChecked ? 1 : 0);
            repository.saveAppSettings(settings);
        });

        layoutExportBackup.setOnClickListener(v -> promptPasswordAndExportBackup());
        layoutRestoreBackup.setOnClickListener(v -> openBackupLauncher.launch(new String[]{"*/*"}));

        layoutDbInfo.setOnClickListener(v -> showDatabaseInfoDialog());
        if (layoutGithubRepo != null) {
            layoutGithubRepo.setOnClickListener(v -> showGithubRepoDialog());
        }
        layoutPrivacy.setOnClickListener(v -> showPrivacyPolicyDialog());
        layoutAbout.setOnClickListener(v -> showAboutAppDialog());

        btnLogout.setOnClickListener(v -> {
            AuthenticationManager.getInstance().logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    /**
     * मास्टर पासवर्ड बदलने का सुरक्षित संवाद (Dialog)।
     */
    private void showChangePasswordDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        final EditText editOld = new EditText(this);
        editOld.setHint("वर्तमान मास्टर पासवर्ड");
        editOld.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(editOld);

        final EditText editNew = new EditText(this);
        editNew.setHint("नया मास्टर पासवर्ड (कम से कम 6 अक्षर)");
        editNew.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(editNew);

        final EditText editConfirm = new EditText(this);
        editConfirm.setHint("नया पासवर्ड पुनः दर्ज करें");
        editConfirm.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(editConfirm);

        new AlertDialog.Builder(this)
                .setTitle(R.string.setting_change_master_pw)
                .setView(layout)
                .setPositiveButton("अपडेट करें", (dialog, which) -> {
                    String oldPw = editOld.getText().toString().trim();
                    String newPw = editNew.getText().toString().trim();
                    String confirmPw = editConfirm.getText().toString().trim();

                    if (!newPw.equals(confirmPw)) {
                        Toast.makeText(this, R.string.msg_password_mismatch, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    boolean changed = AuthenticationManager.getInstance().changeMasterPassword(this, oldPw, newPw);
                    if (changed) {
                        Toast.makeText(this, "मास्टर पासवर्ड सफलतापूर्वक बदल दिया गया!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "वर्तमान पासवर्ड गलत है अथवा नया पासवर्ड अमान्य है!", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("रद्द करें", null)
                .show();
    }

    /**
     * ऑटो-लॉक समय सीमा चयन डायलॉग (1, 5, 10, 15 मिनट)।
     */
    private void showAutoLockOptionsDialog() {
        String[] options = new String[]{"1 मिनट", "5 मिनट", "10 मिनट", "15 मिनट"};
        int[] minutes = new int[]{1, 5, 10, 15};

        int currentSelection = 1; // 5 मिनट
        for (int i = 0; i < minutes.length; i++) {
            if (minutes[i] == settings.getAutoLockMinutes()) {
                currentSelection = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.setting_auto_lock)
                .setSingleChoiceItems(options, currentSelection, (dialog, which) -> {
                    settings.setAutoLockMinutes(minutes[which]);
                    repository.saveAppSettings(settings);
                    textAutoLockVal.setText(options[which]);
                    dialog.dismiss();
                })
                .setNegativeButton("रद्द करें", null)
                .show();
    }

    /**
     * बैकअप एक्सपोर्ट से पहले बैकअप पासवर्ड पूछना।
     */
    private void promptPasswordAndExportBackup() {
        final EditText editPassword = new EditText(this);
        editPassword.setHint("बैकअप सुरक्षा पासवर्ड दर्ज करें");
        editPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        LinearLayout container = new LinearLayout(this);
        container.setPadding(48, 24, 48, 24);
        container.addView(editPassword);

        new AlertDialog.Builder(this)
                .setTitle(R.string.setting_backup)
                .setMessage("बैकअप फ़ाइल को AES-256 से एन्क्रिप्ट करने के लिए एक पासवर्ड चुनें:")
                .setView(container)
                .setPositiveButton("फ़ाइल चुनें", (dialog, which) -> {
                    String pw = editPassword.getText().toString().trim();
                    if (pw.length() < 4) {
                        Toast.makeText(this, "बैकअप पासवर्ड कम से कम 4 अक्षरों का होना चाहिए!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    pendingBackupPassword = pw;
                    createBackupLauncher.launch("PasswordManager_Backup_" + System.currentTimeMillis() + ".spm");
                })
                .setNegativeButton("रद्द करें", null)
                .show();
    }

    private void performExportToUri(Uri uri) {
        if (uri == null || pendingBackupPassword == null) {
            return;
        }

        try (OutputStream os = getContentResolver().openOutputStream(uri)) {
            if (os != null) {
                int count = BackupManager.exportEncryptedBackup(this, os, pendingBackupPassword);
                if (count >= 0) {
                    Toast.makeText(this, "एन्क्रिप्टेड बैकअप सफलतापूर्वक सेव हुआ! कुल खाते: " + count, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "बैकअप बनाने में विफलता!", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "फ़ाइल लिखने में त्रुटि!", Toast.LENGTH_SHORT).show();
        } finally {
            pendingBackupPassword = null;
        }
    }

    private void performRestoreFromUri(Uri uri) {
        if (uri == null) {
            return;
        }

        pendingRestoreUri = uri;
        final EditText editPassword = new EditText(this);
        editPassword.setHint("बैकअप फ़ाइल का पासवर्ड");
        editPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        LinearLayout container = new LinearLayout(this);
        container.setPadding(48, 24, 48, 24);
        container.addView(editPassword);

        new AlertDialog.Builder(this)
                .setTitle(R.string.setting_restore)
                .setMessage("बैकअप फ़ाइल को डिक्रिप्ट करने के लिए पासवर्ड दर्ज करें:")
                .setView(container)
                .setPositiveButton("रीस्टोर करें", (dialog, which) -> {
                    String pw = editPassword.getText().toString().trim();
                    executeRestore(pendingRestoreUri, pw);
                })
                .setNegativeButton("रद्द करें", null)
                .show();
    }

    private void executeRestore(Uri uri, String password) {
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is != null) {
                int count = BackupManager.restoreEncryptedBackup(this, is, password);
                if (count >= 0) {
                    Toast.makeText(this, getString(R.string.msg_restore_success, count), Toast.LENGTH_LONG).show();
                    loadSettingsState();
                } else {
                    Toast.makeText(this, R.string.msg_restore_failed, Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "फ़ाइल पढ़ने में त्रुटि!", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDatabaseInfoDialog() {
        int total = repository.getTotalPasswordCount();
        int fav = repository.getFavoriteCount();
        String info = "डेटाबेस नाम: " + PasswordRepository.class.getSimpleName() + "\n"
                + "फ़ाइल नाम: password_manager.db\n"
                + "कुल खाते: " + total + "\n"
                + "पसंदीदा खाते: " + fav + "\n"
                + "सुरक्षा स्तर: AES-256-GCM + PBKDF2\n"
                + "हार्डवेयर कीस्टोर: सक्रिय";

        new AlertDialog.Builder(this)
                .setTitle(R.string.setting_db_info)
                .setMessage(info)
                .setPositiveButton("ठीक है", null)
                .show();
    }

    private void showGithubRepoDialog() {
        final String webUrl = "https://singlaispanther.github.io/PasswordManager/";
        final String repoName = "SinglaIsPanther/PasswordManager";

        String message = "🌐 ऑनलाइन वेब तिजोरी (Web Vault Link):\n"
                + webUrl + "\n\n"
                + "📦 GitHub Repository:\n"
                + repoName + "\n\n"
                + "इस लिंक को कंप्यूटर/लैपटॉप के Google Chrome में खोलकर आप पासवर्ड्स देख, कॉपी और सिंक कर सकते हैं।";

        new AlertDialog.Builder(this)
                .setTitle("Web Vault & Repository")
                .setMessage(message)
                .setPositiveButton("ब्राउज़र में खोलें", (dialog, which) -> {
                    try {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl));
                        startActivity(browserIntent);
                    } catch (Exception e) {
                        Toast.makeText(this, "ब्राउज़र खोलने में विफलता!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("शेयर करें", (dialog, which) -> {
                    try {
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Password Manager Web Vault");
                        shareIntent.putExtra(Intent.EXTRA_TEXT, "Open Password Manager Web Vault in Chrome: " + webUrl);
                        startActivity(Intent.createChooser(shareIntent, "वेब वॉल्ट लिंक शेयर करें"));
                    } catch (Exception e) {
                        Toast.makeText(this, "शेयर करने में विफलता!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("कॉपी करें", (dialog, which) -> {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        android.content.ClipData clip = android.content.ClipData.newPlainText("Web Vault Link", webUrl);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(this, "वेब वॉल्ट लिंक कॉपी हो गया!", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void showPrivacyPolicyDialog() {
        String policy = "गोपनीयता नीति (Privacy Policy - _mypassword):\n\n"
                + "1. NO ADS (शून्य विज्ञापन):\n"
                + "ऐप में कोई भी विज्ञापन नहीं है (No AdMob, No Popups, No Video Ads)।\n\n"
                + "2. NO THIRD-PARTY (शून्य थर्ड-पार्टी):\n"
                + "कोई थर्ड-पार्टी SDK, एनालिटिक्स या ट्रैकर (Firebase, Facebook) उपयोग नहीं होता।\n\n"
                + "3. LOCAL STORAGE ONLY (100% ऑन-डिवाइस):\n"
                + "सभी डेटा केवल आपके मोबाइल की प्राइवेट SQLite मेमोरी में रहता है। कोई क्लाउड सर्वर नहीं है।\n\n"
                + "4. SALT & HASH मास्टर पासवर्ड:\n"
                + "मास्टर पासवर्ड को 32-बाइट क्रिप्टोग्राफिक Salt व PBKDF2 (1,00,000 पुनरावृत्तियां) द्वारा सुरक्षित किया जाता है।\n\n"
                + "5. USERNAME, PASSWORD & SCREENSHOT ENCRYPTION:\n"
                + "यूजरनेम, पासवर्ड और अटैच किए गए स्क्रीनशॉट Android Keystore की AES-256-GCM मिलिट्री-ग्रेड कुंजी द्वारा एन्क्रिप्टेड रहते हैं।\n\n"
                + "6. NO SENSITIVE PERMISSION REQUIRED:\n"
                + "संपर्क (Contacts), लोकेशन (GPS), कैमरा या स्टोरेज जैसी कोई भी संवेदनशील अनुमति नहीं ली जाती।\n\n"
                + "7. Google Play 100% Policy Compliant\n"
                + "पैकेज: jmd.shiv.mypassword";

        new AlertDialog.Builder(this)
                .setTitle(R.string.setting_privacy)
                .setMessage(policy)
                .setPositiveButton("समझ गया", null)
                .show();
    }

    private void showAboutAppDialog() {
        String about = "Secure Password Manager\n"
                + "संस्करण: 1.0.0 (Production Build)\n"
                + "डेवलपर: JMD Shiv Solutions\n"
                + "पैकेज: jmd.shiv.mypassword\n\n"
                + "विशेष रूप से कंप्यूटर ऑपरेटरों, सरकारी पोर्टलों (SSO, E-Mitra, Election) और कार्यालयीन उपयोग के लिए विकसित।";

        new AlertDialog.Builder(this)
                .setTitle(R.string.setting_about)
                .setMessage(about)
                .setPositiveButton("ठीक है", null)
                .show();
    }
}
