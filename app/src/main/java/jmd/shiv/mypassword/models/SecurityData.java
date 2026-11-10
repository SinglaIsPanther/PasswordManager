package jmd.shiv.mypassword.models;

/**
 * यह मॉडल क्लास मास्टर पासवर्ड हैश, साल्ट और सुरक्षा सम्बन्धी मेटाडेटा को प्रबंधित करती है।
 */
public class SecurityData {

    private int id;
    private String masterPasswordHash;
    private byte[] masterPasswordSalt;
    private byte[] encryptionMetadata;
    private String securityQuestion;
    private String securityAnswerHash;
    private byte[] securityAnswerSalt;
    private String createdAt;

    public SecurityData() {
        this.id = 1;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMasterPasswordHash() {
        return masterPasswordHash;
    }

    public void setMasterPasswordHash(String masterPasswordHash) {
        this.masterPasswordHash = masterPasswordHash;
    }

    public byte[] getMasterPasswordSalt() {
        return masterPasswordSalt;
    }

    public void setMasterPasswordSalt(byte[] masterPasswordSalt) {
        this.masterPasswordSalt = masterPasswordSalt;
    }

    public byte[] getEncryptionMetadata() {
        return encryptionMetadata;
    }

    public void setEncryptionMetadata(byte[] encryptionMetadata) {
        this.encryptionMetadata = encryptionMetadata;
    }

    public String getSecurityQuestion() {
        return securityQuestion;
    }

    public void setSecurityQuestion(String securityQuestion) {
        this.securityQuestion = securityQuestion;
    }

    public String getSecurityAnswerHash() {
        return securityAnswerHash;
    }

    public void setSecurityAnswerHash(String securityAnswerHash) {
        this.securityAnswerHash = securityAnswerHash;
    }

    public byte[] getSecurityAnswerSalt() {
        return securityAnswerSalt;
    }

    public void setSecurityAnswerSalt(byte[] securityAnswerSalt) {
        this.securityAnswerSalt = securityAnswerSalt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
