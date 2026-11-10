package jmd.shiv.mypassword.server;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;

import jmd.shiv.mypassword.database.PasswordRepository;
import jmd.shiv.mypassword.models.AppSettings;
import jmd.shiv.mypassword.models.PasswordRecord;
import jmd.shiv.mypassword.security.AuthenticationManager;
import jmd.shiv.mypassword.security.EncryptionManager;
import jmd.shiv.mypassword.utils.PasswordGenerator;
import jmd.shiv.mypassword.utils.UrlValidator;

/**
 * यह क्लास Google Chrome वेब इंटरफेस से आने वाले सभी REST API अनुरोधों 
 * (CRUD, ऑथेंटिकेशन, पेयरिंग, पासवर्ड जनरेटर, स्क्रीनशॉट) को प्रोसेस करती है।
 */
public class ApiRequestHandler {

    private final Context context;
    private final PasswordRepository repository;

    public ApiRequestHandler(Context context) {
        this.context = context.getApplicationContext();
        this.repository = new PasswordRepository(this.context);
    }

    /**
     * API अनुरोध को सही हैंडलर फ़ंक्शन पर निर्देशित करता है।
     */
    public Response handleRequest(IHTTPSession session) {
        String uri = session.getUri();
        NanoHTTPD.Method method = session.getMethod();

        // 1. CORS Preflight अनुरोध
        if (NanoHTTPD.Method.OPTIONS.equals(method)) {
            Response response = NanoHTTPD.newFixedLengthResponse(Status.OK, "text/plain", "OK");
            return AuthenticationMiddleware.applySecurityHeaders(response);
        }

        // 2. सार्वजनिक एंडपॉइंट्स (No Auth Required)
        if ("/api/server/status".equals(uri) && NanoHTTPD.Method.GET.equals(method)) {
            return handleServerStatus();
        }

        if ("/api/pairing/request".equals(uri) && NanoHTTPD.Method.POST.equals(method)) {
            return handlePairingRequest(session);
        }

        if ("/api/pairing/status".equals(uri) && NanoHTTPD.Method.GET.equals(method)) {
            return handlePairingStatus(session);
        }

        if ("/api/auth/login".equals(uri) && NanoHTTPD.Method.POST.equals(method)) {
            return handleWebLogin(session);
        }

        // 3. सुरक्षित एंडपॉइंट्स (Authentication Token Required)
        Response authError = AuthenticationMiddleware.authenticateRequest(context, session);
        if (authError != null) {
            return authError;
        }

        String sessionToken = AuthenticationMiddleware.extractSessionToken(session);
        SessionManager.SessionInfo sessionInfo = SessionManager.getInstance().validateSession(context, sessionToken);
        String deviceId = sessionInfo != null ? sessionInfo.deviceIdentifier : "UNKNOWN_BROWSER";

        // यदि फोन पर तिजोरी लॉक है, तो संवेदनशील डेटा के लिए संदेश दें
        if (!AuthenticationManager.getInstance().isUnlocked()) {
            return jsonError(Status.FORBIDDEN, "VAULT_LOCKED", "मोबाइल तिजोरी लॉक है। कृपया पहले मास्टर पासवर्ड से अनलॉक करें।");
        }

        SecretKey activeKey = AuthenticationManager.getInstance().getActiveMasterKey();

        try {
            // Logout
            if ("/api/auth/logout".equals(uri) && NanoHTTPD.Method.POST.equals(method)) {
                SessionManager.getInstance().invalidateSession(sessionToken);
                return jsonSuccess("लॉगआउट सफल।");
            }

            // Password Generator
            if ("/api/password-generator".equals(uri) && NanoHTTPD.Method.POST.equals(method)) {
                return handleGeneratePassword(session);
            }

            // Generator Settings (साझा प्राथमिकताएं: मोबाइल व क्रोम दोनों हेतु)
            if ("/api/generator-settings".equals(uri) && NanoHTTPD.Method.GET.equals(method)) {
                return handleGetGeneratorSettings();
            }
            if ("/api/generator-settings".equals(uri) && NanoHTTPD.Method.POST.equals(method)) {
                return handleSaveGeneratorSettings(session);
            }

            // GET /api/passwords (सूची और सर्च)
            if ("/api/passwords".equals(uri) && NanoHTTPD.Method.GET.equals(method)) {
                return handleGetPasswordsList(session, activeKey);
            }

            // POST /api/passwords (नया पासवर्ड रिकॉर्ड जोड़ना)
            if ("/api/passwords".equals(uri) && NanoHTTPD.Method.POST.equals(method)) {
                return handleAddPassword(session, activeKey, deviceId);
            }

            // /api/passwords/{id} रूट्स
            if (uri.startsWith("/api/passwords/")) {
                String sub = uri.substring("/api/passwords/".length());

                // /api/passwords/{id}/screenshot
                if (sub.endsWith("/screenshot")) {
                    String idStr = sub.substring(0, sub.indexOf("/screenshot"));
                    long recordId = Long.parseLong(idStr);
                    if (NanoHTTPD.Method.GET.equals(method)) {
                        return handleGetScreenshot(recordId, activeKey);
                    } else if (NanoHTTPD.Method.POST.equals(method)) {
                        return handleUploadScreenshot(session, recordId, activeKey, deviceId);
                    }
                }

                // /api/passwords/{id}/favorite
                if (sub.endsWith("/favorite") && NanoHTTPD.Method.POST.equals(method)) {
                    String idStr = sub.substring(0, sub.indexOf("/favorite"));
                    long recordId = Long.parseLong(idStr);
                    return handleToggleFavorite(recordId);
                }

                // /api/passwords/{id} (विस्तृत विवरण, संपादन, हटाना)
                long recordId = Long.parseLong(sub);
                if (NanoHTTPD.Method.GET.equals(method)) {
                    return handleGetPasswordDetails(recordId, activeKey);
                } else if (NanoHTTPD.Method.PUT.equals(method)) {
                    return handleEditPassword(session, recordId, activeKey, deviceId);
                } else if (NanoHTTPD.Method.DELETE.equals(method)) {
                    return handleDeletePassword(recordId, deviceId);
                }
            }

        } catch (Exception e) {
            return jsonError(Status.INTERNAL_ERROR, "SERVER_ERROR", "अनुरोध प्रोसेस करने में त्रुटि: " + e.getMessage());
        }

        return jsonError(Status.NOT_FOUND, "ENDPOINT_NOT_FOUND", "अमान्य API पाथ।");
    }

