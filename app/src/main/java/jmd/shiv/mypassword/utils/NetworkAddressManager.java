package jmd.shiv.mypassword.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

/**
 * यह क्लास नेटवर्क इंटरफेसों (Wi-Fi, VPN - Tailscale / WireGuard) के IPv4 पते प्राप्त करने, 
 * कनेक्टिविटी जांचने और पेयरिंग QR Code उत्पन्न करने का कार्य करती है।
 */
public class NetworkAddressManager {

    /**
     * स्थानीय Wi-Fi नेटवर्क (Local Wi-Fi Interface) का IPv4 पता प्राप्त करता है।
     *
     * @param context एप्लीकेशन संदर्भ
     * @return Wi-Fi IP पता (उदा. 192.168.1.105) अथवा null
     */
    public static String getLocalWifiIpAddress(Context context) {
        try {
            // पहला तरीका: NetworkInterface से सीधे wlan इंटरफ़ेस पढ़ना
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                String name = intf.getName().toLowerCase();
                if (name.contains("wlan") || name.contains("eth") || name.contains("en")) {
                    List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                    for (InetAddress addr : addrs) {
                        if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                            String host = addr.getHostAddress();
                            if (host != null && !host.isEmpty()) {
                                return host;
                            }
                        }
                    }
                }
            }

            // दूसरा तरीका: WifiManager द्वारा
            if (context != null) {
                WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (wifiManager != null) {
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    int ip = wifiInfo.getIpAddress();
                    if (ip != 0) {
                        return String.format(java.util.Locale.US, "%d.%d.%d.%d",
                                (ip & 0xff),
                                (ip >> 8 & 0xff),
                                (ip >> 16 & 0xff),
                                (ip >> 24 & 0xff));
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * VPN (Tailscale, WireGuard, tun0) का IPv4 पता प्राप्त करता है।
     *
     * @return VPN IP पता (उदा. 100.x.x.x या 10.x.x.x) अथवा null
     */
    public static String getVpnIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                String name = intf.getName().toLowerCase();
                // VPN इंटरफेस सामान्यतः tun, wg, tailscale आदि से शुरू होते हैं
                if (name.startsWith("tun") || name.startsWith("wg") || name.contains("tailscale") || name.contains("utun")) {
                    List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                    for (InetAddress addr : addrs) {
                        if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                            String host = addr.getHostAddress();
                            if (host != null && !host.isEmpty()) {
                                return host;
                            }
                        }
                    }
                }
            }

            // इसके अलावा CGNAT रेंज (100.64.0.0/10) की जांच (Tailscale विशिष्ट)
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        String host = addr.getHostAddress();
                        if (host != null && (host.startsWith("100.") || host.startsWith("10."))) {
                            return host;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * जांचता है कि क्या डिवाइस वर्तमान में सक्रिय नेटवर्क से कनेक्टेड है।
     */
    public static boolean isConnectedToNetwork(Context context) {
        if (context == null) return false;
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        Network activeNetwork = cm.getActiveNetwork();
        if (activeNetwork == null) return false;

        NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
        return capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        );
    }

    /**
     * पेयरिंग टोकन और वेब URL के लिए QR Code Bitmap तैयार करता है।
     *
     * @param content QR कोड में शामिल की जाने वाली सामग्री
     * @param sizePx  इमेज की चौड़ाई/ऊंचाई (पिक्सेल)
     * @return उत्पन्न Bitmap अथवा null
     */
    public static Bitmap generateQrCodeBitmap(String content, int sizePx) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                    content,
                    BarcodeFormat.QR_CODE,
                    sizePx,
                    sizePx,
                    null
            );

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }
}
