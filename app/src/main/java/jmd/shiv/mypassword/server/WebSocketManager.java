package jmd.shiv.mypassword.server;

import org.json.JSONObject;

import fi.iki.elonen.NanoWSD.WebSocket;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * यह क्लास रियल-टाइम लाइव सिंक्रोनाइज़ेशन (WebSocket Event Broadcasting) 
 * का प्रबंधन करती है। जब भी मोबाइल या Chrome पर कोई बदलाव होता है, यह सभी कनेक्टेड 
 * ब्राउज़रों को तत्काल अपडेट भेजती है।
 */
public class WebSocketManager {

    private static WebSocketManager instance;
    private final Set<WebSocket> activeWebSockets = new CopyOnWriteArraySet<>();

    private WebSocketManager() {
    }

    public static synchronized WebSocketManager getInstance() {
        if (instance == null) {
            instance = new WebSocketManager();
        }
        return instance;
    }

    /**
     * नया सक्रिय WebSocket कनेक्शन पंजीकृत करता है।
     */
    public void addWebSocket(WebSocket webSocket) {
        if (webSocket != null) {
            activeWebSockets.add(webSocket);
        }
    }

    /**
     * बंद हुए WebSocket कनेक्शन को हटाता है।
     */
    public void removeWebSocket(WebSocket webSocket) {
        if (webSocket != null) {
            activeWebSockets.remove(webSocket);
        }
    }

    /**
     * डेटाबेस में रिकॉर्ड परिवर्तन (CREATE, UPDATE, DELETE) की सूचना सभी कनेक्टेड Chrome ब्राउज़रों को प्रसारित (Broadcast) करता है।
     *
     * @param actionType  परिवर्तन का प्रकार (CREATE, UPDATE, DELETE)
     * @param recordId    प्रभावित रिकॉर्ड ID
     * @param websiteName संबंधित वेबसाइट का नाम
     */
    public void broadcastDataChange(String actionType, long recordId, String websiteName) {
        try {
            JSONObject event = new JSONObject();
            event.put("event", "RECORD_CHANGED");
            event.put("action", actionType);
            event.put("record_id", recordId);
            event.put("website_name", websiteName != null ? websiteName : "");
            event.put("timestamp", System.currentTimeMillis());

            String payload = event.toString();
            for (WebSocket ws : activeWebSockets) {
                if (ws.isOpen()) {
                    try {
                        ws.send(payload);
                    } catch (IOException ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * जब मोबाइल पर किसी डिवाइस की अनुमति रद्द की जाती है, तो उसे सूचित करके ब्राउज़र सेशन बंद करना।
     */
    public void broadcastDeviceRevocation(String targetDeviceIdentifier) {
        try {
            JSONObject event = new JSONObject();
            event.put("event", "DEVICE_REVOKED");
            event.put("device_identifier", targetDeviceIdentifier);

            String payload = event.toString();
            for (WebSocket ws : activeWebSockets) {
                if (ws.isOpen()) {
                    try {
                        ws.send(payload);
                    } catch (IOException ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }
}
