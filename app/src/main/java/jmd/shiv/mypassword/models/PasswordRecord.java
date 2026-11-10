package jmd.shiv.mypassword.models;

import java.io.Serializable;

/**
 * यह मॉडल क्लास सुरक्षित पासवर्ड रिकॉर्ड (Website, Username, Password, Role आदि) का डेटा संरचना प्रस्तुत करती है।
 */
public class PasswordRecord implements Serializable {

    private long id;
    private String websiteName;
    private String websiteUrl;
    private byte[] usernameEncrypted;
    private byte[] passwordEncrypted;
    private String role;
    private byte[] screenshotBlob;
    private String screenshotMimeType;
    private byte[] notesEncrypted;
    private int isFavorite;
    private String createdAt;
    private String updatedAt;
    private int recordVersion = 1;

    // डिक्रिप्टेड मान (केवल मेमोरी में अस्थायी उपयोग के लिए)
    private transient String decryptedUsername;
    private transient String decryptedPassword;
    private transient String decryptedNotes;

    public PasswordRecord() {
        this.isFavorite = 0;
    }

    /**
     * रिकॉर्ड ID प्राप्त करना
     */
    public long getId() {
        return id;
    }

    /**
     * रिकॉर्ड ID सेट करना
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * वेबसाइट का नाम प्राप्त करना
     */
    public String getWebsiteName() {
        return websiteName;
    }

    /**
     * वेबसाइट का नाम सेट करना
     */
    public void setWebsiteName(String websiteName) {
        this.websiteName = websiteName;
    }

    /**
     * वेबसाइट URL प्राप्त करना
     */
    public String getWebsiteUrl() {
        return websiteUrl;
    }

    /**
     * वेबसाइट URL सेट करना
     */
    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    /**
     * एन्क्रिप्टेड यूजरनेम बाइट्स प्राप्त करना
     */
    public byte[] getUsernameEncrypted() {
        return usernameEncrypted;
    }

    /**
     * एन्क्रिप्टेड यूजरनेम बाइट्स सेट करना
     */
    public void setUsernameEncrypted(byte[] usernameEncrypted) {
        this.usernameEncrypted = usernameEncrypted;
    }

    /**
     * एन्क्रिप्टेड पासवर्ड बाइट्स प्राप्त करना
     */
    public byte[] getPasswordEncrypted() {
        return passwordEncrypted;
    }

    /**
     * एन्क्रिप्टेड पासवर्ड बाइट्स सेट करना
     */
    public void setPasswordEncrypted(byte[] passwordEncrypted) {
        this.passwordEncrypted = passwordEncrypted;
    }

    /**
     * यूजर की भूमिका / पद प्राप्त करना
     */
    public String getRole() {
        return role;
    }

    /**
     * यूजर की भूमिका / पद सेट करना
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * एन्क्रिप्टेड स्क्रीनशॉट BLOB प्राप्त करना
     */
    public byte[] getScreenshotBlob() {
        return screenshotBlob;
    }

    /**
     * एन्क्रिप्टेड स्क्रीनशॉट BLOB सेट करना
     */
    public void setScreenshotBlob(byte[] screenshotBlob) {
        this.screenshotBlob = screenshotBlob;
    }

    /**
     * स्क्रीनशॉट का MIME टाइप प्राप्त करना
     */
    public String getScreenshotMimeType() {
        return screenshotMimeType;
    }

    /**
     * स्क्रीनशॉट का MIME टाइप सेट करना
     */
    public void setScreenshotMimeType(String screenshotMimeType) {
        this.screenshotMimeType = screenshotMimeType;
    }

    /**
     * एन्क्रिप्टेड नोट्स बाइट्स प्राप्त करना
     */
    public byte[] getNotesEncrypted() {
        return notesEncrypted;
    }

    /**
     * एन्क्रिप्टेड नोट्स बाइट्स सेट करना
     */
    public void setNotesEncrypted(byte[] notesEncrypted) {
        this.notesEncrypted = notesEncrypted;
    }

    /**
     * पसंदीदा स्थिति (1 = Favorite, 0 = Normal) प्राप्त करना
     */
    public int getIsFavorite() {
        return isFavorite;
    }

    /**
     * पसंदीदा स्थिति सेट करना
     */
    public void setIsFavorite(int isFavorite) {
        this.isFavorite = isFavorite;
    }

    /**
     * रिकॉर्ड निर्माण की तिथि प्राप्त करना
     */
    public String getCreatedAt() {
        return createdAt;
    }

    /**
     * रिकॉर्ड निर्माण की तिथि सेट करना
     */
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * रिकॉर्ड अपडेट की तिथि प्राप्त करना
     */
    public String getUpdatedAt() {
        return updatedAt;
    }

    /**
     * रिकॉर्ड अपडेट की तिथि सेट करना
     */
    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * रिकॉर्ड का वर्शन प्राप्त करना (Concurrency & Conflict Prevention)
     */
    public int getRecordVersion() {
        return recordVersion;
    }

    /**
     * रिकॉर्ड का वर्शन सेट करना
     */
    public void setRecordVersion(int recordVersion) {
        this.recordVersion = recordVersion;
    }

    /**
     * डिक्रिप्टेड यूजरनेम प्राप्त करना
     */
    public String getDecryptedUsername() {
        return decryptedUsername;
    }

    /**
     * डिक्रिप्टेड यूजरनेम सेट करना
     */
    public void setDecryptedUsername(String decryptedUsername) {
        this.decryptedUsername = decryptedUsername;
    }

    /**
     * डिक्रिप्टेड पासवर्ड प्राप्त करना
     */
    public String getDecryptedPassword() {
        return decryptedPassword;
    }

    /**
     * डिक्रिप्टेड पासवर्ड सेट करना
     */
    public void setDecryptedPassword(String decryptedPassword) {
        this.decryptedPassword = decryptedPassword;
    }

    /**
     * डिक्रिप्टेड नोट्स प्राप्त करना
     */
    public String getDecryptedNotes() {
        return decryptedNotes;
    }

    /**
     * डिक्रिप्टेड नोट्स सेट करना
     */
    public void setDecryptedNotes(String decryptedNotes) {
        this.decryptedNotes = decryptedNotes;
    }

    /**
     * मेमोरी से सभी संवेदनशील डिक्रिप्टेड स्ट्रिंग्स को साफ़ करने का प्रयास
     */
    public void clearSensitiveMemory() {
        this.decryptedUsername = null;
        this.decryptedPassword = null;
        this.decryptedNotes = null;
    }
}
