package jmd.shiv.mypassword.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * यह क्लास AES-256-GCM एन्क्रिप्शन और डिक्रिप्शन का केंद्रीय प्रबंधन करती है।
 * प्रत्येक एन्क्रिप्शन के लिए 12-बाइट का यूनिक IV/Nonce तैयार किया जाता है।
 */
public class EncryptionManager {

    private static final String AES_GCM_NO_PADDING = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int BACKUP_SALT_LENGTH_BYTES = 16;
    private static final int BACKUP_PBKDF2_ITERATIONS = 100000;

    /**
     * डेटा बाइट्स को AES-256-GCM से एन्क्रिप्ट करता है।
     * आउटपुट में पहले 12 बाइट IV और उसके बाद Ciphertext + Auth Tag शामिल होता है।
     *
     * @param plainBytes सादा डेटा बाइट्स
     * @param secretKey  AES-256 गुप्त कुंजी
     * @return एन्क्रिप्टेड बाइट्स (IV + Ciphertext)
     * @throws Exception एन्क्रिप्शन त्रुटि
     */
    public static byte[] encryptData(byte[] plainBytes, SecretKey secretKey) throws Exception {
        if (plainBytes == null || plainBytes.length == 0 || secretKey == null) {
            return null;
        }

        Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
        byte[] iv;

        try {
            byte[] generatedIv = new byte[GCM_IV_LENGTH_BYTES];
            new SecureRandom().nextBytes(generatedIv);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, generatedIv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
            iv = generatedIv;
        } catch (java.security.InvalidAlgorithmParameterException e) {
            // Android Keystore में यदि कॉलर-प्रोवाइडेड IV की अनुमति न हो तो स्वतः जनरेट करें
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            iv = cipher.getIV();
        }

        byte[] cipherText = cipher.doFinal(plainBytes);

        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
        byteBuffer.put(iv);
        byteBuffer.put(cipherText);
        return byteBuffer.array();
    }

    /**
     * AES-256-GCM द्वारा एन्क्रिप्टेड बाइट्स को डिक्रिप्ट करता है।
     *
     * @param encryptedPayload एन्क्रिप्टेड डेटा (IV + Ciphertext)
     * @param secretKey        AES-256 गुप्त कुंजी
     * @return मूल सादा बाइट्स
     * @throws Exception डिक्रिप्शन त्रुटि अथवा अनधिकृत बदलाव (Tampering)
     */
    public static byte[] decryptData(byte[] encryptedPayload, SecretKey secretKey) throws Exception {
        if (encryptedPayload == null || encryptedPayload.length <= GCM_IV_LENGTH_BYTES || secretKey == null) {
            return null;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedPayload);
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        byteBuffer.get(iv);

        byte[] cipherText = new byte[byteBuffer.remaining()];
        byteBuffer.get(cipherText);

        Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

        return cipher.doFinal(cipherText);
    }

    /**
     * स्ट्रिंग टेक्स्ट को एन्क्रिप्ट करके सुरक्षित बाइट ऐरे बनाता है।
     */
    public static byte[] encryptStringToBytes(String text, SecretKey secretKey) throws Exception {
        if (text == null) {
            return null;
        }
        return encryptData(text.getBytes(StandardCharsets.UTF_8), secretKey);
    }

    /**
     * एन्क्रिप्टेड बाइट ऐरे को डिक्रिप्ट करके मूल स्ट्रिंग प्राप्त करता है।
     */
    public static String decryptBytesToString(byte[] encryptedPayload, SecretKey secretKey) throws Exception {
        if (encryptedPayload == null) {
            return null;
        }
        byte[] decryptedBytes = decryptData(encryptedPayload, secretKey);
        if (decryptedBytes == null) {
            return null;
        }
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * पोर्टेबल बैकअप फ़ाइल के लिए पासवर्ड-आधारित (PBKDF2 + AES-256-GCM) एन्क्रिप्शन।
     * आउटपुट: [16 बाइट Salt] + [12 बाइट IV] + [Ciphertext + Tag]
     */
    public static byte[] encryptBackupData(String jsonContent, String backupPassword) throws Exception {
        if (jsonContent == null || backupPassword == null) {
            return null;
        }

        byte[] salt = new byte[BACKUP_SALT_LENGTH_BYTES];
        new SecureRandom().nextBytes(salt);

        PBEKeySpec spec = new PBEKeySpec(
                backupPassword.toCharArray(),
                salt,
                BACKUP_PBKDF2_ITERATIONS,
                256
        );
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");

        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, parameterSpec);

        byte[] cipherText = cipher.doFinal(jsonContent.getBytes(StandardCharsets.UTF_8));

        ByteBuffer buffer = ByteBuffer.allocate(salt.length + iv.length + cipherText.length);
        buffer.put(salt);
        buffer.put(iv);
        buffer.put(cipherText);
        return buffer.array();
    }

    /**
     * पोर्टेबल बैकअप फ़ाइल को बैकअप पासवर्ड से डिक्रिप्ट करता है।
     */
    public static String decryptBackupData(byte[] backupPayload, String backupPassword) throws Exception {
        if (backupPayload == null || backupPayload.length <= (BACKUP_SALT_LENGTH_BYTES + GCM_IV_LENGTH_BYTES) || backupPassword == null) {
            return null;
        }

        ByteBuffer buffer = ByteBuffer.wrap(backupPayload);
        byte[] salt = new byte[BACKUP_SALT_LENGTH_BYTES];
        buffer.get(salt);

        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        buffer.get(iv);

        byte[] cipherText = new byte[buffer.remaining()];
        buffer.get(cipherText);

        PBEKeySpec spec = new PBEKeySpec(
                backupPassword.toCharArray(),
                salt,
                BACKUP_PBKDF2_ITERATIONS,
                256
        );
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");

        Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, parameterSpec);

        byte[] plainBytes = cipher.doFinal(cipherText);
        return new String(plainBytes, StandardCharsets.UTF_8);
    }
}
