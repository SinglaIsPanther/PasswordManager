package jmd.shiv.mypassword.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * यह क्लास पासवर्ड हैशिंग, सुरक्षित साल्ट (Salt) निर्माण और PBKDF2 सुरक्षा सत्यापन के लिए उपयोग की जाती है।
 */
public class HashUtils {

    private static final int PBKDF2_ITERATIONS = 100000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";

    /**
     * क्रिप्टोग्राफ़िक रूप से सुरक्षित यादृच्छिक साल्ट (Salt) बाइट्स उत्पन्न करता है।
     *
     * @param lengthByte साल्ट की लंबाई (सामान्यतः 32 बाइट)
     * @return उत्पन्न साल्ट बाइट्स
     */
    public static byte[] generateSecureSalt(int lengthByte) {
        byte[] salt = new byte[lengthByte];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(salt);
        return salt;
    }

    /**
     * मास्टर पासवर्ड और साल्ट का उपयोग करके PBKDF2 सुरक्षित हैश तैयार करता है।
     *
     * @param password सादा मास्टर पासवर्ड
     * @param salt     सुरक्षित साल्ट बाइट्स
     * @return हेक्साडेसिमल हैश स्ट्रिंग
     */
    public static String hashPasswordWithSalt(String password, byte[] salt) {
        if (password == null || salt == null) {
            return null;
        }
        try {
            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    PBKDF2_ITERATIONS,
                    KEY_LENGTH_BITS
            );
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            return null;
        }
    }

    /**
     * मास्टर पासवर्ड के इनपुट को संग्रहीत हैश के साथ समय-सुरक्षित (Constant Time) विधि से तुलना करता है।
     *
     * @param inputPassword     यूज़र द्वारा डाला गया पासवर्ड
     * @param storedSalt        डेटाबेस में सुरक्षित साल्ट
     * @param expectedHexHash   डेटाबेस में सुरक्षित हैश स्ट्रिंग
     * @return सत्य यदि पासवर्ड सही है, अन्यथा असत्य
     */
    public static boolean verifyPassword(String inputPassword, byte[] storedSalt, String expectedHexHash) {
        if (inputPassword == null || storedSalt == null || expectedHexHash == null) {
            return false;
        }
        String computedHash = hashPasswordWithSalt(inputPassword, storedSalt);
        if (computedHash == null) {
            return false;
        }

        byte[] a = computedHash.getBytes();
        byte[] b = expectedHexHash.getBytes();
        return MessageDigest.isEqual(a, b);
    }

    /**
     * बाइट ऐरे को हेक्साडेसिमल स्ट्रिंग में परिवर्तित करता है।
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * हेक्साडेसिमल स्ट्रिंग को बाइट ऐरे में परिवर्तित करता है।
     */
    public static byte[] hexToBytes(String hex) {
        if (hex == null || hex.length() % 2 != 0) {
            return new byte[0];
        }
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
