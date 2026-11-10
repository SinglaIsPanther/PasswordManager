package jmd.shiv.mypassword.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

import jmd.shiv.mypassword.R;
import jmd.shiv.mypassword.database.PasswordRepository;
import jmd.shiv.mypassword.managers.ClipboardManagerHelper;
import jmd.shiv.mypassword.models.PairedDevice;
import jmd.shiv.mypassword.server.PairingManager;
import jmd.shiv.mypassword.server.SessionManager;
import jmd.shiv.mypassword.server.WebServerController;
import jmd.shiv.mypassword.server.WebSocketManager;
import jmd.shiv.mypassword.utils.NetworkAddressManager;

/**
 * यह एक्टिविटी कंप्यूटर (Google Chrome) वेब एक्सेस के सर्वर नियंत्रण, 
 * Wi-Fi व VPN IP पतों, QR Code पेयरिंग और कनेक्टेड कंप्यूटर्स के प्रबंधन को नियंत्रित करती है।
 */
public class WebAccessActivity extends BaseSecurityActivity implements WebServerController.ServerStatusListener, PairingManager.PairingRequestListener {

    private ImageView btnBack;
    private TextView textStatusBadge;
    private MaterialButton btnToggleServer;
    private TextView textLocalUrl;
    private MaterialButton btnCopyLocalUrl;
    private MaterialButton btnChangePort;
    private TextView textVpnUrl;

    private MaterialCardView cardPairingInfo;
    private TextView textPairingCode;
    private TextView textCountdown;
    private ImageView imgQrCode;
    private MaterialButton btnRefreshPairingCode;

    private LinearLayout layoutPairedDevices;
    private TextView textNoPairedDevices;

