package jmd.shiv.mypassword.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import jmd.shiv.mypassword.models.AppSettings;
import jmd.shiv.mypassword.models.AuditLog;
import jmd.shiv.mypassword.models.PairedDevice;
import jmd.shiv.mypassword.models.PasswordRecord;
import jmd.shiv.mypassword.models.SecurityData;

/**
 * यह क्लास SQLite डेटाबेस के सभी CRUD (Create, Read, Update, Delete), 
 * पेयर किए गए डिवाइसेज (Chrome) और ऑडिट लॉग्स का केंद्रीय रिपॉजिटरी प्रबंधन करती है।
 */
public class PasswordRepository {

    private final PasswordDatabaseHelper dbHelper;

    public PasswordRepository(Context context) {
        this.dbHelper = PasswordDatabaseHelper.getInstance(context);
    }

    private String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    /**
     * यह Function नए Password Record को SQLite Database में सुरक्षित करता है।
     *
     * @param record सेव किए जाने वाले Password की जानकारी
     * @return सफल होने पर Record ID, अन्यथा -1
     */
    public long savePasswordRecord(PasswordRecord record) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(PasswordDatabaseHelper.COL_WEBSITE_NAME, record.getWebsiteName());
        values.put(PasswordDatabaseHelper.COL_WEBSITE_URL, record.getWebsiteUrl());
        values.put(PasswordDatabaseHelper.COL_USERNAME_ENCRYPTED, record.getUsernameEncrypted());
        values.put(PasswordDatabaseHelper.COL_PASSWORD_ENCRYPTED, record.getPasswordEncrypted());
        values.put(PasswordDatabaseHelper.COL_ROLE, record.getRole());
        values.put(PasswordDatabaseHelper.COL_SCREENSHOT_BLOB, record.getScreenshotBlob());
        values.put(PasswordDatabaseHelper.COL_SCREENSHOT_MIME_TYPE, record.getScreenshotMimeType());
        values.put(PasswordDatabaseHelper.COL_NOTES_ENCRYPTED, record.getNotesEncrypted());
        values.put(PasswordDatabaseHelper.COL_IS_FAVORITE, record.getIsFavorite());
        values.put(PasswordDatabaseHelper.COL_RECORD_VERSION, 1);
        
        String now = getCurrentDateTime();
        values.put(PasswordDatabaseHelper.COL_CREATED_AT, now);
        values.put(PasswordDatabaseHelper.COL_UPDATED_AT, now);

