package jmd.shiv.mypassword.models;

import java.io.Serializable;

/**
 * यह मॉडल क्लास सुरक्षा और डेटा ऑडिट लॉग्स (Action Type, Device Identifier, Timestamp) 
 * को प्रबंधित करती है। ध्यान दें: इसमें कभी भी पासवर्ड या संवेदनशील डेटा स्टोर नहीं किया जाता।
 */
public class AuditLog implements Serializable {

    private long id;
    private long recordId;
    private String actionType; // CREATE, UPDATE, DELETE, VIEW, PAIR, REVOKE, EXPORT
    private String deviceIdentifier;
    private String actionTimestamp;

    public AuditLog() {
    }

    public AuditLog(long recordId, String actionType, String deviceIdentifier, String actionTimestamp) {
        this.recordId = recordId;
        this.actionType = actionType;
        this.deviceIdentifier = deviceIdentifier;
        this.actionTimestamp = actionTimestamp;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getRecordId() {
        return recordId;
    }

    public void setRecordId(long recordId) {
        this.recordId = recordId;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getDeviceIdentifier() {
        return deviceIdentifier;
    }

    public void setDeviceIdentifier(String deviceIdentifier) {
        this.deviceIdentifier = deviceIdentifier;
    }

    public String getActionTimestamp() {
        return actionTimestamp;
    }

    public void setActionTimestamp(String actionTimestamp) {
        this.actionTimestamp = actionTimestamp;
    }
}
