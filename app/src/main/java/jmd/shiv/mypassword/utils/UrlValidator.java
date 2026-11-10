package jmd.shiv.mypassword.utils;

import android.net.Uri;
import android.webkit.URLUtil;

/**
 * यह क्लास वेबसाइट URL के सुरक्षित सत्यापन और HTTPS प्राथमिकता को सुनिश्चित करती है।
 */
public class UrlValidator {

    /**
     * दिए गए इनपुट URL को मान्य करता है और आवश्यक होने पर https:// जोड़ता है।
     *
     * @param inputUrl यूज़र द्वारा दर्ज किया गया URL
     * @return वैध व सुरक्षित URL स्ट्रिंग अथवा अमान्य होने पर null
     */
    public static String sanitizeAndValidateUrl(String inputUrl) {
        if (inputUrl == null) {
            return null;
        }

        String trimmed = inputUrl.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        // यदि प्रोटोकॉल नहीं है तो डिफ़ॉल्ट रूप से https:// जोड़ें
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "https://" + trimmed;
        }

        if (URLUtil.isValidUrl(trimmed)) {
            try {
                Uri parsed = Uri.parse(trimmed);
                String scheme = parsed.getScheme();
                if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                    if (parsed.getHost() != null && !parsed.getHost().isEmpty()) {
                        return trimmed;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
