package jmd.shiv.mypassword.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * यह क्लास SQLite डेटाबेस स्कीमा और माइग्रेशन का प्रबंधन करती है।
 * डेटाबेस का नाम: password_manager.db (संस्करण 2: वेब एक्सेस, पेयरिंग और ऑडिट लॉग्स सपोर्ट)
 */
public class PasswordDatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "password_manager.db";
    public static final int DATABASE_VERSION = 4;

    // टेबल नाम
    public static final String TABLE_PASSWORD_RECORDS = "password_records";
    public static final String TABLE_PAIRED_DEVICES = "paired_devices";
    public static final String TABLE_APP_SETTINGS = "app_settings";
    public static final String TABLE_SECURITY_DATA = "security_data";
    public static final String TABLE_AUDIT_LOGS = "audit_logs";

    // कॉलम: password_records
    public static final String COL_ID = "id";
    public static final String COL_WEBSITE_NAME = "website_name";
    public static final String COL_WEBSITE_URL = "website_url";
    public static final String COL_USERNAME_ENCRYPTED = "username_encrypted";
    public static final String COL_PASSWORD_ENCRYPTED = "password_encrypted";
    public static final String COL_ROLE = "role";
    public static final String COL_SCREENSHOT_BLOB = "screenshot_blob";
    public static final String COL_SCREENSHOT_MIME_TYPE = "screenshot_mime_type";
    public static final String COL_NOTES_ENCRYPTED = "notes_encrypted";
    public static final String COL_IS_FAVORITE = "is_favorite";
    public static final String COL_CREATED_AT = "created_at";
    public static final String COL_UPDATED_AT = "updated_at";
    public static final String COL_RECORD_VERSION = "record_version";

    // कॉलम: paired_devices
    public static final String COL_DEVICE_ID = "id";
    public static final String COL_DEVICE_NAME = "device_name";
    public static final String COL_DEVICE_TYPE = "device_type";
    public static final String COL_DEVICE_IDENTIFIER = "device_identifier";
    public static final String COL_PAIRING_STATUS = "pairing_status";
    public static final String COL_PAIRED_AT = "paired_at";
    public static final String COL_LAST_CONNECTED_AT = "last_connected_at";
    public static final String COL_REVOKED_AT = "revoked_at";

    // कॉलम: app_settings
    public static final String COL_SETTINGS_ID = "id";
    public static final String COL_AUTO_LOCK_MINUTES = "auto_lock_minutes";
    public static final String COL_BIOMETRIC_ENABLED = "biometric_enabled";
    public static final String COL_CLIPBOARD_CLEAR_ENABLED = "clipboard_clear_enabled";
    public static final String COL_THEME_MODE = "theme_mode";
    public static final String COL_SERVER_ENABLED = "server_enabled";
    public static final String COL_SERVER_PORT = "server_port";
    public static final String COL_GEN_LENGTH = "gen_length";
    public static final String COL_GEN_UPPERCASE = "gen_include_uppercase";
    public static final String COL_GEN_LOWERCASE = "gen_include_lowercase";
    public static final String COL_GEN_NUMBERS = "gen_include_numbers";
    public static final String COL_GEN_SYMBOLS = "gen_include_symbols";

    // कॉलम: security_data
    public static final String COL_SEC_ID = "id";
    public static final String COL_MASTER_PASSWORD_HASH = "master_password_hash";
    public static final String COL_MASTER_PASSWORD_SALT = "master_password_salt";
    public static final String COL_ENCRYPTION_METADATA = "encryption_metadata";
    public static final String COL_SECURITY_QUESTION = "security_question";
    public static final String COL_SECURITY_ANSWER_HASH = "security_answer_hash";
    public static final String COL_SECURITY_ANSWER_SALT = "security_answer_salt";
    public static final String COL_SEC_CREATED_AT = "created_at";

    // कॉलम: audit_logs
    public static final String COL_LOG_ID = "id";
    public static final String COL_LOG_RECORD_ID = "record_id";
    public static final String COL_LOG_ACTION_TYPE = "action_type";
    public static final String COL_LOG_DEVICE_IDENTIFIER = "device_identifier";
    public static final String COL_LOG_TIMESTAMP = "action_timestamp";

    private static PasswordDatabaseHelper sInstance;

    public static synchronized PasswordDatabaseHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new PasswordDatabaseHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    private PasswordDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // टेबल 1: password_records
        String createRecordsTable = "CREATE TABLE " + TABLE_PASSWORD_RECORDS + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_WEBSITE_NAME + " TEXT NOT NULL, "
                + COL_WEBSITE_URL + " TEXT, "
                + COL_USERNAME_ENCRYPTED + " BLOB, "
                + COL_PASSWORD_ENCRYPTED + " BLOB, "
                + COL_ROLE + " TEXT, "
                + COL_SCREENSHOT_BLOB + " BLOB, "
                + COL_SCREENSHOT_MIME_TYPE + " TEXT, "
                + COL_NOTES_ENCRYPTED + " BLOB, "
                + COL_IS_FAVORITE + " INTEGER DEFAULT 0, "
                + COL_CREATED_AT + " TEXT, "
                + COL_UPDATED_AT + " TEXT, "
                + COL_RECORD_VERSION + " INTEGER DEFAULT 1"
                + ");";

        // टेबल 2: paired_devices (Chrome / Browser Pairing)
        String createPairedDevicesTable = "CREATE TABLE " + TABLE_PAIRED_DEVICES + " ("
                + COL_DEVICE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_DEVICE_NAME + " TEXT, "
                + COL_DEVICE_TYPE + " TEXT, "
                + COL_DEVICE_IDENTIFIER + " TEXT UNIQUE, "
                + COL_PAIRING_STATUS + " TEXT DEFAULT 'PENDING', "
                + COL_PAIRED_AT + " TEXT, "
                + COL_LAST_CONNECTED_AT + " TEXT, "
                + COL_REVOKED_AT + " TEXT"
                + ");";

        // टेबल 3: app_settings
        String createSettingsTable = "CREATE TABLE " + TABLE_APP_SETTINGS + " ("
                + COL_SETTINGS_ID + " INTEGER PRIMARY KEY, "
                + COL_AUTO_LOCK_MINUTES + " INTEGER DEFAULT 5, "
                + COL_BIOMETRIC_ENABLED + " INTEGER DEFAULT 0, "
                + COL_CLIPBOARD_CLEAR_ENABLED + " INTEGER DEFAULT 1, "
                + COL_THEME_MODE + " TEXT DEFAULT 'LIGHT', "
                + COL_SERVER_ENABLED + " INTEGER DEFAULT 0, "
                + COL_SERVER_PORT + " INTEGER DEFAULT 8443, "
                + COL_GEN_LENGTH + " INTEGER DEFAULT 12, "
                + COL_GEN_UPPERCASE + " INTEGER DEFAULT 1, "
                + COL_GEN_LOWERCASE + " INTEGER DEFAULT 1, "
                + COL_GEN_NUMBERS + " INTEGER DEFAULT 1, "
                + COL_GEN_SYMBOLS + " INTEGER DEFAULT 1"
                + ");";

        // टेबल 4: security_data
        String createSecurityTable = "CREATE TABLE " + TABLE_SECURITY_DATA + " ("
                + COL_SEC_ID + " INTEGER PRIMARY KEY, "
                + COL_MASTER_PASSWORD_HASH + " TEXT NOT NULL, "
                + COL_MASTER_PASSWORD_SALT + " BLOB NOT NULL, "
                + COL_ENCRYPTION_METADATA + " BLOB, "
                + COL_SECURITY_QUESTION + " TEXT, "
                + COL_SECURITY_ANSWER_HASH + " TEXT, "
                + COL_SECURITY_ANSWER_SALT + " BLOB, "
                + COL_SEC_CREATED_AT + " TEXT"
                + ");";

        // टेबल 5: audit_logs (नो संवेदनशील क्रेडेंशियल्स)
        String createAuditTable = "CREATE TABLE " + TABLE_AUDIT_LOGS + " ("
                + COL_LOG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_LOG_RECORD_ID + " INTEGER, "
                + COL_LOG_ACTION_TYPE + " TEXT NOT NULL, "
                + COL_LOG_DEVICE_IDENTIFIER + " TEXT, "
                + COL_LOG_TIMESTAMP + " TEXT NOT NULL"
                + ");";

        db.execSQL(createRecordsTable);
        db.execSQL(createPairedDevicesTable);
        db.execSQL(createSettingsTable);
        db.execSQL(createSecurityTable);
        db.execSQL(createAuditTable);

        // डिफ़ॉल्ट सेटिंग्स प्रविष्टि
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_APP_SETTINGS + " ("
                + COL_SETTINGS_ID + ", "
                + COL_AUTO_LOCK_MINUTES + ", "
                + COL_BIOMETRIC_ENABLED + ", "
                + COL_CLIPBOARD_CLEAR_ENABLED + ", "
                + COL_THEME_MODE + ", "
                + COL_SERVER_ENABLED + ", "
                + COL_SERVER_PORT + ", "
                + COL_GEN_LENGTH + ", "
                + COL_GEN_UPPERCASE + ", "
                + COL_GEN_LOWERCASE + ", "
                + COL_GEN_NUMBERS + ", "
                + COL_GEN_SYMBOLS + ") VALUES (1, 5, 0, 1, 'LIGHT', 0, 8443, 12, 1, 1, 1, 1);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_PASSWORD_RECORDS + " ADD COLUMN " + COL_RECORD_VERSION + " INTEGER DEFAULT 1;");
            } catch (Exception ignored) {
            }
            try {
                db.execSQL("ALTER TABLE " + TABLE_APP_SETTINGS + " ADD COLUMN " + COL_SERVER_ENABLED + " INTEGER DEFAULT 0;");
                db.execSQL("ALTER TABLE " + TABLE_APP_SETTINGS + " ADD COLUMN " + COL_SERVER_PORT + " INTEGER DEFAULT 8443;");
            } catch (Exception ignored) {
            }

            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PAIRED_DEVICES + " ("
                    + COL_DEVICE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COL_DEVICE_NAME + " TEXT, "
                    + COL_DEVICE_TYPE + " TEXT, "
                    + COL_DEVICE_IDENTIFIER + " TEXT UNIQUE, "
                    + COL_PAIRING_STATUS + " TEXT DEFAULT 'PENDING', "
                    + COL_PAIRED_AT + " TEXT, "
                    + COL_LAST_CONNECTED_AT + " TEXT, "
                    + COL_REVOKED_AT + " TEXT"
                    + ");");

            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_AUDIT_LOGS + " ("
                    + COL_LOG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COL_LOG_RECORD_ID + " INTEGER, "
                    + COL_LOG_ACTION_TYPE + " TEXT NOT NULL, "
                    + COL_LOG_DEVICE_IDENTIFIER + " TEXT, "
                    + COL_LOG_TIMESTAMP + " TEXT NOT NULL"
                    + ");");
        }

        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_SECURITY_DATA + " ADD COLUMN " + COL_SECURITY_QUESTION + " TEXT;");
                db.execSQL("ALTER TABLE " + TABLE_SECURITY_DATA + " ADD COLUMN " + COL_SECURITY_ANSWER_HASH + " TEXT;");
                db.execSQL("ALTER TABLE " + TABLE_SECURITY_DATA + " ADD COLUMN " + COL_SECURITY_ANSWER_SALT + " BLOB;");
            } catch (Exception ignored) {
            }
        }

        if (oldVersion < 4) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_APP_SETTINGS + " ADD COLUMN " + COL_GEN_LENGTH + " INTEGER DEFAULT 12;");
                db.execSQL("ALTER TABLE " + TABLE_APP_SETTINGS + " ADD COLUMN " + COL_GEN_UPPERCASE + " INTEGER DEFAULT 1;");
                db.execSQL("ALTER TABLE " + TABLE_APP_SETTINGS + " ADD COLUMN " + COL_GEN_LOWERCASE + " INTEGER DEFAULT 1;");
                db.execSQL("ALTER TABLE " + TABLE_APP_SETTINGS + " ADD COLUMN " + COL_GEN_NUMBERS + " INTEGER DEFAULT 1;");
                db.execSQL("ALTER TABLE " + TABLE_APP_SETTINGS + " ADD COLUMN " + COL_GEN_SYMBOLS + " INTEGER DEFAULT 1;");
            } catch (Exception ignored) {
            }
        }
    }
}
