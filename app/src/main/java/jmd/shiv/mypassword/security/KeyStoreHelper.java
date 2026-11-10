package jmd.shiv.mypassword.security;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.security.KeyStore;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 * यह क्लास Android Keystore सिस्टम के साथ इंटरैक्ट करके हार्डवेयर-समर्थित 
 * AES-256 एन्क्रिप्शन कुंजियों का सुरक्षित निर्माण और प्रबंधन करती है।
 */
public class KeyStoreHelper {

    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String DEFAULT_MASTER_KEY_ALIAS = "JMD_SHIV_VAULT_KEY_V1";

    /**
     * Android Keystore से मास्टर एन्क्रिप्शन कुंजी प्राप्त करता है या नई कुंजी उत्पन्न करता है।
     *
     * @return SecretKey AES-256 कुंजी
     * @throws Exception कुंजी निर्माण अथवा लोड करने में त्रुटि
     */
    public static SecretKey getOrCreateMasterKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keyStore.load(null);

        if (!keyStore.containsAlias(DEFAULT_MASTER_KEY_ALIAS)) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEY_STORE
            );

            KeyGenParameterSpec parameterSpec = new KeyGenParameterSpec.Builder(
                    DEFAULT_MASTER_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
            )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(false)
                    .build();

            keyGenerator.init(parameterSpec);
            return keyGenerator.generateKey();
        }

        KeyStore.SecretKeyEntry secretKeyEntry = (KeyStore.SecretKeyEntry) keyStore.getEntry(DEFAULT_MASTER_KEY_ALIAS, null);
        return secretKeyEntry.getSecretKey();
    }

    /**
     * यदि जरूरत पड़े तो Keystore से कुंजी हटाने की सुविधा (डेटा रीसेट के लिए)।
     */
    public static void deleteMasterKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            if (keyStore.containsAlias(DEFAULT_MASTER_KEY_ALIAS)) {
                keyStore.deleteEntry(DEFAULT_MASTER_KEY_ALIAS);
            }
        } catch (Exception ignored) {
        }
    }
}
