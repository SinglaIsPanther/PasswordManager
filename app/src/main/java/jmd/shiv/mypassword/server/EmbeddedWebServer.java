package jmd.shiv.mypassword.server;

import android.content.Context;
import android.content.res.AssetManager;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import fi.iki.elonen.NanoWSD;
import fi.iki.elonen.NanoWSD.WebSocket;
import fi.iki.elonen.NanoWSD.WebSocketFrame;
import fi.iki.elonen.NanoWSD.WebSocketFrame.CloseCode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * यह क्लास Android डिवाइस में एम्बेडेड HTTP और WebSocket सर्वर का मुख्य इंजन है।
 * यह Chrome वेब इंटरफेस (HTML/CSS/JS) सर्व करता है और लाइव WebSocket कनेक्शन हैंडल करता है।
 */
public class EmbeddedWebServer extends NanoWSD {

    private final Context context;
    private final ApiRequestHandler apiHandler;

    public EmbeddedWebServer(Context context, int port) {
        super(port);
        this.context = context.getApplicationContext();
        this.apiHandler = new ApiRequestHandler(this.context);
    }

    @Override
    public Response serve(IHTTPSession session) {
        // NanoWSD के लिए सभी हेडर कीज को लोअरकेस में भी मैप में उपलब्ध कराना
        try {
            java.util.Map<String, String> headers = session.getHeaders();
            java.util.Map<String, String> lowerMap = new java.util.HashMap<>();
            for (java.util.Map.Entry<String, String> entry : headers.entrySet()) {
                if (entry.getKey() != null) {
                    lowerMap.put(entry.getKey().toLowerCase(java.util.Locale.US), entry.getValue());
                }
            }
            headers.putAll(lowerMap);
        } catch (Exception ignored) {
        }

        return super.serve(session);
    }

    @Override
    protected boolean isWebsocketRequested(IHTTPSession session) {
        String uri = session.getUri();
        if ("/ws".equals(uri) || "/ws/".equals(uri)) {
            return true;
        }

        java.util.Map<String, String> headers = session.getHeaders();
        String upgrade = headers.get("upgrade");
        if (upgrade == null) upgrade = headers.get("Upgrade");
        String connection = headers.get("connection");
        if (connection == null) connection = headers.get("Connection");

        boolean hasUpgrade = upgrade != null && upgrade.toLowerCase(java.util.Locale.US).contains("websocket");
        boolean hasConn = connection != null && connection.toLowerCase(java.util.Locale.US).contains("upgrade");
        return (hasUpgrade && hasConn) || super.isWebsocketRequested(session);
    }

    @Override
    protected WebSocket openWebSocket(IHTTPSession handshake) {
        return new RealtimeClientWebSocket(handshake);
    }

    @Override
    protected Response serveHttp(IHTTPSession session) {
        String uri = session.getUri();

        // 1. REST API अनुरोध
        if (uri.startsWith("/api/")) {
            return apiHandler.handleRequest(session);
        }

        // 2. स्टैटिक वेब एसेट्स (Chrome Web Interface)
        return serveStaticWebAsset(uri);
    }

    /**
     * assets/web/ से HTML, CSS, JavaScript फ़ाइलें पढ़कर ब्राउज़र को भेजता है।
     */
    private Response serveStaticWebAsset(String uri) {
        String assetPath;
        String mimeType;

        if (uri == null || uri.equals("/") || uri.equals("/index.html")) {
            assetPath = "web/index.html";
            mimeType = "text/html; charset=utf-8";
        } else if (uri.equals("/style.css")) {
            assetPath = "web/style.css";
            mimeType = "text/css; charset=utf-8";
        } else if (uri.equals("/app.js")) {
            assetPath = "web/app.js";
            mimeType = "application/javascript; charset=utf-8";
        } else if (uri.equals("/favicon.ico")) {
            Response emptyFav = NanoHTTPD.newFixedLengthResponse(Status.NO_CONTENT, "image/x-icon", "");
            return AuthenticationMiddleware.applySecurityHeaders(emptyFav);
        } else {
            assetPath = "web/index.html"; // SPA Fallback
            mimeType = "text/html; charset=utf-8";
        }

        try {
            AssetManager assetManager = context.getAssets();
            InputStream is = assetManager.open(assetPath);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int read;
            while ((read = is.read(buf)) != -1) {
                baos.write(buf, 0, read);
            }
            byte[] fileBytes = baos.toByteArray();

            Response response = NanoHTTPD.newFixedLengthResponse(
                    Status.OK,
                    mimeType,
                    new ByteArrayInputStream(fileBytes),
                    fileBytes.length
            );
            return AuthenticationMiddleware.applySecurityHeaders(response);
        } catch (IOException e) {
            Response err = NanoHTTPD.newFixedLengthResponse(
                    Status.NOT_FOUND,
                    "text/plain",
                    "Asset Not Found: " + assetPath
            );
            return AuthenticationMiddleware.applySecurityHeaders(err);
        }
    }

    /**
     * कनेक्टेड ब्राउज़र का आंतरिक WebSocket कार्यान्वयन।
     */
    private static class RealtimeClientWebSocket extends WebSocket {

        public RealtimeClientWebSocket(IHTTPSession handshakeRequest) {
            super(handshakeRequest);
        }

        @Override
        protected void onOpen() {
            WebSocketManager.getInstance().addWebSocket(this);
        }

        @Override
        protected void onClose(CloseCode code, String reason, boolean initiatedByRemote) {
            WebSocketManager.getInstance().removeWebSocket(this);
        }

        @Override
        protected void onMessage(WebSocketFrame message) {
            // पिंग-पॉन्ग या लाइव हार्टबीट
            if ("ping".equalsIgnoreCase(message.getTextPayload())) {
                try {
                    send("pong");
                } catch (IOException ignored) {
                }
            }
        }

        @Override
        protected void onPong(WebSocketFrame pong) {
        }

        @Override
        protected void onException(IOException exception) {
            WebSocketManager.getInstance().removeWebSocket(this);
        }
    }
}
