package jmd.shiv.mypassword.models;

import java.io.Serializable;

/**
 * यह मॉडल क्लास कंप्यूटर (Google Chrome) और मोबाइल के बीच पेयर किए गए 
 * डिवाइस की जानकारी और सत्र (Session) स्थिति को प्रस्तुत करती है।
 */
public class PairedDevice implements Serializable {

    private long id;
    private String deviceName;
    private String deviceType;
    private String deviceIdentifier;
    private String pairingStatus; // PENDING, APPROVED, REVOKED
    private String pairedAt;
    private String lastConnectedAt;
    private String revokedAt;

    public PairedDevice() {
        this.pairingStatus = "PENDING";
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getDeviceIdentifier() {
        return deviceIdentifier;
    }

    public void setDeviceIdentifier(String deviceIdentifier) {
        this.deviceIdentifier = deviceIdentifier;
    }

    public String getPairingStatus() {
        return pairingStatus;
    }

    public void setPairingStatus(String pairingStatus) {
        this.pairingStatus = pairingStatus;
    }

    public String getPairedAt() {
        return pairedAt;
    }

    public void setPairedAt(String pairedAt) {
        this.pairedAt = pairedAt;
    }

    public String getLastConnectedAt() {
        return lastConnectedAt;
    }

    public void setLastConnectedAt(String lastConnectedAt) {
        this.lastConnectedAt = lastConnectedAt;
    }

    public String getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(String revokedAt) {
        this.revokedAt = revokedAt;
    }
}
