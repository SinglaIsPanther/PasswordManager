# Privacy Policy for Secure Password Manager

**Last updated:** November 2026

Welcome to **Secure Password Manager** ("we", "our", or "the App"), developed by `jmd.shiv.mypassword`. We are committed to protecting your privacy and ensuring you have complete control over your credentials and personal information.

---

### 1. Zero-Cloud & 100% Offline Architecture
Secure Password Manager operates on a strict **Zero-Knowledge, Offline-First** principle:
- **No Remote Servers:** We do not operate any external cloud servers, databases, or analytics services.
- **Local Storage Only:** All your saved passwords, usernames, bank card numbers, recovery notes, and screenshot attachments are stored exclusively on your device's local internal storage.
- **AES-256 Bit Encryption:** Your database is fully encrypted using SQLCipher AES-256 with keys managed securely through the hardware-backed Android KeyStore.

---

### 2. Information We Do NOT Collect
We believe your private credentials belong to you alone. The App does NOT collect, track, or share:
- No Personal Identifiable Information (PII)
- No Device Identifiers or Advertising IDs (GAID)
- No Location Data or Contacts
- No Browsing History or Search Queries
- No Analytics, Crashlytics, or Telemetry

---

### 3. Permissions Requested & Why
The App requests only the minimum necessary permissions strictly required for its core features:
- **Biometric / Fingerprint (`USE_BIOMETRIC`):** Used solely on your device to authenticate and unlock your encrypted vault. Your biometric data is processed entirely by the Android OS hardware security module and is never accessible to the App.
- **Local Network / Wi-Fi (`INTERNET`, `ACCESS_WIFI_STATE`):** Used exclusively for the optional local Wi-Fi web sync feature (e.g., connecting your phone to your PC's browser over your private home Wi-Fi via a dynamic local IP and port like `192.168.1.x:49152`). No data ever leaves your local network.
- **Notification (`POST_NOTIFICATIONS`):** Used only to display the ongoing status of the local Wi-Fi web server while it is active.

---

### 4. Third-Party Services & Ads
- **No Ads:** The App is 100% ad-free.
- **No Third-Party SDKs:** We do not integrate third-party advertising, analytics, or tracking libraries.

---

### 5. Data Security & Storage Deletion
- **Data Protection:** All credentials stored in the vault are protected with military-grade encryption.
- **Data Deletion:** You can delete individual credentials or clear your entire vault at any time directly within the App settings. Uninstalling the App immediately and permanently erases all vault data from your device.

---

### 6. Children's Privacy
The App is intended for general audiences aged 18 and older. We do not knowingly collect or solicit any data from children under the age of 13.

---

### 7. Contact Us
If you have any questions or feedback regarding this Privacy Policy, please contact us at:
- **Email:** `support@mypasswordmanager.local` (or your developer email)
- **Package Name:** `jmd.shiv.mypassword`