    private Response handleServerStatus() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("app_name", "Secure Password Manager");
            obj.put("version", "1.0.0");
            obj.put("is_unlocked", AuthenticationManager.getInstance().isUnlocked());
            obj.put("total_records", repository.getTotalPasswordCount());
            return jsonResponse(Status.OK, obj.toString());
        } catch (Exception e) {
            return jsonError(Status.INTERNAL_ERROR, "ERROR", e.getMessage());
        }
    }

    private Response handlePairingRequest(IHTTPSession session) {
        try {
            JSONObject body = parseJsonBody(session);
            String deviceName = body.optString("device_name", "Chrome Browser");
            String deviceType = body.optString("device_type", "Chrome on PC");
            String deviceIdentifier = body.optString("device_identifier");
            String pairingCode = body.optString("pairing_code");

            if (deviceIdentifier == null || pairingCode == null) {
                return jsonError(Status.BAD_REQUEST, "MISSING_PARAMS", "डिवाइस ID और पेयरिंग कोड आवश्यक हैं।");
            }

            boolean ok = PairingManager.getInstance().submitPairingRequest(deviceName, deviceType, deviceIdentifier, pairingCode);
            if (ok) {
                JSONObject res = new JSONObject();
                res.put("status", "PENDING");
                res.put("message", "मोबाइल स्क्रीन पर पेयरिंग अनुमति की प्रतीक्षा की जा रही है...");
                return jsonResponse(Status.OK, res.toString());
            } else {
                return jsonError(Status.BAD_REQUEST, "INVALID_CODE", "अमान्य या समाप्त (Expired) पेयरिंग कोड! मोबाइल स्क्रीन पर दिख रहा नया कोड डालें।");
            }
        } catch (Exception e) {
            return jsonError(Status.INTERNAL_ERROR, "ERROR", e.getMessage());
        }
    }

    private Response handlePairingStatus(IHTTPSession session) {
        try {
            Map<String, String> params = session.getParms();
            String deviceIdentifier = params.get("device_identifier");
            if (deviceIdentifier == null) {
                return jsonError(Status.BAD_REQUEST, "MISSING_PARAM", "device_identifier आवश्यक है।");
            }

            PairingManager.PendingRequest req = PairingManager.getInstance().getPendingRequest(deviceIdentifier);
            JSONObject res = new JSONObject();

            if (req == null) {
                res.put("status", "NOT_FOUND");
            } else {
                res.put("status", req.status);
                if ("APPROVED".equals(req.status)) {
                    res.put("session_token", req.assignedSessionToken);
                }
            }
            return jsonResponse(Status.OK, res.toString());
        } catch (Exception e) {
            return jsonError(Status.INTERNAL_ERROR, "ERROR", e.getMessage());
        }
    }

    private Response handleWebLogin(IHTTPSession session) {
        try {
            JSONObject body = parseJsonBody(session);
            String masterPassword = body.optString("master_password");
            String deviceIdentifier = body.optString("device_identifier");
            String deviceName = body.optString("device_name", "Chrome Browser");

            if (masterPassword.isEmpty()) {
                return jsonError(Status.BAD_REQUEST, "EMPTY_PASSWORD", "मास्टर पासवर्ड आवश्यक है।");
            }

            // मास्टर पासवर्ड का सत्यापन
            boolean valid = AuthenticationManager.getInstance().verifyMasterPassword(context, masterPassword);
            if (!valid) {
                int remaining = AuthenticationManager.getInstance().getRemainingLockoutSeconds();
                if (remaining > 0) {
                    return jsonError(Status.FORBIDDEN, "RATE_LIMIT", "लगातार गलत प्रयास! " + remaining + " सेकंड बाद पुनः प्रयास करें।");
                }
                return jsonError(Status.UNAUTHORIZED, "WRONG_PASSWORD", "गलत मास्टर पासवर्ड!");
            }

            // नया सत्र टोकन जारी करना
            String token = SessionManager.getInstance().createSession(deviceIdentifier, deviceName);
            JSONObject res = new JSONObject();
            res.put("success", true);
            res.put("session_token", token);
            res.put("message", "लॉगिन सफल।");
            return jsonResponse(Status.OK, res.toString());
        } catch (Exception e) {
            return jsonError(Status.INTERNAL_ERROR, "ERROR", e.getMessage());
        }
    }

    private Response handleGetPasswordsList(IHTTPSession session, SecretKey activeKey) {
        try {
            Map<String, String> parms = session.getParms();
            String query = parms.get("q");
            String sort = parms.get("sort");
            if (sort == null) sort = "RECENT";

            List<PasswordRecord> records;
            if (query != null && !query.trim().isEmpty()) {
                records = repository.searchPasswordRecords(query.trim());
            } else {
                records = repository.getAllPasswordRecords(sort);
            }

            JSONArray array = new JSONArray();
            for (PasswordRecord r : records) {
                JSONObject obj = new JSONObject();
                obj.put("id", r.getId());
                obj.put("website_name", r.getWebsiteName());
                obj.put("website_url", r.getWebsiteUrl() != null ? r.getWebsiteUrl() : "");
                obj.put("role", r.getRole() != null ? r.getRole() : "");
                obj.put("is_favorite", r.getIsFavorite());
                obj.put("record_version", r.getRecordVersion());
                obj.put("has_screenshot", r.getScreenshotBlob() != null);
                obj.put("updated_at", r.getUpdatedAt());

                // यूजरनेम डिक्रिप्ट करें (पासवर्ड सूची में कभी सादा नहीं भेजा जाता)
                try {
                    String username = EncryptionManager.decryptBytesToString(r.getUsernameEncrypted(), activeKey);
                    obj.put("username", username != null ? username : "");
                } catch (Exception e) {
                    obj.put("username", "•••");
                }
                obj.put("password_masked", "••••••••••••");

                array.put(obj);
            }

            JSONObject responseObj = new JSONObject();
            responseObj.put("records", array);
            responseObj.put("total", records.size());
            return jsonResponse(Status.OK, responseObj.toString());
        } catch (Exception e) {
            return jsonError(Status.INTERNAL_ERROR, "ERROR", e.getMessage());
        }
    }

    private Response handleGetPasswordDetails(long recordId, SecretKey activeKey) {
        try {
            PasswordRecord r = repository.getPasswordRecordById(recordId);
            if (r == null) {
                return jsonError(Status.NOT_FOUND, "NOT_FOUND", "रिकॉर्ड नहीं मिला।");
            }

            JSONObject obj = new JSONObject();
            obj.put("id", r.getId());
            obj.put("website_name", r.getWebsiteName());
            obj.put("website_url", r.getWebsiteUrl() != null ? r.getWebsiteUrl() : "");
            obj.put("role", r.getRole() != null ? r.getRole() : "");
            obj.put("is_favorite", r.getIsFavorite());
            obj.put("record_version", r.getRecordVersion());
            obj.put("has_screenshot", r.getScreenshotBlob() != null);
            obj.put("created_at", r.getCreatedAt());
            obj.put("updated_at", r.getUpdatedAt());

            String username = EncryptionManager.decryptBytesToString(r.getUsernameEncrypted(), activeKey);
            String password = EncryptionManager.decryptBytesToString(r.getPasswordEncrypted(), activeKey);
            String notes = EncryptionManager.decryptBytesToString(r.getNotesEncrypted(), activeKey);

            obj.put("username", username != null ? username : "");
            obj.put("password", password != null ? password : "");
            obj.put("notes", notes != null ? notes : "");

            return jsonResponse(Status.OK, obj.toString());
        } catch (Exception e) {
            return jsonError(Status.INTERNAL_ERROR, "ERROR", e.getMessage());
        }
    }

    private Response handleAddPassword(IHTTPSession session, SecretKey activeKey, String deviceId) {
        try {
            JSONObject body = parseJsonBody(session);
            String websiteName = body.optString("website_name").trim();
            String websiteUrl = body.optString("website_url").trim();
            String username = body.optString("username").trim();
            String password = body.optString("password").trim();
            String role = body.optString("role").trim();
            String notes = body.optString("notes").trim();
            boolean isFavorite = body.optBoolean("is_favorite", false);

            if (websiteName.isEmpty() || username.isEmpty() || password.isEmpty()) {
                return jsonError(Status.BAD_REQUEST, "MISSING_FIELDS", "वेबसाइट का नाम, यूजरनेम और पासवर्ड आवश्यक हैं।");
            }

            if (!websiteUrl.isEmpty()) {
                String sanitized = UrlValidator.sanitizeAndValidateUrl(websiteUrl);
                if (sanitized != null) websiteUrl = sanitized;
            }

            PasswordRecord record = new PasswordRecord();
            record.setWebsiteName(websiteName);
            record.setWebsiteUrl(websiteUrl);
            record.setRole(role);
            record.setIsFavorite(isFavorite ? 1 : 0);

            record.setUsernameEncrypted(EncryptionManager.encryptStringToBytes(username, activeKey));
            record.setPasswordEncrypted(EncryptionManager.encryptStringToBytes(password, activeKey));
            record.setNotesEncrypted(EncryptionManager.encryptStringToBytes(notes, activeKey));

            // स्क्रीनशॉट बेस64 अपलोड (वैकल्पिक)
            String screenshotBase64 = body.optString("screenshot_base64");
            if (!screenshotBase64.isEmpty()) {
                byte[] rawImg = Base64.decode(screenshotBase64, Base64.DEFAULT);
                byte[] compressedImg = compressImageBytes(rawImg);
                record.setScreenshotBlob(EncryptionManager.encryptData(compressedImg, activeKey));
                record.setScreenshotMimeType(body.optString("screenshot_mime_type", "image/jpeg"));
            }

            long id = repository.savePasswordRecord(record);
            if (id != -1) {
                repository.insertAuditLog(id, "CREATE", deviceId);
                // WebSocket ब्रॉडकास्ट (Chrome व अन्य कनेक्टेड ब्राउज़रों को सूचित करें)
                WebSocketManager.getInstance().broadcastDataChange("CREATE", id, websiteName);

                JSONObject res = new JSONObject();
                res.put("success", true);
                res.put("id", id);
                res.put("message", "पासवर्ड सफलतापूर्वक सेव हुआ।");
                return jsonResponse(Status.OK, res.toString());
            } else {
                return jsonError(Status.INTERNAL_ERROR, "SAVE_FAILED", "डेटाबेस में सेव करने में त्रुटि।");
            }
        } catch (Exception e) {
            return jsonError(Status.INTERNAL_ERROR, "ERROR", e.getMessage());
        }
    }

    private Response handleEditPassword(IHTTPSession session, long recordId, SecretKey activeKey, String deviceId) {
        try {
            PasswordRecord existing = repository.getPasswordRecordById(recordId);
            if (existing == null) {
                return jsonError(Status.NOT_FOUND, "NOT_FOUND", "रिकॉर्ड नहीं मिला।");
            }

            JSONObject body = parseJsonBody(session);

            // कॉन्करेंसी टकराव (Concurrency Conflict) जांचना
            int clientVersion = body.optInt("record_version", existing.getRecordVersion());
            if (clientVersion < existing.getRecordVersion()) {
                return jsonError(Status.CONFLICT, "VERSION_CONFLICT", "इस रिकॉर्ड को मोबाइल या अन्य ब्राउज़र पर हाल ही में अपडेट किया गया है। कृपया ताज़ा डेटा लोड करें।");
            }

            String websiteName = body.optString("website_name", existing.getWebsiteName()).trim();
            String websiteUrl = body.optString("website_url", existing.getWebsiteUrl()).trim();
            String username = body.optString("username").trim();
            String password = body.optString("password").trim();
            String role = body.optString("role", existing.getRole()).trim();
            String notes = body.optString("notes").trim();
            int isFav = body.optInt("is_favorite", existing.getIsFavorite());

            if (!websiteUrl.isEmpty()) {
                String sanitized = UrlValidator.sanitizeAndValidateUrl(websiteUrl);
                if (sanitized != null) websiteUrl = sanitized;
            }

            existing.setWebsiteName(websiteName);
            existing.setWebsiteUrl(websiteUrl);
            existing.setRole(role);
            existing.setIsFavorite(isFav);

            if (!username.isEmpty()) {
                existing.setUsernameEncrypted(EncryptionManager.encryptStringToBytes(username, activeKey));
            }
            if (!password.isEmpty()) {
                existing.setPasswordEncrypted(EncryptionManager.encryptStringToBytes(password, activeKey));
            }
            if (!notes.isEmpty()) {
                existing.setNotesEncrypted(EncryptionManager.encryptStringToBytes(notes, activeKey));
            }

            // यदि नया स्क्रीनशॉट भेजा गया हो
            if (body.has("screenshot_base64")) {
                String b64 = body.optString("screenshot_base64");
                if (!b64.isEmpty()) {
                    byte[] rawImg = Base64.decode(b64, Base64.DEFAULT);
                    byte[] compressed = compressImageBytes(rawImg);
                    existing.setScreenshotBlob(EncryptionManager.encryptData(compressed, activeKey));
                    existing.setScreenshotMimeType(body.optString("screenshot_mime_type", "image/jpeg"));
                } else {
                    existing.setScreenshotBlob(null);
                    existing.setScreenshotMimeType(null);
                }
            }

            int count = repository.updatePasswordRecord(existing);
            if (count > 0) {
                repository.insertAuditLog(recordId, "UPDATE", deviceId);
                WebSocketManager.getInstance().broadcastDataChange("UPDATE", recordId, websiteName);

                JSONObject res = new JSONObject();
                res.put("success", true);
                res.put("message", "रिकॉर्ड सफलतापूर्वक अपडेट हुआ।");
                return jsonResponse(Status.OK, res.toString());
            } else {
                return jsonError(Status.INTERNAL_ERROR, "UPDATE_FAILED", "अपडेट असफल रहा।");
            }
        } catch (Exception e) {
            return jsonError(Status.INTERNAL_ERROR, "ERROR", e.getMessage());
        }
    }

    private Response handleDeletePassword(long recordId, String deviceId) {
        try {
            PasswordRecord record = repository.getPasswordRecordById(recordId);
            String name = (record != null) ? record.getWebsiteName() : "";
            int count = repository.deletePasswordRecord(recordId);
            if (count > 0) {
                repository.insertAuditLog(recordId, "DELETE", deviceId);
                WebSocketManager.getInstance().broadcastDataChange("DELETE", recordId, name);

                JSONObject res = new JSONObject();
                res.put("success", true);
                res.put("message", "रिकॉर्ड सफलतापूर्वक हटा दिया गया।");
                return jsonResponse(Status.OK, res.toString());
            } else {
                return jsonError(Status.NOT_FOUND, "NOT_FOUND", "रिकॉर्ड नहीं मिला।");
            }
        } catch (Exception e) {
            return jsonError(Status.INTERNAL_ERROR, "ERROR", e.getMessage());
        }
    }

    private Response handleToggleFavorite(long recordId) {
        try {
            PasswordRecord r = repository.getPasswordRecordById(recordId);
            if (r == null) {
                return jsonError(Status.NOT_FOUND, "NOT_FOUND", "रिकॉर्ड नहीं मिला।");
            }
            boolean newFav = r.getIsFavorite() == 0;
            repository.toggleFavorite(recordId, newFav);
            WebSocketManager.getInstance().broadcastDataChange("UPDATE", recordId, r.getWebsiteName());

            JSONObject res = new JSONObject();
            res.put("success", true);
            res.put("is_favorite", newFav ? 1 : 0);
            return jsonResponse(Status.OK, res.toString());
        } catch (Exception e) {
            return jsonError(Status.INTERNAL_ERROR, "ERROR", e.getMessage());
        }
    }

    private Response handleGetScreenshot(long recordId, SecretKey activeKey) {
        try {
            PasswordRecord r = repository.getPasswordRecordById(recordId);
            if (r == null || r.getScreenshotBlob() == null) {
                return jsonError(Status.NOT_FOUND, "NO_IMAGE", "स्क्रीनशॉट उपलब्ध नहीं है।");
            }

            byte[] decrypted = EncryptionManager.decryptData(r.getScreenshotBlob(), activeKey);
            if (decrypted == null) {
                return jsonError(Status.INTERNAL_ERROR, "DECRYPT_ERROR", "इमेज डिक्रिप्ट करने में त्रुटि।");
            }

            String mime = r.getScreenshotMimeType() != null ? r.getScreenshotMimeType() : "image/jpeg";
            Response response = NanoHTTPD.newFixedLengthResponse(Status.OK, mime, new ByteArrayInputStream(decrypted), decrypted.length);
            return AuthenticationMiddleware.applySecurityHeaders(response);
        } catch (Exception e) {
            return jsonError(Status.INTERNAL_ERROR, "ERROR", e.getMessage());
        }
    }

    private Response handleUploadScreenshot(IHTTPSession session, long recordId, SecretKey activeKey, String deviceId) {
        try {
            PasswordRecord r = repository.getPasswordRecordById(recordId);
            if (r == null) {
                return jsonError(Status.NOT_FOUND, "NOT_FOUND", "रिकॉर्ड नहीं मिला।");
            }

            JSONObject body = parseJsonBody(session);
            String b64 = body.optString("screenshot_base64");
            if (b64.isEmpty()) {
                return jsonError(Status.BAD_REQUEST, "EMPTY_DATA", "इमेज डेटा अनुपलब्ध है।");
            }

            byte[] raw = Base64.decode(b64, Base64.DEFAULT);
            byte[] compressed = compressImageBytes(raw);

            r.setScreenshotBlob(EncryptionManager.encryptData(compressed, activeKey));
            r.setScreenshotMimeType(body.optString("screenshot_mime_type", "image/jpeg"));
            repository.updatePasswordRecord(r);

            repository.insertAuditLog(recordId, "SCREENSHOT_UPLOAD", deviceId);
            WebSocketManager.getInstance().broadcastDataChange("UPDATE", recordId, r.getWebsiteName());

            JSONObject res = new JSONObject();
            res.put("success", true);
            res.put("message", "स्क्रीनशॉट सफलतापूर्वक अपलोड और एन्क्रिप्ट हुआ।");
            return jsonResponse(Status.OK, res.toString());
        } catch (Exception e) {
            return jsonError(Status.INTERNAL_ERROR, "ERROR", e.getMessage());
        }
    }

    private Response handleGeneratePassword(IHTTPSession session) {
        try {
            JSONObject body = parseJsonBody(session);
            PasswordRepository repo = new PasswordRepository(context);
            AppSettings settings = repo.getAppSettings();

            // यदि क्लाइंट ने नियम नहीं भेजे हैं, तो डेटाबेस में सहेजे गए नियमों का उपयोग करें
            int length = body.has("length") ? body.optInt("length", settings.getGenLength()) : settings.getGenLength();
            boolean upper = body.has("uppercase") ? body.optBoolean("uppercase", settings.getGenIncludeUppercase() == 1) : (settings.getGenIncludeUppercase() == 1);
            boolean lower = body.has("lowercase") ? body.optBoolean("lowercase", settings.getGenIncludeLowercase() == 1) : (settings.getGenIncludeLowercase() == 1);
            boolean nums = body.has("numbers") ? body.optBoolean("numbers", settings.getGenIncludeNumbers() == 1) : (settings.getGenIncludeNumbers() == 1);
            boolean syms = body.has("symbols") ? body.optBoolean("symbols", settings.getGenIncludeSymbols() == 1) : (settings.getGenIncludeSymbols() == 1);

            if (body.optBoolean("saveSettings", false)) {
                settings.setGenLength(length);
                settings.setGenIncludeUppercase(upper ? 1 : 0);
                settings.setGenIncludeLowercase(lower ? 1 : 0);
                settings.setGenIncludeNumbers(nums ? 1 : 0);
                settings.setGenIncludeSymbols(syms ? 1 : 0);
                repo.saveAppSettings(settings);
            }

            String generated = PasswordGenerator.generatePassword(length, upper, lower, nums, syms);
            JSONObject res = new JSONObject();
            res.put("password", generated);
            res.put("length", length);
            res.put("uppercase", upper);
            res.put("lowercase", lower);
            res.put("numbers", nums);
            res.put("symbols", syms);
            return jsonResponse(Status.OK, res.toString());
        } catch (Exception e) {
            return jsonError(Status.INTERNAL_ERROR, "ERROR", e.getMessage());
        }
    }

    private Response handleGetGeneratorSettings() {
        try {
            PasswordRepository repo = new PasswordRepository(context);
            AppSettings settings = repo.getAppSettings();
            JSONObject res = new JSONObject();
            res.put("length", settings.getGenLength());
            res.put("uppercase", settings.getGenIncludeUppercase() == 1);
            res.put("lowercase", settings.getGenIncludeLowercase() == 1);
            res.put("numbers", settings.getGenIncludeNumbers() == 1);
            res.put("symbols", settings.getGenIncludeSymbols() == 1);
            return jsonResponse(Status.OK, res.toString());
        } catch (Exception e) {
            return jsonError(Status.INTERNAL_ERROR, "ERROR", e.getMessage());
        }
    }

    private Response handleSaveGeneratorSettings(IHTTPSession session) {
        try {
            JSONObject body = parseJsonBody(session);
            PasswordRepository repo = new PasswordRepository(context);
            AppSettings settings = repo.getAppSettings();

            if (body.has("length")) settings.setGenLength(body.getInt("length"));
            if (body.has("uppercase")) settings.setGenIncludeUppercase(body.getBoolean("uppercase") ? 1 : 0);
            if (body.has("lowercase")) settings.setGenIncludeLowercase(body.getBoolean("lowercase") ? 1 : 0);
            if (body.has("numbers")) settings.setGenIncludeNumbers(body.getBoolean("numbers") ? 1 : 0);
            if (body.has("symbols")) settings.setGenIncludeSymbols(body.getBoolean("symbols") ? 1 : 0);

            repo.saveAppSettings(settings);
            return jsonSuccess("पासवर्ड सेटिंग्स सफलतापूर्वक अपडेट की गईं।");
        } catch (Exception e) {
            return jsonError(Status.INTERNAL_ERROR, "ERROR", e.getMessage());
        }
    }

    private JSONObject parseJsonBody(IHTTPSession session) throws Exception {
        Map<String, String> files = new HashMap<>();
        session.parseBody(files);

        String jsonString = null;
        if (files.containsKey("postData")) {
            jsonString = files.get("postData");
        } else if (files.containsKey("content")) {
            String filePath = files.get("content");
            if (filePath != null) {
                File f = new File(filePath);
                if (f.exists()) {
                    FileInputStream fis = new FileInputStream(f);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buf = new byte[2048];
                    int r;
                    while ((r = fis.read(buf)) != -1) {
                        baos.write(buf, 0, r);
                    }
                    fis.close();
                    jsonString = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                }
            }
        }

        if (jsonString == null || jsonString.trim().isEmpty()) {
            for (Map.Entry<String, String> entry : files.entrySet()) {
                String val = entry.getValue();
                if (val != null && val.trim().startsWith("{") && val.trim().endsWith("}")) {
                    jsonString = val;
                    break;
                } else if (val != null) {
                    File f = new File(val);
                    if (f.exists() && f.isFile()) {
                        try (FileInputStream fis = new FileInputStream(f);
                             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                            byte[] buf = new byte[2048];
                            int r;
                            while ((r = fis.read(buf)) != -1) {
                                baos.write(buf, 0, r);
                            }
                            String fileContent = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                            if (fileContent.trim().startsWith("{") && fileContent.trim().endsWith("}")) {
                                jsonString = fileContent;
                                break;
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }

        if (jsonString != null && !jsonString.trim().isEmpty()) {
            return new JSONObject(jsonString);
        }
        return new JSONObject();
    }

    private byte[] compressImageBytes(byte[] rawBytes) {
        if (rawBytes == null) return null;
        try {
            Bitmap bmp = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.length);
            if (bmp == null) return rawBytes;

            int maxDim = Math.max(bmp.getWidth(), bmp.getHeight());
            if (maxDim > 1280) {
                float scale = 1280f / maxDim;
                Bitmap scaled = Bitmap.createScaledBitmap(bmp, Math.round(bmp.getWidth() * scale), Math.round(bmp.getHeight() * scale), true);
                bmp.recycle();
                bmp = scaled;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            bmp.recycle();
            return baos.toByteArray();
        } catch (Exception e) {
            return rawBytes;
        }
    }

    private Response jsonResponse(Status status, String jsonContent) {
        Response response = NanoHTTPD.newFixedLengthResponse(status, "application/json", jsonContent);
        return AuthenticationMiddleware.applySecurityHeaders(response);
    }

    private Response jsonSuccess(String message) {
        return jsonResponse(Status.OK, "{\"success\":true,\"message\":\"" + message + "\"}");
    }

    private Response jsonError(Status status, String errorCode, String message) {
        return jsonResponse(status, "{\"error\":\"" + errorCode + "\",\"message\":\"" + message + "\"}");
    }
}
