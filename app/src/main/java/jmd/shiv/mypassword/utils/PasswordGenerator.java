package jmd.shiv.mypassword.utils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * यह क्लास SecureRandom का उपयोग करके उच्च-सुरक्षा और जटिलता वाले पासवर्ड उत्पन्न करती है।
 */
public class PasswordGenerator {

    private static final String UPPERCASE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ"; // अस्पष्ट अक्षर (I, O) हटाए गए
    private static final String LOWERCASE_CHARS = "abcdefghijkmnpqrstuvwxyz"; // अस्पष्ट अक्षर (l, o) हटाए गए
    private static final String NUMBER_CHARS = "0123456789"; // सभी अंक (0-9) पिन व डिजिट पासवर्ड हेतु
    private static final String SPECIAL_CHARS = "!@#$%^&*()-_=+[]{}?";

    /**
     * चयनित श्रेणियों और निर्दिष्ट लंबाई के आधार पर मजबूत पासवर्ड जनरेट करता है।
     * यह सुनिश्चित करता है कि चयनित प्रत्येक श्रेणी से कम से कम एक अक्षर अवश्य शामिल हो।
     *
     * @param length           पासवर्ड की लंबाई (8 से 64 के बीच)
     * @param includeUpper     क्या बड़े अक्षर शामिल करने हैं
     * @param includeLower     क्या छोटे अक्षर शामिल करने हैं
     * @param includeNumbers   क्या अंक शामिल करने हैं
     * @param includeSymbols   क्या विशेष चिन्ह शामिल करने हैं
     * @return जनरेट किया गया मजबूत पासवर्ड स्ट्रिंग
     */
    public static String generatePassword(int length, boolean includeUpper, boolean includeLower, boolean includeNumbers, boolean includeSymbols) {
        // न्यूनतम 4 और अधिकतम 15 अक्षर (यूज़र आवश्यकता अनुसार)
        if (length < 4) {
            length = 4;
        } else if (length > 15) {
            length = 15;
        }

        // यदि यूज़र ने किसी भी विकल्प का चयन नहीं किया तो डिफ़ॉल्ट रूप से अंक ऑन करें
        if (!includeUpper && !includeLower && !includeNumbers && !includeSymbols) {
            includeNumbers = true;
        }

        SecureRandom random = new SecureRandom();
        List<Character> requiredChars = new ArrayList<>();
        StringBuilder pool = new StringBuilder();

        if (includeUpper) {
            pool.append(UPPERCASE_CHARS);
            requiredChars.add(UPPERCASE_CHARS.charAt(random.nextInt(UPPERCASE_CHARS.length())));
        }
        if (includeLower) {
            pool.append(LOWERCASE_CHARS);
            requiredChars.add(LOWERCASE_CHARS.charAt(random.nextInt(LOWERCASE_CHARS.length())));
        }
        if (includeNumbers) {
            pool.append(NUMBER_CHARS);
            requiredChars.add(NUMBER_CHARS.charAt(random.nextInt(NUMBER_CHARS.length())));
        }
        if (includeSymbols) {
            pool.append(SPECIAL_CHARS);
            requiredChars.add(SPECIAL_CHARS.charAt(random.nextInt(SPECIAL_CHARS.length())));
        }

        String characterPool = pool.toString();
        List<Character> passwordChars = new ArrayList<>(requiredChars);

        int remainingLength = length - requiredChars.size();
        for (int i = 0; i < remainingLength; i++) {
            char randomChar = characterPool.charAt(random.nextInt(characterPool.length()));
            passwordChars.add(randomChar);
        }

        // सभी अक्षरों को यादृच्छिक रूप से आपस में फेंटना (Shuffle)
        Collections.shuffle(passwordChars, random);

        StringBuilder finalPassword = new StringBuilder();
        for (char c : passwordChars) {
            finalPassword.append(c);
        }
        return finalPassword.toString();
    }
}
