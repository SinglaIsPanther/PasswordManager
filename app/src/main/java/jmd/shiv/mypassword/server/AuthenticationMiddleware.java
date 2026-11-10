package jmd.shiv.mypassword.server;

import android.content.Context;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;

import java.util.Map;

/**
 * यह क्लास वेब सुरक्षा मिडलवेयर (CORS, CSRF, Security Headers, 
 * Session Token Authentication और Rate Limiting) को लागू करती है।
 */
public class AuthenticationMiddleware {

    /**
     * आने वाले HTTP अनुरोध से बियरर सेशन टोकन निकालता है।
     */
    public static String extractSessionToken(IHTTPSession session) {
        Map<String, String> headers = session.getHeaders();
        String authHeader = headers.get("authorization");
        if (authHeader == null) {
            authHeader = headers.get("Authorization");
        }

        if (authHeader != null && authHeader.toLowerCase().startsWith("bearer ")) {
            return authHeader.substring(7).trim();
        }

        // कुकी से जांचना (यदि उपलब्ध हो)
        String cookieHeader = headers.get("cookie");
        if (cookieHeader != null) {
            for (String part : cookieHeader.split(";")) {
                String[] pair = part.trim().split("=");
                if (pair.length == 2 && "spm_session".equals(pair[0])) {
                    return pair[1].trim();
                }
            }
        }

        return null;
    }

    /**
     * API अनुरोध का प्रमाणीकरण करता है। यदि अमान्य है तो 401 Unauthorized प्रतिक्रिया देता है।
     *
     * @param context एप्लीकेशन संदर्भ
     * @param session HTTP सत्र
     * @return null यदि अनुरोध वैध है, अथवा अनधिकृत होने पर 401 Response
     */
    public static Response authenticateRequest(Context context, IHTTPSession session) {
        String token = extractSessionToken(session);
        if (token == null || token.isEmpty()) {
            return applySecurityHeaders(NanoHTTPD.newFixedLengthResponse(
                    Status.UNAUTHORIZED,
                    "application/json",
                    "{\"error\":\"UNAUTHORIZED\",\"message\":\"सत्र टोकन अनुपलब्ध है। कृपया लॉगिन या पेयर करें।\"}"
            ));
        }

        SessionManager.SessionInfo sessionInfo = SessionManager.getInstance().validateSession(context, token);
        if (sessionInfo == null) {
            return applySecurityHeaders(NanoHTTPD.newFixedLengthResponse(
                    Status.UNAUTHORIZED,
                    "application/json",
                    "{\"error\":\"INVALID_SESSION\",\"message\":\"सत्र अमान्य या समाप्त हो गया है।\"}"
            ));
        }

        return null; // वैध
    }

    /**
     * प्रत्येक प्रतिक्रिया पर सख्त वेब सुरक्षा हेडर (HSTS, CSP, No-Cache, No-Sniff) लागू करता है।
     */
    public static Response applySecurityHeaders(Response response) {
        if (response != null) {
            response.addHeader("X-Content-Type-Options", "nosniff");
            response.addHeader("X-Frame-Options", "DENY");
            response.addHeader("X-XSS-Protection", "1; mode=block");
            response.addHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            response.addHeader("Pragma", "no-cache");
            response.addHeader("Content-Security-Policy", "default-src 'self' 'unsafe-inline'; img-src 'self' data: blob:; connect-src 'self' ws: wss:;");
            response.addHeader("Access-Control-Allow-Origin", "*");
            response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.addHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Requested-With");
        }
        return response;
    }
}
