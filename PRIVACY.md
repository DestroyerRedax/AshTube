# AshTube Privacy Policy

**Effective Date:** October 15, 2026  
**Application Version:** 1.0

AshTube is committed to user privacy, local data sovereignty, and security. This Privacy Policy explains how AshTube handles data on your device.

---

## 1. No Account and No Cloud Tracking

- **No User Accounts:** AshTube Version 1 does not require, support, or create any user account, login, or cloud profile.
- **No Analytics or Telemetry:** AshTube contains zero analytics SDKs, advertising trackers, device fingerprinting, or telemetry tracking libraries.
- **No Remote Transmission of Personal Data:** Your watch history, favourites, and preferences are never transmitted to any external server or cloud service.

---

## 2. On-Device Storage Only

All application data is stored locally on your device:

| Data Category | Storage Mechanism | Retention Policy |
|---|---|---|
| **Watch History & Progress** | Local SQLite Database (Room) | Stored until cleared by the user or when app data is deleted. |
| **Favourites** | Local SQLite Database (Room) | Stored until unfavourited or cleared by the user. |
| **Download Queue & Tasks** | Local SQLite Database (Room) | Stored until completed, deleted, or cleared. |
| **Local Video Index** | Local SQLite Database (Room) | Stored until removed by user or app data is deleted. |
| **App Preferences & Gates** | Android DataStore Preferences | Stored until reset or when app data is cleared. |
| **Media & Poster Cache** | Scoped App Cache Directory | Evicted automatically by size policy or system cleanup. |

---

## 3. Storage Access Framework (SAF) and File Permissions

- **Scoped Access Only:** AshTube uses Android's Storage Access Framework (`ACTION_OPEN_DOCUMENT` and `ACTION_OPEN_DOCUMENT_TREE`) to allow users to select local video files or directories.
- **No Broad Storage Scanning:** AshTube does not request `MANAGE_EXTERNAL_STORAGE` or broad `READ_EXTERNAL_STORAGE` permissions. It accesses only the exact files or directories explicitly selected by the user.
- **Permission Revocation:** When a local video or folder is removed from the AshTube library, persistable URI permissions are immediately released.

---

## 4. Adults (18+) Content Isolation

- **Disabled by Default:** The Adults (18+) section is locked and hidden by default.
- **Isolated Browsing:** Adult titles, metadata, and search queries remain strictly isolated from general and Kids sections.
- **Instant Revocation:** Users can lock and reset adult access at any time with a single tap in the app header or Settings.

---

## 5. Network Security & TLS Enforcement

- **HTTPS Only:** AshTube enforces secure HTTPS/TLS connections for all network communication (`android:usesCleartextTraffic="false"`).
- **Certificate Validation:** Unencrypted HTTP traffic and invalid certificates are strictly rejected.
- **No Logging of Sensitive URLs:** Stream tokens, authorization headers, and temporary playback URLs are never written to device logs or error traces.

---

## 6. User Control and Data Erasure

You maintain complete control over your data at all times:
- **Instant Clear:** Tap **Clear watch history** or **Clear favourites** inside Settings to instantly erase stored records.
- **System Wipe:** Clearing AshTube data or uninstalling the application via Android System Settings immediately and permanently destroys all local databases, preferences, and cached files.

---

## 7. Contact & Open-Source Integrity

AshTube is an open-source, independent Android video player client. It does not use, imitate, or depend on private Moviebox APK backends, accounts, tokens, signing schemes, or proprietary assets.