    private PasswordRepository repository;
    private final Handler countdownHandler = new Handler(Looper.getMainLooper());
    private Runnable countdownRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_access);
        applyEdgeToEdgeInsets(findViewById(R.id.layout_web_access_root));

        repository = new PasswordRepository(this);
        initializeViews();
        setupClickListeners();

        WebServerController.getInstance().setStatusListener(this);
        PairingManager.getInstance().setPairingRequestListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUiState();
        startPairingCountdown();
        loadPairedDevicesList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
        }
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btn_web_back);
        textStatusBadge = findViewById(R.id.text_server_status_badge);
        btnToggleServer = findViewById(R.id.btn_toggle_web_server);
        textLocalUrl = findViewById(R.id.text_web_local_url);
        btnChangePort = findViewById(R.id.btn_change_port);
        btnCopyLocalUrl = findViewById(R.id.btn_copy_local_url);
        textVpnUrl = findViewById(R.id.text_web_vpn_url);

        cardPairingInfo = findViewById(R.id.card_pairing_info);
        textPairingCode = findViewById(R.id.text_web_pairing_code);
        textCountdown = findViewById(R.id.text_pairing_countdown);
        imgQrCode = findViewById(R.id.img_web_qr_code);
        btnRefreshPairingCode = findViewById(R.id.btn_refresh_pairing_code);

        layoutPairedDevices = findViewById(R.id.layout_paired_devices_container);
        textNoPairedDevices = findViewById(R.id.text_no_paired_devices);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnToggleServer.setOnClickListener(v -> {
            boolean isRunning = WebServerController.getInstance().isServerRunning();
            if (isRunning) {
                WebServerController.getInstance().stopServer(this);
                Toast.makeText(this, "वेब सर्वर बंद कर दिया गया।", Toast.LENGTH_SHORT).show();
            } else {
                int port = WebServerController.getInstance().getServerPort(this);
                WebServerController.getInstance().startServer(this, port);
                Toast.makeText(this, "वेब सर्वर प्रारंभ किया जा रहा है...", Toast.LENGTH_SHORT).show();
            }
            updateUiState();
        });

        btnChangePort.setOnClickListener(v -> {
            int newPort = WebServerController.getInstance().generateNewPort(this);
            updateUiState();
            Toast.makeText(this, "नया पोर्ट जनरेट किया गया: " + newPort, Toast.LENGTH_SHORT).show();
        });

        btnCopyLocalUrl.setOnClickListener(v -> {
            String url = textLocalUrl.getText().toString();
            if (url.startsWith("http")) {
                ClipboardManagerHelper.copyToClipboard(this, "Web URL", url, false);
                Toast.makeText(this, "URL क्लिपबोर्ड में कॉपी किया गया!", Toast.LENGTH_SHORT).show();
            }
        });

        btnRefreshPairingCode.setOnClickListener(v -> {
            PairingManager.getInstance().generateNewPairingCode();
            updatePairingCodeAndQr();
            Toast.makeText(this, "नया पेयरिंग कोड तैयार किया गया!", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * सर्वर स्थिति और IP पतों के आधार पर संपूर्ण UI को अपडेट करता है।
     */
    private void updateUiState() {
        boolean isRunning = WebServerController.getInstance().isServerRunning();
        int port = WebServerController.getInstance().getServerPort(this);

        String wifiIp = NetworkAddressManager.getLocalWifiIpAddress(this);
        String vpnIp = NetworkAddressManager.getVpnIpAddress();

        if (isRunning) {
            textStatusBadge.setText("सर्वर सक्रिय (Live)");
            textStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.success_green));
            btnToggleServer.setText("सर्वर बंद करें (Stop Server)");
            btnToggleServer.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.error_red));

            String localUrl = (wifiIp != null) ? "http://" + wifiIp + ":" + port : "Wi-Fi कनेक्टेड नहीं है";
            textLocalUrl.setText(localUrl);

            if (vpnIp != null) {
                textVpnUrl.setText("http://" + vpnIp + ":" + port + " (Tailscale/WireGuard सक्रिय)");
                textVpnUrl.setTextColor(ContextCompat.getColor(this, R.color.primary_blue));
            } else {
                textVpnUrl.setText("VPN कनेक्टेड नहीं है (केवल लोकल Wi-Fi पर उपलब्ध)");
                textVpnUrl.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            }

            cardPairingInfo.setVisibility(View.VISIBLE);
            updatePairingCodeAndQr();
        } else {
            textStatusBadge.setText("सर्वर बंद है");
            textStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.error_red));
            btnToggleServer.setText("सर्वर चालू करें (Start Server)");
            btnToggleServer.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary_blue));

            textLocalUrl.setText("सर्वर बंद है (ऊपर Start Server दबाएं)");
            textVpnUrl.setText("सर्वर बंद है");
            cardPairingInfo.setVisibility(View.GONE);
        }
    }

    private void updatePairingCodeAndQr() {
        String code = PairingManager.getInstance().getActivePairingCode();
        textPairingCode.setText(code.substring(0, 3) + " " + code.substring(3));

        String wifiIp = NetworkAddressManager.getLocalWifiIpAddress(this);
        int port = WebServerController.getInstance().getServerPort(this);
        String webUrl = (wifiIp != null) ? "http://" + wifiIp + ":" + port : "http://localhost:" + port;

        // QR Code सामग्री
        String qrPayload = webUrl + "#pairing_code=" + code;
        Bitmap qrBmp = NetworkAddressManager.generateQrCodeBitmap(qrPayload, 360);
        if (qrBmp != null) {
            imgQrCode.setImageBitmap(qrBmp);
        }
    }

    private void startPairingCountdown() {
        if (countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
        }

        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                int sec = PairingManager.getInstance().getRemainingPairingSeconds();
                if (sec <= 0) {
                    PairingManager.getInstance().generateNewPairingCode();
                    updatePairingCodeAndQr();
                    sec = PairingManager.getInstance().getRemainingPairingSeconds();
                }

                int mins = sec / 60;
                int remSec = sec % 60;
                textCountdown.setText(String.format("वैधता: %02d:%02d", mins, remSec));

                countdownHandler.postDelayed(this, 1000);
            }
        };
        countdownHandler.post(countdownRunnable);
    }

    /**
     * डेटाबेस से पेयर किए गए डिवाइसेज की सूची लोड करता है।
     */
    private void loadPairedDevicesList() {
        layoutPairedDevices.removeAllViews();
        List<PairedDevice> list = repository.getAllPairedDevices();

        if (list.isEmpty()) {
            layoutPairedDevices.addView(textNoPairedDevices);
            textNoPairedDevices.setVisibility(View.VISIBLE);
            return;
        }

        textNoPairedDevices.setVisibility(View.GONE);

        for (PairedDevice device : list) {
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_password_card, layoutPairedDevices, false);
            // कस्टम व्यू या डायनामिक रो
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 16, 0, 16);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);

            LinearLayout infoLayout = new LinearLayout(this);
            infoLayout.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            infoLayout.setLayoutParams(lp);

            TextView title = new TextView(this);
            title.setText(device.getDeviceName() != null ? device.getDeviceName() : "Chrome Browser");
            title.setTextSize(15);
            title.setTypeface(null, android.graphics.Typeface.BOLD);
            title.setTextColor(ContextCompat.getColor(this, R.color.text_primary));

            TextView subtitle = new TextView(this);
            String status = "APPROVED".equalsIgnoreCase(device.getPairingStatus()) ? "सक्रिय (Connected)" : "अस्वीकृत (Revoked)";
            subtitle.setText(status + " • " + (device.getLastConnectedAt() != null ? device.getLastConnectedAt() : ""));
            subtitle.setTextSize(12);
            subtitle.setTextColor("APPROVED".equalsIgnoreCase(device.getPairingStatus()) ?
                    ContextCompat.getColor(this, R.color.success_green) :
                    ContextCompat.getColor(this, R.color.error_red));

            infoLayout.addView(title);
            infoLayout.addView(subtitle);

            MaterialButton btnRevoke = new MaterialButton(this, null, com.google.android.material.R.attr.borderlessButtonStyle);
            btnRevoke.setText("डिस्कनेक्ट");
            btnRevoke.setTextColor(ContextCompat.getColor(this, R.color.error_red));
            btnRevoke.setTextSize(12);
            btnRevoke.setOnClickListener(v -> {
                repository.revokePairedDevice(device.getDeviceIdentifier());
                SessionManager.getInstance().revokeDeviceSessions(device.getDeviceIdentifier());
                WebSocketManager.getInstance().broadcastDeviceRevocation(device.getDeviceIdentifier());
                Toast.makeText(this, "डिवाइस का एक्सेस समाप्त कर दिया गया।", Toast.LENGTH_SHORT).show();
                loadPairedDevicesList();
            });

            row.addView(infoLayout);
            row.addView(btnRevoke);

            layoutPairedDevices.addView(row);

            View div = new View(this);
            div.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
            div.setBackgroundColor(ContextCompat.getColor(this, R.color.divider_color));
            layoutPairedDevices.addView(div);
        }
    }

    // =========================================================================
    // Callbacks
    // =========================================================================
    @Override
    public void onServerStatusChanged(boolean isRunning, String localUrl, String vpnUrl) {
        updateUiState();
    }

    @Override
    public void onPairingRequested(String deviceName, String deviceType, String deviceIdentifier) {
        // कंप्यूटर से पेयरिंग अनुमोदन डायलॉग
        new AlertDialog.Builder(this)
                .setTitle("कंप्यूटर पेयरिंग अनुरोध")
                .setMessage("कंप्यूटर: " + deviceName + "\nप्रकार: " + deviceType + "\n\nक्या आप इस कंप्यूटर को अपनी पासवर्ड तिजोरी से जोड़ना चाहते हैं?")
                .setIcon(R.drawable.ic_shield_lock)
                .setCancelable(false)
                .setPositiveButton("स्वीकार करें (Approve)", (dialog, which) -> {
                    boolean ok = PairingManager.getInstance().approvePairing(this, deviceIdentifier);
                    if (ok) {
                        Toast.makeText(this, "कंप्यूटर सफलतापूर्वक पेयर हो गया!", Toast.LENGTH_SHORT).show();
                        loadPairedDevicesList();
                    }
                })
                .setNegativeButton("अस्वीकार करें (Deny)", (dialog, which) -> {
                    PairingManager.getInstance().rejectPairing(deviceIdentifier);
                    Toast.makeText(this, "पेयरिंग अनुरोध अस्वीकार किया गया।", Toast.LENGTH_SHORT).show();
                })
                .show();
    }
}
