# Google Play Console Publish - सम्पूर्ण आवश्यक डेटा पैकेज (Complete Publishing Guide)

यह दस्तावेज़ Google Play Console पर **Secure Password Manager** (`jmd.shiv.mypassword`) ऐप को सफलतापूर्वक प्रकाशित (Publish) करने के लिए आवश्यक सभी डेटा, टेक्स्ट, नीतियां और फ़ाइल पाथ एक ही स्थान पर उपलब्ध कराता है।

---

## 1. मुख्य स्टोर लिस्टिंग विवरण (Main Store Listing)

### App Name (ऐप का नाम)
*(अधिकतम 30 अक्षर - Google Play Limit: 30 chars)*
```text
Secure Password Manager & Vault
```

### Short Description (संक्षिप्त विवरण)
*(अधिकतम 80 अक्षर - Google Play Limit: 80 chars)*
```text
100% Offline AES-256 encrypted password vault with biometric unlock & Wi-Fi sync.
```

### Full Description (विस्तृत विवरण)
*(Google Play Limit: 4000 chars)*
```markdown
Keep your digital life safe with Secure Password Manager — the ultimate 100% offline, zero-knowledge password vault and credential organizer designed with military-grade security.

🛡️ ZERO-CLOUD ARCHITECTURE (100% PRIVATE & OFFLINE)
Unlike other password managers, Secure Password Manager never uploads your passwords, notes, or bank details to remote servers or third-party cloud platforms. Everything stays strictly on your physical Android device, encrypted with SQLCipher AES-256 bit encryption and protected by the hardware-backed Android KeyStore.

✨ KEY HIGHLIGHTS & FEATURES:

🔐 Biometric & Master PIN Authentication
• Instant, secure access with Fingerprint and Face Unlock.
• Fallback master password with anti-tamper security.
• Auto-locks instantly whenever you exit the app or switch screens.

🎲 Smart Custom Password Generator
• Flexible password lengths from 4 to 15 characters.
• Dedicated "Digits-Only" mode for quick ATM PINs and transaction codes.
• Full alphanumeric & special character support (!@#$%).
• Real-time 128-bit entropy strength meter.

🌐 Local Wi-Fi Chrome PC Sync (Zero Cloud)
• Access and edit your passwords from your desktop Chrome browser.
• Dynamic random port generation on every session for airtight network security.
• Pair instantly with a secure QR code — no internet connection required.

📷 Safe Screenshot & Document Attachments
• Securely store screenshots of recovery codes, 2FA backup keys, and ID cards.
• Attachments are encrypted inside the database and protected from outside gallery scans.
• Built-in FLAG_SECURE shields the app against spyware and unauthorized screen capture.

💳 Comprehensive Credential Organizer
• Organize Web Logins, Bank Cards, Private Notes, and API Keys.
• Real-time live search with instant filtering.
• One-tap quick copy with automatic clipboard auto-clear after 30 seconds.

⚡ Lightweight, Modern & 100% Native
• Built using 100% Native Java for blazing-fast speed and low battery consumption.
• Frosted Glass Slate dark theme engineered for visual comfort.
• Completely Ad-Free with zero analytics, trackers, or background telemetry.
• Fully optimized for Android 16 (API 36).

Take back control of your privacy today. Download Secure Password Manager!
```

---

## 2. स्टोर ग्राफिक्स और मीडिया फ़ाइल्स (Store Assets)

| एसेट प्रकार (Asset Type) | आवश्यकता (Spec) | फ़ाइल पाथ (File Path) |
|---|---|---|
| **App Icon** | 512 x 512 px PNG (RGB, No Alpha) | `D:\Android\AndroidStudioProjects6\PasswordManager\play_store_assets\icon_512x512.png` |
| **Feature Graphic** | 1024 x 500 px PNG/JPEG | `D:\Android\AndroidStudioProjects6\PasswordManager\play_store_assets\feature_graphic_1024x500.png` |
| **Phone Screenshots (6)** | 1080 x 1920 px PNG (16:9) | `D:\Android\AndroidStudioProjects6\PasswordManager\play_store_assets\screenshots\` |
| **Demo Promo Video** | 1080p MP4 (YouTube Upload) | `D:\Android\AndroidStudioProjects6\PasswordManager\play_store_assets\password_manager_demo_video.mp4` |

---

## 3. ऐप रिलीज़ (App Release / Production Track)

- **Release AAB File (गूगल प्ले पर अपलोड करने के लिए):**  
  `D:\Android\AndroidStudioProjects6\PasswordManager\app\build\outputs\bundle\release\app-release.aab`
- **Release Name:** `1.0.0 (1)`
- **Keystore Alias:** `passwordmanagerkey`
- **Target SDK:** 36 (Android 16)

### Release Notes (What's new in this release)
```text
en-US
Initial official release:
• 100% offline AES-256 encrypted password vault
• Biometric fingerprint and master PIN authentication
• Smart password generator with 4-15 length and digits mode
• Local Wi-Fi Chrome web access with dynamic port pairing
• Full compatibility with Android 16 (API 36)

