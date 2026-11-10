package jmd.shiv.mypassword.managers;

import android.content.Context;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;

import jmd.shiv.mypassword.database.PasswordRepository;
import jmd.shiv.mypassword.models.PasswordRecord;
import jmd.shiv.mypassword.security.AuthenticationManager;
import jmd.shiv.mypassword.security.EncryptionManager;

/**
 * यह क्लास पासवर्ड डेटाबेस के सुरक्षित, पासवर्ड-संरक्षित एन्क्रिप्टेड बैकअप (Export) 
 * और डेटा रीस्टोर (Import) का प्रबंधन करती है।
 */
public class BackupManager {

    private static final String BACKUP_HEADER_APP = "SecurePasswordManager";
    private static final int BACKUP_VERSION = 1;

    /**
     * सभी पासवर्ड रिकॉर्ड्स का AES-256-GCM एन्क्रिप्टेड बैकअप तैयार करता है 
     * और दिए गए OutputStream में लिखता है।
     *
     * @param context        एप्लीकेशन संदर्भ
     * @param outputStream   गंतव्य स्ट्रीम
     * @param backupPassword बैकअप सुरक्षा पासवर्ड
     * @return कुल निर्यात किए गए रिकॉर्ड्स की संख्या, अथवा -1 यदि त्रुटि
     */
    public static int exportEncryptedBackup(Context context, OutputStream outputStream, String backupPassword) {
        if (context == null || outputStream == null || backupPassword == null || backupPassword.length() < 4) {
            return -1;
        }

        SecretKey activeKey = AuthenticationManager.getInstance().getActiveMasterKey();
        if (activeKey == null) {
            return -1;
        }

        try {
            PasswordRepository repository = new PasswordRepository(context);
            List<PasswordRecord> records = repository.getAllPasswordRecords("RECENT");

            JSONObject rootJson = new JSONObject();
            rootJson.put("version", BACKUP_VERSION);
            rootJson.put("app", BACKUP_HEADER_APP);
            rootJson.put("exported_at", System.currentTimeMillis());

            JSONArray jsonArray = new JSONArray();
            for (PasswordRecord record : records) {
                JSONObject item = new JSONObject();
                item.put("website_name", record.getWebsiteName());
                item.put("website_url", record.getWebsiteUrl());
                item.put("role", record.getRole());
                item.put("is_favorite", record.getIsFavorite());
                item.put("created_at", record.getCreatedAt());
                item.put("updated_at", record.getUpdatedAt());

                // संवेदनशील फ़ील्ड डिक्रिप्ट करना
                String username = EncryptionManager.decryptBytesToString(record.getUsernameEncrypted(), activeKey);
                String password = EncryptionManager.decryptBytesToString(record.getPasswordEncrypted(), activeKey);
                String notes = EncryptionManager.decryptBytesToString(record.getNotesEncrypted(), activeKey);

                item.put("username", username != null ? username : "");
                item.put("password", password != null ? password : "");
                item.put("notes", notes != null ? notes : "");

                // स्क्रीनशॉट BLOB डिक्रिप्ट करके Base64 में रखना
                if (record.getScreenshotBlob() != null) {
                    byte[] decryptedImg = EncryptionManager.decryptData(record.getScreenshotBlob(), activeKey);
                    if (decryptedImg != null) {
                        item.put("screenshot_base64", Base64.encodeToString(decryptedImg, Base64.NO_WRAP));
                        item.put("screenshot_mime_type", record.getScreenshotMimeType());
                    }
                }

                jsonArray.put(item);
            }

            rootJson.put("records", jsonArray);

            // पूरे JSON को PBKDF2 + AES-256-GCM से एन्क्रिप्ट करना
            byte[] encryptedPayload = EncryptionManager.encryptBackupData(rootJson.toString(), backupPassword);
            if (encryptedPayload == null) {
                return -1;
            }

            outputStream.write(encryptedPayload);
            outputStream.flush();

            return records.size();
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * एन्क्रिप्टेड बैकअप फ़ाइल को इनपुट स्ट्रीम से पढ़कर डिक्रिप्ट करता है 
     * और रिकॉर्ड्स को डेटाबेस में पुनर्स्थापित (Restore) करता है।
     *
     * @param context        एप्लीकेशन संदर्भ
     * @param inputStream    स्रोत स्ट्रीम
     * @param backupPassword बैकअप पासवर्ड
     * @return पुनर्स्थापित रिकॉर्ड्स की संख्या, अथवा -1 यदि विफलता
     */
    public static int restoreEncryptedBackup(Context context, InputStream inputStream, String backupPassword) {
        if (context == null || inputStream == null || backupPassword == null) {
            return -1;
        }

        SecretKey activeKey = AuthenticationManager.getInstance().getActiveMasterKey();
        if (activeKey == null) {
            return -1;
        }

        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] temp = new byte[4096];
            int read;
            while ((read = inputStream.read(temp)) != -1) {
                buffer.write(temp, 0, read);
            }
            byte[] backupBytes = buffer.toByteArray();

            // बैकअप डिक्रिप्ट करना
            String jsonString = EncryptionManager.decryptBackupData(backupBytes, backupPassword);
            if (jsonString == null) {
                return -1;
            }

            JSONObject rootJson = new JSONObject(jsonString);
            if (!BACKUP_HEADER_APP.equals(rootJson.optString("app"))) {
                return -1; // अमान्य बैकअप फ़ाइल प्रारूप
            }

            JSONArray jsonArray = rootJson.getJSONArray("records");
            PasswordRepository repository = new PasswordRepository(context);
            int restoredCount = 0;

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject item = jsonArray.getJSONObject(i);
                PasswordRecord record = new PasswordRecord();
                record.setWebsiteName(item.optString("website_name"));
                record.setWebsiteUrl(item.optString("website_url"));
                record.setRole(item.optString("role"));
                record.setIsFavorite(item.optInt("is_favorite", 0));
                record.setCreatedAt(item.optString("created_at"));
                record.setUpdatedAt(item.optString("updated_at"));

                // नए डिवाइस की मास्टर कुंजी से पुनः एन्क्रिप्ट करना
                String username = item.optString("username");
                String password = item.optString("password");
                String notes = item.optString("notes");

                record.setUsernameEncrypted(EncryptionManager.encryptStringToBytes(username, activeKey));
                record.setPasswordEncrypted(EncryptionManager.encryptStringToBytes(password, activeKey));
                record.setNotesEncrypted(EncryptionManager.encryptStringToBytes(notes, activeKey));

                if (item.has("screenshot_base64")) {
                    String base64Img = item.optString("screenshot_base64");
                    if (!base64Img.isEmpty()) {
                        byte[] imgBytes = Base64.decode(base64Img, Base64.NO_WRAP);
                        record.setScreenshotBlob(EncryptionManager.encryptData(imgBytes, activeKey));
                        record.setScreenshotMimeType(item.optString("screenshot_mime_type", "image/jpeg"));
                    }
                }

                long id = repository.savePasswordRecord(record);
                if (id != -1) {
                    restoredCount++;
                }
            }

            return restoredCount;
        } catch (Exception e) {
            return -1;
        }
    }
}
