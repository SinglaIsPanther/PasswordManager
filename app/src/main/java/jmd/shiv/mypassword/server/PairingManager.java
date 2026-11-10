package jmd.shiv.mypassword.server;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jmd.shiv.mypassword.database.PasswordRepository;
import jmd.shiv.mypassword.models.PairedDevice;

/**
 * यह क्लास कंप्यूटर ब्राउज़र (Chrome) और मोबाइल के बीच सुरक्षित पेयरिंग 
 * (Temporary Pairing Code, Approval Dialog और One-Time Pairing Tokens) को प्रबंधित करती है।
 */
public class PairingManager {

    private static PairingManager instance;

    public interface PairingRequestListener {
        void onPairingRequested(String deviceName, String deviceType, String deviceIdentifier);
    }

    private String activePairingCode = null;
    private long codeExpiryTimestamp = 0;
    private static final long PAIRING_CODE_VALIDITY_MS = 5 * 60 * 1000; // 5 मिनट वैधता

    // विचाराधीन पेयरिंग अनुरोध (Pending Pairing Requests)
    private final Map<String, PendingRequest> pendingRequests = new ConcurrentHashMap<>();
    private PairingRequestListener requestListener;

    public static class PendingRequest {
        public final String deviceName;
        public final String deviceType;
        public final String deviceIdentifier;
        public volatile String status; // PENDING, APPROVED, REJECTED
        public volatile String assignedSessionToken;
        public final long requestTime;

        public PendingRequest(String deviceName, String deviceType, String deviceIdentifier) {
            this.deviceName = deviceName;
            this.deviceType = deviceType;
            this.deviceIdentifier = deviceIdentifier;
            this.status = "PENDING";
            this.requestTime = System.currentTimeMillis();
        }
    }

    private PairingManager() {
        generateNewPairingCode();
    }

    public static synchronized PairingManager getInstance() {
        if (instance == null) {
            instance = new PairingManager();
        }
        return instance;
    }

    public void setPairingRequestListener(PairingRequestListener listener) {
        this.requestListener = listener;
    }

    /**
     * नया 6-अंकीय अस्थायी पेयरिंग कोड उत्पन्न करता है।
     */
    public synchronized String generateNewPairingCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        this.activePairingCode = String.valueOf(code);
        this.codeExpiryTimestamp = System.currentTimeMillis() + PAIRING_CODE_VALIDITY_MS;
        return this.activePairingCode;
    }

    /**
     * वर्तमान सक्रिय पेयरिंग कोड प्राप्त करता है (यदि एक्सपायर न हुआ हो)।
     */
    public synchronized String getActivePairingCode() {
        if (System.currentTimeMillis() > codeExpiryTimestamp) {
            return generateNewPairingCode();
        }
        return activePairingCode;
    }

    /**
     * शेष सेकंड प्राप्त करता है जब तक वर्तमान पेयरिंग कोड मान्य है।
     */
    public synchronized int getRemainingPairingSeconds() {
        long diff = codeExpiryTimestamp - System.currentTimeMillis();
        return diff > 0 ? (int) (diff / 1000) : 0;
    }

    /**
     * ब्राउज़र द्वारा भेजे गए पेयरिंग कोड का सत्यापन और अनुमोदन अनुरोध बनाना।
     *
     * @param deviceName       कंप्यूटर का नाम (उदा. Chrome on Windows)
     * @param deviceType       डिवाइस प्रकार
     * @param deviceIdentifier ब्राउज़र का यूनीक ID
     * @param inputCode        यूज़र द्वारा ब्राउज़र में डाला गया 6-अंकीय कोड
     * @return सत्य यदि कोड सही है और अनुरोध भेजा गया, अन्यथा असत्य
     */
    public boolean submitPairingRequest(String deviceName, String deviceType, String deviceIdentifier, String inputCode) {
        if (inputCode == null || deviceIdentifier == null) {
            return false;
        }

        // कोड एक्सपायरी और मिलान की जांच
        if (System.currentTimeMillis() > codeExpiryTimestamp || !inputCode.trim().equals(activePairingCode)) {
            return false;
        }

        PendingRequest request = new PendingRequest(deviceName, deviceType, deviceIdentifier);
        pendingRequests.put(deviceIdentifier, request);

        // मोबाइल UI को सूचित करें
        if (requestListener != null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                if (requestListener != null) {
                    requestListener.onPairingRequested(deviceName, deviceType, deviceIdentifier);
                }
            });
        }

        return true;
    }

    /**
     * ब्राउज़र से पेयरिंग स्थिति (Polling) चेक करने का फंक्शन।
     */
    public PendingRequest getPendingRequest(String deviceIdentifier) {
        if (deviceIdentifier == null) return null;
        return pendingRequests.get(deviceIdentifier);
    }

    /**
     * मोबाइल एप्लीकेशन द्वारा पेयरिंग अनुरोध को स्वीकार (Approve) करना।
     */
    public boolean approvePairing(Context context, String deviceIdentifier) {
        PendingRequest request = pendingRequests.get(deviceIdentifier);
        if (request == null) {
            return false;
        }

        // नया सुरक्षित 256-बिट सेशन टोकन बनाना
        String sessionToken = SessionManager.getInstance().createSession(deviceIdentifier, request.deviceName);
        request.status = "APPROVED";
        request.assignedSessionToken = sessionToken;

        // SQLite डेटाबेस में सहेजना
        PasswordRepository repository = new PasswordRepository(context);
        PairedDevice pairedDevice = new PairedDevice();
        pairedDevice.setDeviceName(request.deviceName);
        pairedDevice.setDeviceType(request.deviceType);
        pairedDevice.setDeviceIdentifier(deviceIdentifier);
        pairedDevice.setPairingStatus("APPROVED");
        repository.savePairedDevice(pairedDevice);

        repository.insertAuditLog(0, "PAIR", deviceIdentifier);

        // पेयरिंग कोड को तुरंत रीसेट करें (Replay Protection)
        generateNewPairingCode();
        return true;
    }

    /**
     * मोबाइल एप्लीकेशन द्वारा पेयरिंग अनुरोध को अस्वीकार (Reject) करना।
     */
    public void rejectPairing(String deviceIdentifier) {
        PendingRequest request = pendingRequests.get(deviceIdentifier);
        if (request != null) {
            request.status = "REJECTED";
        }
    }
}
