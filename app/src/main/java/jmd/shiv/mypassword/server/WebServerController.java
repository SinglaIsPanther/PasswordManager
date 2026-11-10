package jmd.shiv.mypassword.server;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import androidx.core.content.ContextCompat;

import jmd.shiv.mypassword.database.PasswordRepository;
import jmd.shiv.mypassword.models.AppSettings;
import jmd.shiv.mypassword.utils.NetworkAddressManager;

/**
 * यह क्लास वेब सर्वर के जीवन चक्र (Start/Stop, Server Status, IP Monitoring) 
 * का केंद्रीय नियंत्रक (Controller) है।
 */
public class WebServerController {

    private static WebServerController instance;

    public interface ServerStatusListener {
        void onServerStatusChanged(boolean isRunning, String localUrl, String vpnUrl);
    }

    private boolean isServerRunning = false;
    private int serverPort = 0;
    private ServerStatusListener statusListener;

    private WebServerController() {
    }

    public static synchronized WebServerController getInstance() {
        if (instance == null) {
            instance = new WebServerController();
        }
        return instance;
    }

    public void setStatusListener(ServerStatusListener listener) {
        this.statusListener = listener;
    }

    public boolean isServerRunning() {
        return isServerRunning;
    }

    /**
     * वर्तमान सक्रिय या कॉन्फ़िगर किया गया पोर्ट प्राप्त करता है।
     */
    public synchronized int getServerPort(Context context) {
        if (serverPort <= 0 && context != null) {
            try {
                PasswordRepository repo = new PasswordRepository(context);
                AppSettings settings = repo.getAppSettings();
                serverPort = settings.getServerPort();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (serverPort <= 0) {
            serverPort = generateNewPort(context);
        }
        return serverPort;
    }

    public int getServerPort() {
        return serverPort > 0 ? serverPort : 8443;
    }

    /**
     * मोबाइल लॉगिन होने पर हर बार एक नया सुरक्षित रैंडम पोर्ट नंबर (8000 - 9999) जनरेट करता है।
     */
    public synchronized int generateNewPort(Context context) {
        java.security.SecureRandom random = new java.security.SecureRandom();
        int newPort = 8000 + random.nextInt(2000); // 8000 से 9999 के बीच रैंडम पोर्ट
        this.serverPort = newPort;

        if (context != null) {
            try {
                PasswordRepository repo = new PasswordRepository(context);
                AppSettings settings = repo.getAppSettings();
                settings.setServerPort(newPort);
                repo.saveAppSettings(settings);

                // यदि सर्वर वर्तमान में चल रहा हो तो उसे नए पोर्ट पर रीस्टार्ट करें
                if (isServerRunning) {
                    stopServer(context);
                    startServer(context, newPort);
                } else {
                    notifyListener(context);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return newPort;
    }

    /**
     * एम्बेडेड वेब सर्वर प्रारंभ करता है और Foreground Service चालू करता है।
     */
    public synchronized void startServer(Context context, int port) {
        if (isServerRunning) {
            return;
        }

        this.serverPort = port > 0 ? port : 8443;

        Intent intent = new Intent(context, WebServerService.class);
        intent.setAction(WebServerService.ACTION_START_SERVER);
        intent.putExtra(WebServerService.EXTRA_PORT, this.serverPort);
        ContextCompat.startForegroundService(context, intent);

        this.isServerRunning = true;
        notifyListener(context);

        // डेटाबेस सेटिंग्स में स्थिति सहेजना
        PasswordRepository repo = new PasswordRepository(context);
        AppSettings settings = repo.getAppSettings();
        settings.setServerEnabled(1);
        settings.setServerPort(this.serverPort);
        repo.saveAppSettings(settings);
    }

    /**
     * एम्बेडेड वेब सर्वर को सुरक्षित रूप से बंद करता है।
     */
    public synchronized void stopServer(Context context) {
        if (!isServerRunning) {
            return;
        }

        Intent intent = new Intent(context, WebServerService.class);
        intent.setAction(WebServerService.ACTION_STOP_SERVER);
        context.startService(intent);

        this.isServerRunning = false;
        notifyListener(context);

        PasswordRepository repo = new PasswordRepository(context);
        AppSettings settings = repo.getAppSettings();
        settings.setServerEnabled(0);
        repo.saveAppSettings(settings);
    }

    public void updateRunningState(boolean running, Context context) {
        this.isServerRunning = running;
        notifyListener(context);
    }

    private void notifyListener(Context context) {
        if (statusListener != null) {
            String wifiIp = NetworkAddressManager.getLocalWifiIpAddress(context);
            String vpnIp = NetworkAddressManager.getVpnIpAddress();

            String localUrl = wifiIp != null ? "http://" + wifiIp + ":" + serverPort : null;
            String vpnUrl = vpnIp != null ? "http://" + vpnIp + ":" + serverPort : null;

            new Handler(Looper.getMainLooper()).post(() -> {
                if (statusListener != null) {
                    statusListener.onServerStatusChanged(isServerRunning, localUrl, vpnUrl);
                }
            });
        }
    }
}
