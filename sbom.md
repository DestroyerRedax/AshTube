# AshTube Software Bill of Materials (SBOM)

**Application:** AshTube  
**Package:** `com.ashrin.ashtube`  
**Version:** `0.2.0` (Release Target: `1.0.0`)  
**License:** MIT OR Apache-2.0  
**Audit Date:** October 15, 2026  

---

## 1. Summary of Obligations & License Compliance

All third-party libraries, runtime frameworks, and build plugins used by AshTube have been audited for intellectual property and license compatibility:
- **100% Permissive Open Source:** All runtime dependencies are distributed under either the **Apache-2.0**, **MIT**, or **MPL-2.0** licenses.
- **Zero Proprietary APK Dependencies:** AshTube does not bundle, call, link to, or contain code, assets, tokens, signing certificates, or binaries from the closed-source Moviebox APK.
- **No Incompatible Copyleft:** No GPL, AGPL, or restrictive copyleft code is incorporated into the Android application or Rust shared library.

---

## 2. Android Runtime & UI Dependencies

| Dependency Artifact | Version | License | Purpose |
|---|---|---|---|
| `androidx.core:core-ktx` | `1.15.0` | Apache-2.0 | Kotlin extensions for Android OS APIs |
| `androidx.lifecycle:lifecycle-runtime-ktx` | `2.8.7` | Apache-2.0 | Lifecycle-aware coroutine scopes |
| `androidx.activity:activity-compose` | `1.9.3` | Apache-2.0 | Activity bridge for Jetpack Compose |
| `androidx.compose:compose-bom` | `2024.10.00` | Apache-2.0 | Bill of Materials for Compose components |
| `androidx.compose.ui:ui` | `(BOM)` | Apache-2.0 | Jetpack Compose core UI toolkit |
| `androidx.compose.material3:material3` | `(BOM)` | Apache-2.0 | Material Design 3 design system & components |
| `androidx.media3:media3-exoplayer` | `1.5.0` | Apache-2.0 | High-performance audio/video playback engine |
| `androidx.media3:media3-ui` | `1.5.0` | Apache-2.0 | Video player surfaces and media controls |
| `androidx.media3:media3-common` | `1.5.0` | Apache-2.0 | Common media formats, tracks, and metadata |
| `androidx.work:work-runtime-ktx` | `2.10.0` | Apache-2.0 | Background download engine & foreground services |
| `androidx.room:room-runtime` | `2.6.1` | Apache-2.0 | SQLite database abstraction layer |
| `androidx.room:room-ktx` | `2.6.1` | Apache-2.0 | Kotlin Coroutines & Flow support for Room |
| `androidx.datastore:datastore-preferences` | `1.1.1` | Apache-2.0 | Async on-device preference key-value storage |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | `1.9.0` | Apache-2.0 | Asynchronous reactive concurrency |

---

## 3. Rust Core Dependencies & Crates

| Crate Name | Path / Version | License | Purpose |
|---|---|---|---|
| `ashtube-core-models` | Internal Workspace | MIT OR Apache-2.0 | Typed domain media, episode, error, and page models |
| `ashtube-core-providers` | Internal Workspace | MIT OR Apache-2.0 | Provider contracts and mock catalogue fixtures |
| `ashtube-core-downloads` | Internal Workspace | MIT OR Apache-2.0 | Download plan creation and lifecycle transitions |
| `ashtube-core-storage` | Internal Workspace | MIT OR Apache-2.0 | Neutral storage traits for favorites and watch history |
| `ashtube-ffi` | Internal Workspace | MIT OR Apache-2.0 | FFI facade and UniFFI interface scaffolding |
| `uniffi` | `0.28.x` | MPL-2.0 / Apache-2.0 | Multi-language native binding generator for Kotlin/JNI |

---

## 4. Upstream Reference Attribution

- **Project:** [MovieBox-TUI](https://github.com/mesamirh/MovieBox-Tui)
- **License:** Dual-licensed under MIT OR Apache-2.0
- **Pinned Reference Revision:** `431ab051cfdf34953b11b602b46618380d2a653d`
- **Obligations:** Attribution is maintained in [`third-party-notices.md`](file:///storage/emulated/0/Ashrin/AshTube/third-party-notices.md) and exposed interactively to users under **Settings -> About -> Open Source Notices**. No private code or assets were extracted.
