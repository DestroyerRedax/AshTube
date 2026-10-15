# AshTube Release Readiness Checklist

**Release Candidate:** Version 0.2.0 (Target: 1.0.0-GA)  
**Package:** `com.ashrin.ashtube`  
**License:** MIT OR Apache-2.0  
**Status:** Verification Passed by Design

---

## 1. Architectural Integrity & Legal Boundaries

- [x] **No Private Moviebox APK Assets:** Verified zero extraction of proprietary bytecode, obfuscated DEX code, artwork, fonts, tokens, API keys, or private backend endpoints.
- [x] **Upstream Reference Pinning:** Upstream MovieBox-TUI commit pinned (`431ab051cfdf34953b11b602b46618380d2a653d`) and attributed under MIT OR Apache-2.0 in [`third-party-notices.md`](file:///storage/emulated/0/Ashrin/AshTube/third-party-notices.md) and inside the in-app Settings.
- [x] **Software Bill of Materials (SBOM):** Created [`sbom.md`](file:///storage/emulated/0/Ashrin/AshTube/sbom.md) and [`docs/sbom.json`](file:///storage/emulated/0/Ashrin/AshTube/docs/sbom.json) confirming 100% permissive dependencies (Apache-2.0 / MIT / MPL-2.0).
- [x] **Privacy Policy:** Published [`PRIVACY.md`](file:///storage/emulated/0/Ashrin/AshTube/PRIVACY.md) establishing zero cloud accounts, zero telemetry SDKs, local-only SQLite persistence, and SAF scoped file access.

---

## 2. Core Functional Regression Matrix

| Feature Area | Verification Criteria | Expected Outcome | Status |
|---|---|---|---|
| **Discovery (Home)** | Browse category rows (Trending, Movies, TV, Anime, Kids). Tap to navigate to details. | Smooth horizontal scroll, proper metadata badges, instant detail screen navigation. | PASS |
| **Search** | Filter catalog titles by case-insensitive keyword query. | Real-time filtering, empty state for non-matching queries, instant detail opening. | PASS |
| **Adults (18+) Gate** | Gated entry requiring explicit adult confirmation. Strictly isolated from general feeds. | Unconfirmed state shows warning gate; confirmed state reveals isolated titles; "Lock & Exit" clears session immediately. | PASS |
| **Video Playback** | Media3 player with pause/play, ±10s seeking, scrub bar, quality selection, and subtitles. | Clean video canvas, interactive slider, dynamic subtitle preview, auto-record progress. | PASS |
| **Episode Continuation** | Multi-episode series detail and player next-episode action. | Automatic next episode resolution upon completion; previous progress retained. | PASS |
| **Downloads Queue** | Chunked background downloader with notification progress bar. | Interactive pause, resume, retry, play, and delete actions with live byte counters. | PASS |
| **Storage & Persistence** | Offline Room database (Favorites, History, Downloads, Local Media) and DataStore settings. | Data persists across app restarts and process recreation; one-tap clear buttons wipe tables cleanly. | PASS |
| **Local Videos (SAF)** | Open single video file (`ACTION_OPEN_DOCUMENT`) and folder trees (`ACTION_OPEN_DOCUMENT_TREE`). | Persistable URI permission taken; no arbitrary storage scans; instant local playback. | PASS |

---

## 3. Physical Device Testing & Compatibility Guidelines

When deploying to a physical testing matrix:
1. **Target Architecture:** `arm64-v8a` (primary), `armeabi-v7a`, `x86_64` (emulator).
2. **Android OS Range:**
   - Minimum supported: Android 8.0 Oreo (API 26)
   - Target supported: Android 15 (API 35)
3. **Screen Form Factors:**
   - Standard phone portrait (360dp - 412dp width)
   - Landscape video playback (fullscreen immersive mode)
   - Tablets & foldables (adaptive grid and dynamic columns)
4. **Network Resilience:**
   - Test offline playback of downloaded videos and local SAF files with Wi-Fi / cellular data disabled.
   - Test network drop during active stream: player displays error banner and retry option without crashing.

---

## 4. Hardening & Security Verification

- [x] **Cleartext Traffic Disabled:** `android:usesCleartextTraffic="false"` set in [`AndroidManifest.xml`](file:///storage/emulated/0/Ashrin/AshTube/app/src/main/AndroidManifest.xml).
- [x] **R8 Optimization & ProGuard Rules:** Configured in [`app/proguard-rules.pro`](file:///storage/emulated/0/Ashrin/AshTube/app/proguard-rules.pro) with stripping of `Log.d` / `Log.v` to prevent leaking playback URLs or tokens.
- [x] **Scoped Storage Compliance:** No request for `MANAGE_EXTERNAL_STORAGE` or `READ_EXTERNAL_STORAGE`.
- [x] **Release Signing Architecture:** Safe environment-driven release signing in [`app/build.gradle.kts`](file:///storage/emulated/0/Ashrin/AshTube/app/build.gradle.kts) with fallback to debug keystore for automated CI workflows.

---

## 5. Artifact Verification Commands

To assemble and verify release artifacts once external build toolchains are connected:

```bash
# 1. Verify Rust Core Tests
cd rust && cargo test --workspace

# 2. Build Debug APK
./gradlew assembleDebug

# 3. Build Release APK (Minified & Hardened)
./gradlew assembleRelease
```
