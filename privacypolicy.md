# Privacy Policy for Secure Password Manager (_mypassword)

**Application Name:** Secure Password Manager (_mypassword)  
**Package Name:** `jmd.shiv.mypassword`  
**Developer:** Shiv (JMD Apps)  
**Effective Date:** November 4, 2026  
**Contact Email:** privacy.mypassword@gmail.com  

---

## 🔒 ZERO-KNOWLEDGE & ABSOLUTE PRIVACY GUARANTEE

Secure Password Manager (**_mypassword**) is built with a single, uncompromising principle: **Your digital life belongs solely to you.**

We provide a 100% private, standalone, offline password vault designed without any external dependencies, cloud tracking, or advertisements.

---

## 1. 🚫 NO ADS (Zero Advertisements)
* Our application contains **ABSOLUTELY NO ADS**.
* There are no banner ads, interstitial ads, video ads, or sponsored content.
* We do not integrate any advertising SDKs (such as Google AdMob, Unity Ads, Facebook Audience Network, or AppLovin).
* You will never be tracked for ad profiling or behavioral targeting.

---

## 2. 🚫 NO THIRD PARTY & NO THIRD-PARTY TOOLS
* We do **NOT** use any third-party tracking, analytics, or profiling libraries (e.g., Firebase Analytics, Google Analytics, Flurry, Mixpanel, Crashlytics, or Facebook SDK).
* We do **NOT** partner with, sell, rent, monetize, or transmit any data to data brokers, advertising agencies, or marketing companies.
* We do **NOT** operate any external cloud sync servers, central databases, or remote web services.
* 100% of the application code is native and self-contained.

---

## 3. 💾 LOCAL MOBILE STORAGE (100% On-Device Vault)
* All your data is saved strictly in your **local mobile storage** inside your Android device's private, sandbox-isolated SQLite database (`/data/data/jmd.shiv.mypassword/databases/`).
* No information ever leaves your mobile phone. Even if your internet connection is completely turned off, the application functions with 100% full capability.

---

## 4. 🛡️ MILITARY-GRADE ENCRYPTION SPECIFICATIONS

### A. Master Password: Cryptographic Hashing with Salt
* Your Master Password is **NEVER saved in plain text** anywhere on your device.
* It is protected using standard **PBKDF2WithHmacSHA256** with **100,000 key-derivation iterations**.
* A unique, cryptographically secure **32-byte random Salt** is generated for your master password, preventing rainbow-table and dictionary attacks.
* Continuous incorrect password attempts trigger progressive hardware-level rate-limiting lockouts.

### B. Username Saved in Encrypted Format
* Usernames, Login IDs, and Email Addresses are **NOT stored in plain text**.
* Every username is encrypted using **AES-256-GCM** with hardware-backed keys before being written to the database.

### C. Password Saved in Encrypted Format
* All website and portal passwords are encrypted using **AES-256-GCM** (Galois/Counter Mode with 128-bit authentication integrity tags).
* Each password encryption utilizes a distinct, random **12-byte Initialization Vector (IV)** to ensure identical passwords never produce identical ciphertexts.

### D. Image Screenshots Saved in Encrypted Format
* When you attach a login page screenshot, the image is compressed and encrypted into an **AES-256-GCM Encrypted BLOB** directly within the SQLite database.
* The raw image is never stored as an unencrypted file in your public gallery or SD card, preventing other apps from viewing your login screenshots.

### E. Hardware-Backed Android Keystore
* The master cryptographic encryption keys are generated and securely stored inside the **Android Keystore System** (hardware-backed TEE / StrongBox).
* The private keys never enter application RAM in plain text and cannot be extracted even if the device is inspected.

---

## 5. 📴 NO SENSITIVE MOBILE PERMISSIONS REQUIRED
Unlike conventional apps that intrusively demand your personal device data, **Secure Password Manager requires NO sensitive permissions**:

* ❌ **NO Contacts Permission** (Cannot access your address book or friends list)
* ❌ **NO Location / GPS Permission** (Cannot track your physical location)
* ❌ **NO Camera Permission** (Does not access or control your camera hardware)
* ❌ **NO Microphone Permission** (Cannot record audio)
* ❌ **NO SMS / Call Log Permission** (Cannot read your messages or phone calls)
* ❌ **NO Storage / Gallery Permission** (Uses Android Photo Picker; never reads private photos)
* ❌ **NO Google Account / Identity Permission** (Runs completely anonymously without sign-in)

> **Local Network Disclosure:** The application contains an optional feature to view your passwords on your own computer's Chrome browser over your **same local Wi-Fi router** (`192.168.1.x`) or encrypted private VPN (Tailscale/WireGuard). The internal web server runs strictly on your phone's local network chip and **never** connects to or transmits data over the public internet.

---

## 6. 🛡️ ANTI-REVERSE ENGINEERING & SCREEN PROTECTION
* **`FLAG_SECURE` Protection:** Screenshots and screen recordings are automatically blocked on all app screens to prevent background spyware or screen recording malware from capturing your credentials.
* **Anti-Debugging & Anti-Frida:** Active monitors detect attached debuggers (ptrace), memory injection, and dynamic instrumentation tools (Frida/Xposed), automatically locking the vault upon detection.
* **R8 Bytecode Obfuscation:** The production release is heavily obfuscated with stripped logs and scrambled symbols to resist decompilation.

---

## 7. 🗑️ COMPLETE DATA CONTROL & DELETION
* You own 100% of your data.
* You can export an encrypted portable backup file (`.spmbak`) protected by your custom password at any time.
* You can permanently and irreversibly wipe all database records, master keys, and audit logs by tapping **"Clear Data"** in Settings or via Android App Settings → Clear Storage. Once wiped, the data is cryptographically destroyed forever.

---

## 8. 👶 CHILDREN'S PRIVACY & COPPA COMPLIANCE
Our application does not collect any personal information from anyone, including children under the age of 13. It complies fully with COPPA and Google Play Family Policy requirements.

---

## 9. 📧 CONTACT & DEVELOPER VERIFICATION
If you have any questions or require verification regarding this Privacy Policy, please contact:

* **Developer:** Shiv (JMD Apps)
* **Application:** Secure Password Manager (_mypassword)
* **Package:** `jmd.shiv.mypassword`
* **Email:** [privacy.mypassword@gmail.com](mailto:privacy.mypassword@gmail.com)
