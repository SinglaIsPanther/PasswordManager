package jmd.shiv.mypassword.models;

/**
 * यह मॉडल क्लास एप्लीकेशन सेटिंग्स (Auto-lock, Biometric, Theme आदि) को प्रबंधित करती है।
 */
public class AppSettings {

    private int id;
    private int autoLockMinutes;
    private int biometricEnabled;
    private int clipboardClearEnabled;
    private String themeMode;
    private int serverEnabled;
    private int serverPort;

    // पासवर्ड जनरेटर प्राथमिकता सेटिंग्स (Mobile व Web दोनों में साझा)
    private int genLength;
    private int genIncludeUppercase;
    private int genIncludeLowercase;
    private int genIncludeNumbers;
    private int genIncludeSymbols;

    public AppSettings() {
        this.id = 1;
        this.autoLockMinutes = 5; // डिफ़ॉल्ट 5 मिनट
        this.biometricEnabled = 0; // डिफ़ॉल्ट बंद
        this.clipboardClearEnabled = 1; // डिफ़ॉल्ट चालू (30 सेकंड बाद)
        this.themeMode = "LIGHT";
        this.serverEnabled = 0; // डिफ़ॉल्ट बंद
        this.serverPort = 8443; // डिफ़ॉल्ट पोर्ट
        this.genLength = 12; // डिफ़ॉल्ट 12 अक्षर
        this.genIncludeUppercase = 1;
        this.genIncludeLowercase = 1;
        this.genIncludeNumbers = 1;
        this.genIncludeSymbols = 1;
    }

    public int getGenLength() {
        return genLength >= 4 && genLength <= 15 ? genLength : 12;
    }

    public void setGenLength(int genLength) {
        this.genLength = genLength;
    }

    public int getGenIncludeUppercase() {
        return genIncludeUppercase;
    }

    public void setGenIncludeUppercase(int genIncludeUppercase) {
        this.genIncludeUppercase = genIncludeUppercase;
    }

    public int getGenIncludeLowercase() {
        return genIncludeLowercase;
    }

    public void setGenIncludeLowercase(int genIncludeLowercase) {
        this.genIncludeLowercase = genIncludeLowercase;
    }

    public int getGenIncludeNumbers() {
        return genIncludeNumbers;
    }

    public void setGenIncludeNumbers(int genIncludeNumbers) {
        this.genIncludeNumbers = genIncludeNumbers;
    }

    public int getGenIncludeSymbols() {
        return genIncludeSymbols;
    }

    public void setGenIncludeSymbols(int genIncludeSymbols) {
        this.genIncludeSymbols = genIncludeSymbols;
    }

    public int getServerEnabled() {
        return serverEnabled;
    }

    public void setServerEnabled(int serverEnabled) {
        this.serverEnabled = serverEnabled;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAutoLockMinutes() {
        return autoLockMinutes;
    }

    public void setAutoLockMinutes(int autoLockMinutes) {
        this.autoLockMinutes = autoLockMinutes;
    }

    public int getBiometricEnabled() {
        return biometricEnabled;
    }

    public void setBiometricEnabled(int biometricEnabled) {
        this.biometricEnabled = biometricEnabled;
    }

    public int getClipboardClearEnabled() {
        return clipboardClearEnabled;
    }

    public void setClipboardClearEnabled(int clipboardClearEnabled) {
        this.clipboardClearEnabled = clipboardClearEnabled;
    }

    public String getThemeMode() {
        return themeMode;
    }

    public void setThemeMode(String themeMode) {
        this.themeMode = themeMode;
    }
}
