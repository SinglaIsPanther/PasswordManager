package jmd.shiv.mypassword.server;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import jmd.shiv.mypassword.R;
import jmd.shiv.mypassword.activities.WebAccessActivity;
import jmd.shiv.mypassword.utils.NetworkAddressManager;

/**
 * यह सर्विस एम्बेडेड वेब सर्वर को बैकग्राउंड में Foreground Service के रूप में 
 * चलाती है, जिससे फोन स्क्रीन बंद होने पर भी Chrome वेब एक्सेस सुचारू रूप से चलता रहे।
 */
public class WebServerService extends Service {

    public static final String ACTION_START_SERVER = "jmd.shiv.mypassword.START_SERVER";
    public static final String ACTION_STOP_SERVER = "jmd.shiv.mypassword.STOP_SERVER";
    public static final String EXTRA_PORT = "EXTRA_PORT";

    private static final String CHANNEL_ID = "web_server_channel";
    private static final int NOTIFICATION_ID = 8443;

    private EmbeddedWebServer webServer;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        acquirePartialWakeLock();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_STOP_SERVER.equals(action)) {
                stopWebServer();
                stopForeground(true);
                stopSelf();
                WebServerController.getInstance().updateRunningState(false, this);
                return START_NOT_STICKY;
            } else if (ACTION_START_SERVER.equals(action)) {
                int port = intent.getIntExtra(EXTRA_PORT, 8443);
                startWebServer(port);
            }
        }
        return START_STICKY;
    }

    private void startWebServer(int port) {
        if (webServer != null) {
            webServer.stop();
        }

        try {
            webServer = new EmbeddedWebServer(this, port);
            webServer.start();
            WebServerController.getInstance().updateRunningState(true, this);

            String wifiIp = NetworkAddressManager.getLocalWifiIpAddress(this);
            String url = (wifiIp != null) ? "http://" + wifiIp + ":" + port : "पोर्ट: " + port;

            Notification notification = buildNotification(url);
            startForeground(NOTIFICATION_ID, notification);
        } catch (Exception e) {
            WebServerController.getInstance().updateRunningState(false, this);
            stopSelf();
        }
    }

    private void stopWebServer() {
        if (webServer != null) {
            try {
                webServer.stop();
            } catch (Exception ignored) {
            }
            webServer = null;
        }
    }

    private void acquirePartialWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PasswordManager::WebServerWakeLock");
            wakeLock.acquire(4 * 60 * 60 * 1000L); // अधिकतम 4 घंटे
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Password Manager Web Server",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("कंप्यूटर Chrome वेब एक्सेस एवं लाइव सिंक्रोनाइज़ेशन सूचना");
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification(String urlInfo) {
        Intent openActivityIntent = new Intent(this, WebAccessActivity.class);
        PendingIntent pOpenIntent = PendingIntent.getActivity(
                this,
                0,
                openActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent stopIntent = new Intent(this, WebServerService.class);
        stopIntent.setAction(ACTION_STOP_SERVER);
        PendingIntent pStopIntent = PendingIntent.getService(
                this,
                1,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Password Manager Web Server Running")
                .setContentText("URL: " + urlInfo)
                .setSmallIcon(R.drawable.ic_shield_lock)
                .setContentIntent(pOpenIntent)
                .addAction(R.drawable.ic_logout, "सर्वर बंद करें", pStopIntent)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopWebServer();
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