hi-IN
प्रारंभिक आधिकारिक रिलीज़:
• 100% ऑफ़लाइन AES-256 एन्क्रिप्टेड पासवर्ड वॉल्ट
• बायोमेट्रिक फिंगरप्रिंट और मास्टर पिन अनलॉक
• 4 से 15 अक्षरों वाला स्मार्ट पासवर्ड जनरेटर (अंक व सिंबल मोड)
• डायनामिक रैंडम पोर्ट युक्त लोकल वाई-फाई क्रोम वेब सिंक
• नवीनतम Android 16 (API 36) के साथ पूर्ण संगत
```

---

## 4. नीति एवं अनुपालन घोषणाएं (Policy & App Content Checklist)

Google Play Console में **Policy and programs > App content** के सभी फॉर्म्स में निम्नलिखित विकल्प चुनें:

### 1. Privacy Policy (गोपनीयता नीति)
- **URL:** अपने GitHub Repository / Pages पर `PRIVACY_POLICY.md` को होस्ट करें या Google Sites का लिंक दें।
- तैयार नीति फ़ाइल: `D:\Android\AndroidStudioProjects6\PasswordManager\play_store_assets\PRIVACY_POLICY.md`

### 2. App Access (ऐप एक्सेस - गूगल समीक्षक के लिए)
- **विकल्प चुनें:** *All functionality is available without special access*  
  (या यदि मास्टर पिन सेट किया है तो *All or some functionality is restricted* चुनकर डेमो क्रेडेंशियल्स दें, जैसे Master PIN: `1234`).

### 3. Ads (विज्ञापन)
- **विकल्प चुनें:** *No, my app does not contain ads* (ऐप में कोई विज्ञापन नहीं है).

### 4. Content Rating (सामग्री रेटिंग प्रश्नावली)
- **Category:** *Utility, Productivity, Communication, or other*
- **Violence:** No
- **Sexuality:** No
- **Language / Profanity:** No
- **Controlled Substances:** No
- **Promotes illegal activities:** No
- **Does the app natively allow users to interact/exchange messages:** No
- **Does the app share user's current precise physical location:** No
- **Does the app allow users to purchase digital goods:** No
*(परिणाम: रेटिंग **Everyone / PEGI 3** प्राप्त होगी)*

### 5. Target Audience and Content (लक्षित दर्शक)
- **Age group:** 18 and over (18 वर्ष या उससे अधिक)
- **Appeal to children:** No

### 6. Data Safety (डेटा सुरक्षा घोषणा)
- **Does your app collect or share any of the required user data types?**  
  👉 **NO** (चुनें: *No user data is collected or shared*).
- **Is all user data collected by your app encrypted in transit?**  
  👉 **Not applicable** (चूंकि कोई डेटा ट्रांसफर नहीं होता, स्थानीय स्टोरेज पर डेटा AES-256 से एन्क्रिप्टेड है).
- **Do you provide a way for users to request that their data be deleted?**  
  👉 **YES** (उपयोगकर्ता सेटिंग्स में जाकर कभी भी डेटा डिलीट कर सकते हैं या ऐप अनइंस्टॉल करने पर सारा डेटा तुरंत नष्ट हो जाता है).

### 7. Financial Features (वित्तीय सुविधाएं)
- **विकल्प चुनें:** *The app doesn't provide any financial features* (यह केवल स्थानीय पासवर्ड/नोट्स रखने का टूल है).

### 8. Government Apps / Health Apps / News
- **Government App:** No
- **Health / Medical App:** No
- **News App:** No

### 9. Advertising ID (विज्ञापन आईडी)
- **Does your app use advertising ID?**  
  👉 **NO** (ऐप विज्ञापन आईडी का उपयोग नहीं करता).

---

## 5. स्टोर सेटिंग्स (Store Settings & Contact Details)
- **App Category:** `Productivity` (या `Tools`)
- **Tags:** Password Manager, Security, Privacy, Offline Vault
- **Developer Contact:** अपना संपर्क ईमेल (जैसे: `your-email@gmail.com`) दर्ज करें।