        return db.insert(PasswordDatabaseHelper.TABLE_PASSWORD_RECORDS, null, values);
    }

    /**
     * यह Function पहले से मौजूद Password Record के विवरण को अपडेट करता है और वर्शन बढ़ाता है।
     *
     * @param record अपडेट की जाने वाली नई जानकारी
     * @return प्रभावित पंक्तियों की संख्या
     */
    public int updatePasswordRecord(PasswordRecord record) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(PasswordDatabaseHelper.COL_WEBSITE_NAME, record.getWebsiteName());
        values.put(PasswordDatabaseHelper.COL_WEBSITE_URL, record.getWebsiteUrl());
        values.put(PasswordDatabaseHelper.COL_USERNAME_ENCRYPTED, record.getUsernameEncrypted());
        values.put(PasswordDatabaseHelper.COL_PASSWORD_ENCRYPTED, record.getPasswordEncrypted());
        values.put(PasswordDatabaseHelper.COL_ROLE, record.getRole());
        values.put(PasswordDatabaseHelper.COL_SCREENSHOT_BLOB, record.getScreenshotBlob());
        values.put(PasswordDatabaseHelper.COL_SCREENSHOT_MIME_TYPE, record.getScreenshotMimeType());
        values.put(PasswordDatabaseHelper.COL_NOTES_ENCRYPTED, record.getNotesEncrypted());
        values.put(PasswordDatabaseHelper.COL_IS_FAVORITE, record.getIsFavorite());
        values.put(PasswordDatabaseHelper.COL_UPDATED_AT, getCurrentDateTime());
        values.put(PasswordDatabaseHelper.COL_RECORD_VERSION, record.getRecordVersion() + 1);

        return db.update(
                PasswordDatabaseHelper.TABLE_PASSWORD_RECORDS,
                values,
                PasswordDatabaseHelper.COL_ID + " = ?",
                new String[]{String.valueOf(record.getId())}
        );
    }

    /**
     * यह Function ID के आधार पर Password Record को डेटाबेस से स्थायी रूप से हटाता है।
     *
     * @param id हटाए जाने वाले रिकॉर्ड की ID
     * @return प्रभावित पंक्तियों की संख्या
     */
    public int deletePasswordRecord(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(
                PasswordDatabaseHelper.TABLE_PASSWORD_RECORDS,
                PasswordDatabaseHelper.COL_ID + " = ?",
                new String[]{String.valueOf(id)}
        );
    }

    /**
     * यह Function रिकॉर्ड ID के आधार पर एक विशिष्ट Password Record निकालता है।
     *
     * @param id रिकॉर्ड की पहचान संख्या
     * @return प्राप्त PasswordRecord ऑब्जेक्ट अथवा null
     */
    public PasswordRecord getPasswordRecordById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                PasswordDatabaseHelper.TABLE_PASSWORD_RECORDS,
                null,
                PasswordDatabaseHelper.COL_ID + " = ?",
                new String[]{String.valueOf(id)},
                null,
                null,
                null
        );

        PasswordRecord record = null;
        if (cursor != null && cursor.moveToFirst()) {
            record = mapCursorToPasswordRecord(cursor);
            cursor.close();
        }
        return record;
    }

    /**
     * यह Function सभी सहेजे गए Password Records की सूची क्रमानुसार प्रदान करता है।
     *
     * @param sortType छँटाई का प्रकार (RECENT, A_Z, Z_A, FAVORITES_FIRST)
     * @return सभी Password Records की सूची
     */
    public List<PasswordRecord> getAllPasswordRecords(String sortType) {
        List<PasswordRecord> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String orderBy;
        if ("A_Z".equalsIgnoreCase(sortType)) {
            orderBy = PasswordDatabaseHelper.COL_WEBSITE_NAME + " COLLATE NOCASE ASC";
        } else if ("Z_A".equalsIgnoreCase(sortType)) {
            orderBy = PasswordDatabaseHelper.COL_WEBSITE_NAME + " COLLATE NOCASE DESC";
        } else if ("FAVORITES_FIRST".equalsIgnoreCase(sortType)) {
            orderBy = PasswordDatabaseHelper.COL_IS_FAVORITE + " DESC, " + PasswordDatabaseHelper.COL_UPDATED_AT + " DESC";
        } else {
            // RECENT
            orderBy = PasswordDatabaseHelper.COL_UPDATED_AT + " DESC";
        }

        Cursor cursor = db.query(
                PasswordDatabaseHelper.TABLE_PASSWORD_RECORDS,
                null,
                null,
                null,
                null,
                null,
                orderBy
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(mapCursorToPasswordRecord(cursor));
            }
            cursor.close();
        }
        return list;
    }

    /**
     * यह Function वेबसाइट नाम, वेबसाइट URL या रोल के आधार पर सुरक्षित रूप से खोज करता है।
     * (ध्यान दें: पासवर्ड डेटा कभी भी खोज इंडेक्स में शामिल नहीं किया जाता)
     *
     * @param query खोज शब्द
     * @return मेल खाते Records की सूची
     */
    public List<PasswordRecord> searchPasswordRecords(String query) {
        List<PasswordRecord> list = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return getAllPasswordRecords("RECENT");
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String wildQuery = "%" + query.trim() + "%";
        String selection = PasswordDatabaseHelper.COL_WEBSITE_NAME + " LIKE ? OR "
                + PasswordDatabaseHelper.COL_WEBSITE_URL + " LIKE ? OR "
                + PasswordDatabaseHelper.COL_ROLE + " LIKE ?";
        String[] selectionArgs = new String[]{wildQuery, wildQuery, wildQuery};

        Cursor cursor = db.query(
                PasswordDatabaseHelper.TABLE_PASSWORD_RECORDS,
                null,
                selection,
                selectionArgs,
                null,
                null,
                PasswordDatabaseHelper.COL_WEBSITE_NAME + " COLLATE NOCASE ASC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(mapCursorToPasswordRecord(cursor));
            }
            cursor.close();
        }
        return list;
    }

    /**
     * पसंदीदा (Favorite) स्थिति को टॉगल करने का कार्य।
     */
    public boolean toggleFavorite(long id, boolean isFavorite) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(PasswordDatabaseHelper.COL_IS_FAVORITE, isFavorite ? 1 : 0);
        values.put(PasswordDatabaseHelper.COL_UPDATED_AT, getCurrentDateTime());
        int count = db.update(
                PasswordDatabaseHelper.TABLE_PASSWORD_RECORDS,
                values,
                PasswordDatabaseHelper.COL_ID + " = ?",
                new String[]{String.valueOf(id)}
        );
        return count > 0;
    }

    /**
     * कुल सहेजे गए खातों की संख्या प्राप्त करता है।
     */
    public int getTotalPasswordCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + PasswordDatabaseHelper.TABLE_PASSWORD_RECORDS, null);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    /**
     * पसंदीदा खातों की संख्या प्राप्त करता है।
     */
    public int getFavoriteCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + PasswordDatabaseHelper.TABLE_PASSWORD_RECORDS + " WHERE " + PasswordDatabaseHelper.COL_IS_FAVORITE + " = 1", null);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    /**
     * हाल ही में जोड़े गए सीमित रिकॉर्ड्स प्राप्त करना।
     */
    public List<PasswordRecord> getRecentPasswordRecords(int limit) {
        List<PasswordRecord> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                PasswordDatabaseHelper.TABLE_PASSWORD_RECORDS,
                null,
                null,
                null,
                null,
                null,
                PasswordDatabaseHelper.COL_CREATED_AT + " DESC",
                String.valueOf(limit)
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(mapCursorToPasswordRecord(cursor));
            }
            cursor.close();
        }
        return list;
    }

    // =========================================================================
    // पेयर किए गए डिवाइसेज (Chrome Web Sessions) CRUD
    // =========================================================================

    /**
     * नए पेयर किए गए डिवाइस को डेटाबेस में सेव या अपडेट करता है।
     */
    public long savePairedDevice(PairedDevice device) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(PasswordDatabaseHelper.COL_DEVICE_NAME, device.getDeviceName());
        values.put(PasswordDatabaseHelper.COL_DEVICE_TYPE, device.getDeviceType());
        values.put(PasswordDatabaseHelper.COL_DEVICE_IDENTIFIER, device.getDeviceIdentifier());
        values.put(PasswordDatabaseHelper.COL_PAIRING_STATUS, device.getPairingStatus());
        values.put(PasswordDatabaseHelper.COL_PAIRED_AT, device.getPairedAt() != null ? device.getPairedAt() : getCurrentDateTime());
        values.put(PasswordDatabaseHelper.COL_LAST_CONNECTED_AT, getCurrentDateTime());

        return db.insertWithOnConflict(
                PasswordDatabaseHelper.TABLE_PAIRED_DEVICES,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
        );
    }

    /**
     * डिवाइस आईडेंटिफायर के आधार पर पेयर किए गए डिवाइस की जानकारी प्राप्त करता है।
     */
    public PairedDevice getPairedDeviceByIdentifier(String identifier) {
        if (identifier == null) return null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                PasswordDatabaseHelper.TABLE_PAIRED_DEVICES,
                null,
                PasswordDatabaseHelper.COL_DEVICE_IDENTIFIER + " = ?",
                new String[]{identifier},
                null,
                null,
                null
        );

        PairedDevice device = null;
        if (cursor != null && cursor.moveToFirst()) {
            device = mapCursorToPairedDevice(cursor);
            cursor.close();
        }
        return device;
    }

    /**
     * सभी पेयर किए गए डिवाइसेज की सूची प्राप्त करना।
     */
    public List<PairedDevice> getAllPairedDevices() {
        List<PairedDevice> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                PasswordDatabaseHelper.TABLE_PAIRED_DEVICES,
                null,
                null,
                null,
                null,
                null,
                PasswordDatabaseHelper.COL_LAST_CONNECTED_AT + " DESC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(mapCursorToPairedDevice(cursor));
            }
            cursor.close();
        }
        return list;
    }

    /**
     * डिवाइस की पेयरिंग स्थिति अपडेट करता है (उदा. APPROVED, REVOKED, PENDING)।
     */
    public boolean updatePairedDeviceStatus(String identifier, String status) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(PasswordDatabaseHelper.COL_PAIRING_STATUS, status);
        values.put(PasswordDatabaseHelper.COL_LAST_CONNECTED_AT, getCurrentDateTime());
        if ("REVOKED".equalsIgnoreCase(status)) {
            values.put(PasswordDatabaseHelper.COL_REVOKED_AT, getCurrentDateTime());
        }

        int count = db.update(
                PasswordDatabaseHelper.TABLE_PAIRED_DEVICES,
                values,
                PasswordDatabaseHelper.COL_DEVICE_IDENTIFIER + " = ?",
                new String[]{identifier}
        );
        return count > 0;
    }

    /**
     * डिवाइस के अंतिम कनेक्शन समय (Last Connected At) को अपडेट करता है।
     */
    public void updatePairedDeviceLastSeen(String identifier) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(PasswordDatabaseHelper.COL_LAST_CONNECTED_AT, getCurrentDateTime());
        db.update(
                PasswordDatabaseHelper.TABLE_PAIRED_DEVICES,
                values,
                PasswordDatabaseHelper.COL_DEVICE_IDENTIFIER + " = ?",
                new String[]{identifier}
        );
    }

    /**
     * पेयर किए गए डिवाइस की अनुमति रद्द (Revoke/Disconnect) करना।
     */
    public boolean revokePairedDevice(String identifier) {
        return updatePairedDeviceStatus(identifier, "REVOKED");
    }

    /**
     * पेयर डिवाइस को डेटाबेस से स्थायी रूप से हटाना।
     */
    public boolean deletePairedDevice(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count = db.delete(
                PasswordDatabaseHelper.TABLE_PAIRED_DEVICES,
                PasswordDatabaseHelper.COL_DEVICE_ID + " = ?",
                new String[]{String.valueOf(id)}
        );
        return count > 0;
    }

    // =========================================================================
    // ऑडिट लॉग्स (Audit Logs)
    // =========================================================================

    /**
     * सुरक्षा व डेटाबेस क्रियाकलाप के लिए ऑडिट लॉग प्रविष्टि दर्ज करता है।
     */
    public void insertAuditLog(long recordId, String actionType, String deviceIdentifier) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(PasswordDatabaseHelper.COL_LOG_RECORD_ID, recordId);
            values.put(PasswordDatabaseHelper.COL_LOG_ACTION_TYPE, actionType);
            values.put(PasswordDatabaseHelper.COL_LOG_DEVICE_IDENTIFIER, deviceIdentifier);
            values.put(PasswordDatabaseHelper.COL_LOG_TIMESTAMP, getCurrentDateTime());
            db.insert(PasswordDatabaseHelper.TABLE_AUDIT_LOGS, null, values);
        } catch (Exception ignored) {
        }
    }

    /**
     * हाल के ऑडिट लॉग्स की सूची प्राप्त करना।
     */
    public List<AuditLog> getRecentAuditLogs(int limit) {
        List<AuditLog> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                PasswordDatabaseHelper.TABLE_AUDIT_LOGS,
                null,
                null,
                null,
                null,
                null,
                PasswordDatabaseHelper.COL_LOG_TIMESTAMP + " DESC",
                String.valueOf(limit)
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                AuditLog log = new AuditLog();
                log.setId(cursor.getLong(cursor.getColumnIndexOrThrow(PasswordDatabaseHelper.COL_LOG_ID)));
                log.setRecordId(cursor.getLong(cursor.getColumnIndexOrThrow(PasswordDatabaseHelper.COL_LOG_RECORD_ID)));
                log.setActionType(cursor.getString(cursor.getColumnIndexOrThrow(PasswordDatabaseHelper.COL_LOG_ACTION_TYPE)));
                log.setDeviceIdentifier(cursor.getString(cursor.getColumnIndexOrThrow(PasswordDatabaseHelper.COL_LOG_DEVICE_IDENTIFIER)));
                log.setActionTimestamp(cursor.getString(cursor.getColumnIndexOrThrow(PasswordDatabaseHelper.COL_LOG_TIMESTAMP)));
                list.add(log);
            }
            cursor.close();
        }
        return list;
    }

    // =========================================================================
    // एप्लीकेशन सेटिंग्स व सुरक्षा डेटा
    // =========================================================================

    /**
     * एप्लीकेशन सेटिंग्स प्राप्त करता है।
     */
    public AppSettings getAppSettings() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(PasswordDatabaseHelper.TABLE_APP_SETTINGS, null, PasswordDatabaseHelper.COL_SETTINGS_ID + " = 1", null, null, null, null);
        AppSettings settings = new AppSettings();
        if (cursor != null && cursor.moveToFirst()) {
            settings.setId(cursor.getInt(cursor.getColumnIndexOrThrow(PasswordDatabaseHelper.COL_SETTINGS_ID)));
            settings.setAutoLockMinutes(cursor.getInt(cursor.getColumnIndexOrThrow(PasswordDatabaseHelper.COL_AUTO_LOCK_MINUTES)));
            settings.setBiometricEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(PasswordDatabaseHelper.COL_BIOMETRIC_ENABLED)));
            settings.setClipboardClearEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(PasswordDatabaseHelper.COL_CLIPBOARD_CLEAR_ENABLED)));
            settings.setThemeMode(cursor.getString(cursor.getColumnIndexOrThrow(PasswordDatabaseHelper.COL_THEME_MODE)));
            
            int serverEnabledIdx = cursor.getColumnIndex(PasswordDatabaseHelper.COL_SERVER_ENABLED);
            if (serverEnabledIdx != -1) {
                settings.setServerEnabled(cursor.getInt(serverEnabledIdx));
            }
            int serverPortIdx = cursor.getColumnIndex(PasswordDatabaseHelper.COL_SERVER_PORT);
            if (serverPortIdx != -1) {
                settings.setServerPort(cursor.getInt(serverPortIdx));
            }

            int genLenIdx = cursor.getColumnIndex(PasswordDatabaseHelper.COL_GEN_LENGTH);
            if (genLenIdx != -1 && !cursor.isNull(genLenIdx)) {
                settings.setGenLength(cursor.getInt(genLenIdx));
            }
            int genUpperIdx = cursor.getColumnIndex(PasswordDatabaseHelper.COL_GEN_UPPERCASE);
            if (genUpperIdx != -1 && !cursor.isNull(genUpperIdx)) {
                settings.setGenIncludeUppercase(cursor.getInt(genUpperIdx));
            }
            int genLowerIdx = cursor.getColumnIndex(PasswordDatabaseHelper.COL_GEN_LOWERCASE);
            if (genLowerIdx != -1 && !cursor.isNull(genLowerIdx)) {
                settings.setGenIncludeLowercase(cursor.getInt(genLowerIdx));
            }
            int genNumsIdx = cursor.getColumnIndex(PasswordDatabaseHelper.COL_GEN_NUMBERS);
            if (genNumsIdx != -1 && !cursor.isNull(genNumsIdx)) {
                settings.setGenIncludeNumbers(cursor.getInt(genNumsIdx));
            }
            int genSymsIdx = cursor.getColumnIndex(PasswordDatabaseHelper.COL_GEN_SYMBOLS);
            if (genSymsIdx != -1 && !cursor.isNull(genSymsIdx)) {
                settings.setGenIncludeSymbols(cursor.getInt(genSymsIdx));
            }
            cursor.close();
        }
        return settings;
    }

    /**
     * एप्लीकेशन सेटिंग्स सुरक्षित करता है।
     */
    public boolean saveAppSettings(AppSettings settings) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(PasswordDatabaseHelper.COL_SETTINGS_ID, 1);
        values.put(PasswordDatabaseHelper.COL_AUTO_LOCK_MINUTES, settings.getAutoLockMinutes());
        values.put(PasswordDatabaseHelper.COL_BIOMETRIC_ENABLED, settings.getBiometricEnabled());
        values.put(PasswordDatabaseHelper.COL_CLIPBOARD_CLEAR_ENABLED, settings.getClipboardClearEnabled());
        values.put(PasswordDatabaseHelper.COL_THEME_MODE, settings.getThemeMode());
        values.put(PasswordDatabaseHelper.COL_SERVER_ENABLED, settings.getServerEnabled());
        values.put(PasswordDatabaseHelper.COL_SERVER_PORT, settings.getServerPort());
        values.put(PasswordDatabaseHelper.COL_GEN_LENGTH, settings.getGenLength());
        values.put(PasswordDatabaseHelper.COL_GEN_UPPERCASE, settings.getGenIncludeUppercase());
        values.put(PasswordDatabaseHelper.COL_GEN_LOWERCASE, settings.getGenIncludeLowercase());
        values.put(PasswordDatabaseHelper.COL_GEN_NUMBERS, settings.getGenIncludeNumbers());
        values.put(PasswordDatabaseHelper.COL_GEN_SYMBOLS, settings.getGenIncludeSymbols());

        long result = db.insertWithOnConflict(
                PasswordDatabaseHelper.TABLE_APP_SETTINGS,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
        );
        return result != -1;
    }

    /**
     * सुरक्षा संबंधी मेटाडेटा (मास्टर पासवर्ड हैश व साल्ट) प्राप्त करता है।
     */
    public SecurityData getSecurityData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(PasswordDatabaseHelper.TABLE_SECURITY_DATA, null, PasswordDatabaseHelper.COL_SEC_ID + " = 1", null, null, null, null);
        SecurityData data = null;
        if (cursor != null && cursor.moveToFirst()) {
            data = new SecurityData();
            data.setId(cursor.getInt(cursor.getColumnIndexOrThrow(PasswordDatabaseHelper.COL_SEC_ID)));
            data.setMasterPasswordHash(cursor.getString(cursor.getColumnIndexOrThrow(PasswordDatabaseHelper.COL_MASTER_PASSWORD_HASH)));
            data.setMasterPasswordSalt(cursor.getBlob(cursor.getColumnIndexOrThrow(PasswordDatabaseHelper.COL_MASTER_PASSWORD_SALT)));
            data.setEncryptionMetadata(cursor.getBlob(cursor.getColumnIndexOrThrow(PasswordDatabaseHelper.COL_ENCRYPTION_METADATA)));

            int qIdx = cursor.getColumnIndex(PasswordDatabaseHelper.COL_SECURITY_QUESTION);
            if (qIdx != -1) {
                data.setSecurityQuestion(cursor.getString(qIdx));
            }
            int ansHashIdx = cursor.getColumnIndex(PasswordDatabaseHelper.COL_SECURITY_ANSWER_HASH);
            if (ansHashIdx != -1) {
                data.setSecurityAnswerHash(cursor.getString(ansHashIdx));
            }
            int ansSaltIdx = cursor.getColumnIndex(PasswordDatabaseHelper.COL_SECURITY_ANSWER_SALT);
            if (ansSaltIdx != -1) {
                data.setSecurityAnswerSalt(cursor.getBlob(ansSaltIdx));
            }

            data.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(PasswordDatabaseHelper.COL_SEC_CREATED_AT)));
            cursor.close();
        }
        return data;
    }

    /**
     * सुरक्षा संबंधी मेटाडेटा सुरक्षित करता है।
     */
    public boolean saveSecurityData(SecurityData data) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(PasswordDatabaseHelper.COL_SEC_ID, 1);
        values.put(PasswordDatabaseHelper.COL_MASTER_PASSWORD_HASH, data.getMasterPasswordHash());
        values.put(PasswordDatabaseHelper.COL_MASTER_PASSWORD_SALT, data.getMasterPasswordSalt());
        values.put(PasswordDatabaseHelper.COL_ENCRYPTION_METADATA, data.getEncryptionMetadata());
        values.put(PasswordDatabaseHelper.COL_SECURITY_QUESTION, data.getSecurityQuestion());
        values.put(PasswordDatabaseHelper.COL_SECURITY_ANSWER_HASH, data.getSecurityAnswerHash());
        values.put(PasswordDatabaseHelper.COL_SECURITY_ANSWER_SALT, data.getSecurityAnswerSalt());
        values.put(PasswordDatabaseHelper.COL_SEC_CREATED_AT, getCurrentDateTime());

        long result = db.insertWithOnConflict(
                PasswordDatabaseHelper.TABLE_SECURITY_DATA,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
        );
        return result != -1;
    }

    /**
     * जांचता है कि क्या मास्टर पासवर्ड पहले से सेटअप किया गया है या नहीं।
     */
    public boolean hasSecuritySetup() {
        SecurityData data = getSecurityData();
        return data != null && data.getMasterPasswordHash() != null && !data.getMasterPasswordHash().isEmpty();
    }

    /**
     * डेटाबेस को रीसेट करने का कार्य (सभी डेटा मिटाता है)।
     */
    public void resetAllData() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(PasswordDatabaseHelper.TABLE_PASSWORD_RECORDS, null, null);
        db.delete(PasswordDatabaseHelper.TABLE_SECURITY_DATA, null, null);
        db.delete(PasswordDatabaseHelper.TABLE_PAIRED_DEVICES, null, null);
        db.delete(PasswordDatabaseHelper.TABLE_AUDIT_LOGS, null, null);
    }

    private PasswordRecord mapCursorToPasswordRecord(Cursor cursor) {
        PasswordRecord record = new PasswordRecord();
        record.setId(cursor.getLong(cursor.getColumnIndexOrThrow(PasswordDatabaseHelper.COL_ID)));
        record.setWebsiteName(cursor.getString(cursor.getColumnIndexOrThrow(PasswordDatabaseHelper.COL_WEBSITE_NAME)));
        record.setWebsiteUrl(cursor.getString(cursor.getColumnIndexOrThrow(PasswordDatabaseHelper.COL_WEBSITE_URL)));
        record.setUsernameEncrypted(cursor.getBlob(cursor.getColumnIndexOrThrow(PasswordDatabaseHelper.COL_USERNAME_ENCRYPTED)));
        record.setPasswordEncrypted(cursor.getBlob(cursor.getColumnIndexOrThrow(PasswordDatabaseHelper.COL_PASSWORD_ENCRYPTED)));
        record.setRole(cursor.getString(cursor.getColumnIndexOrThrow(PasswordDatabaseHelper.COL_ROLE)));
        record.setScreenshotBlob(cursor.getBlob(cursor.getColumnIndexOrThrow(PasswordDatabaseHelper.COL_SCREENSHOT_BLOB)));
        record.setScreenshotMimeType(cursor.getString(cursor.getColumnIndexOrThrow(PasswordDatabaseHelper.COL_SCREENSHOT_MIME_TYPE)));
        record.setNotesEncrypted(cursor.getBlob(cursor.getColumnIndexOrThrow(PasswordDatabaseHelper.COL_NOTES_ENCRYPTED)));
        record.setIsFavorite(cursor.getInt(cursor.getColumnIndexOrThrow(PasswordDatabaseHelper.COL_IS_FAVORITE)));
        record.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(PasswordDatabaseHelper.COL_CREATED_AT)));
        record.setUpdatedAt(cursor.getString(cursor.getColumnIndexOrThrow(PasswordDatabaseHelper.COL_UPDATED_AT)));

        int versionIdx = cursor.getColumnIndex(PasswordDatabaseHelper.COL_RECORD_VERSION);
        if (versionIdx != -1) {
            record.setRecordVersion(cursor.getInt(versionIdx));
        }
        return record;
    }

    private PairedDevice mapCursorToPairedDevice(Cursor cursor) {
        PairedDevice device = new PairedDevice();
        device.setId(cursor.getLong(cursor.getColumnIndexOrThrow(PasswordDatabaseHelper.COL_DEVICE_ID)));
        device.setDeviceName(cursor.getString(cursor.getColumnIndexOrThrow(PasswordDatabaseHelper.COL_DEVICE_NAME)));
        device.setDeviceType(cursor.getString(cursor.getColumnIndexOrThrow(PasswordDatabaseHelper.COL_DEVICE_TYPE)));
        device.setDeviceIdentifier(cursor.getString(cursor.getColumnIndexOrThrow(PasswordDatabaseHelper.COL_DEVICE_IDENTIFIER)));
        device.setPairingStatus(cursor.getString(cursor.getColumnIndexOrThrow(PasswordDatabaseHelper.COL_PAIRING_STATUS)));
        device.setPairedAt(cursor.getString(cursor.getColumnIndexOrThrow(PasswordDatabaseHelper.COL_PAIRED_AT)));
        device.setLastConnectedAt(cursor.getString(cursor.getColumnIndexOrThrow(PasswordDatabaseHelper.COL_LAST_CONNECTED_AT)));
        device.setRevokedAt(cursor.getString(cursor.getColumnIndexOrThrow(PasswordDatabaseHelper.COL_REVOKED_AT)));
        return device;
    }
}
