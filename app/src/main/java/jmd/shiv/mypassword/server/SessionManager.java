package jmd.shiv.mypassword.server;

import android.content.Context;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jmd.shiv.mypassword.database.PasswordRepository;
import jmd.shiv.mypassword.models.PairedDevice;
import jmd.shiv.mypassword.security.HashUtils;

/**
 * यह क्लास कनेक्टेड ब्राउज़र सत्रों (Sessions), सेशन टोकन्स और सत्र समाप्ति 
 * (Session Expiry व Revocation) का प्रबंधन करती है।
 */
public class SessionManager {

    private static SessionManager instance;

    public static class SessionInfo {
        public final String sessionToken;
        public final String deviceIdentifier;
        public final String deviceName;
        public volatile long lastActiveTime;

        public SessionInfo(String sessionToken, String deviceIdentifier, String deviceName) {
            this.sessionToken = sessionToken;
            this.deviceIdentifier = deviceIdentifier;
            this.deviceName = deviceName;
            this.lastActiveTime = System.currentTimeMillis();
        }
    }

    private final Map<String, SessionInfo> activeSessions = new ConcurrentHashMap<>();
    private static final long SESSION_TIMEOUT_MS = 2 * 60 * 60 * 1000; // 2 घंटे निष्क्रियता पर एक्सपायर

    private SessionManager() {
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * नया सुरक्षित 256-बिट सेशन टोकन उत्पन्न करता है और सक्रिय सत्र जोड़ता है।
     */
    public String createSession(String deviceIdentifier, String deviceName) {
        byte[] tokenBytes = new byte[32];
        new SecureRandom().nextBytes(tokenBytes);
        String token = HashUtils.bytesToHex(tokenBytes);

        SessionInfo info = new SessionInfo(token, deviceIdentifier, deviceName);
        activeSessions.put(token, info);
        return token;
    }

    /**
     * आने वाले HTTP/WebSocket अनुरोध के सेशन टोकन का सत्यापन करता है।
     *
     * @param context      एप्लीकेशन संदर्भ
     * @param sessionToken ब्राउज़र द्वारा भेजा गया बियरर टोकन
     * @return वैध SessionInfo अथवा अमान्य/एक्सपायर्ड होने पर null
     */
    public SessionInfo validateSession(Context context, String sessionToken) {
        if (sessionToken == null || sessionToken.isEmpty()) {
            return null;
        }

        SessionInfo info = activeSessions.get(sessionToken);
        if (info == null) {
            return null;
        }

        // समय सीमा (Inactivity Timeout) की जांच
        if (System.currentTimeMillis() - info.lastActiveTime > SESSION_TIMEOUT_MS) {
            activeSessions.remove(sessionToken);
            return null;
        }

        // SQLite डेटाबेस से जांचना कि क्या यह डिवाइस मोबाइल पर रिवोक तो नहीं किया गया
        if (context != null) {
            PasswordRepository repository = new PasswordRepository(context);
            PairedDevice device = repository.getPairedDeviceByIdentifier(info.deviceIdentifier);
            if (device == null || !"APPROVED".equalsIgnoreCase(device.getPairingStatus())) {
                activeSessions.remove(sessionToken);
                return null;
            }
        }

        info.lastActiveTime = System.currentTimeMillis();
        return info;
    }

    /**
     * किसी सत्र को तत्काल समाप्त (Invalidate) करना।
     */
    public void invalidateSession(String sessionToken) {
        if (sessionToken != null) {
            activeSessions.remove(sessionToken);
        }
    }

    /**
     * विशिष्ट डिवाइस के सभी सक्रिय सत्र रद्द करना।
     */
    public void revokeDeviceSessions(String deviceIdentifier) {
        if (deviceIdentifier == null) return;
        activeSessions.entrySet().removeIf(entry -> deviceIdentifier.equals(entry.getValue().deviceIdentifier));
    }
}
