# Moviebox APK Analysis

## APK identity

- **File:** `moviebox-v-4.0.03.1009.03.apk`
- **SHA-256:** `cd813045fc4b751986e37b86a9d2d149cbd9043a6acd858830de88832efc2300`
- **Package contents:** 6,307 ZIP entries, one `classes.dex`, and native libraries for ARM64 and ARMv7
- **Build metadata:** Android Gradle Plugin 8.6.0

## Main features indicated by the package

The following features appear in `assets/appTab.json` and `assets/therouter/routeMap.json`:

- **Video catalog:** Movies, TV, Trending, Nollywood, Anime, Kids, ShortTV, Music, and Education; with search, categories, filters, rankings, and detail pages.
- **Playback and downloads:** Video/music playback, download manager, series downloads, playback history, local video, and Wi-Fi transfer.
- **Account and family controls:** Login, settings, privacy data, device management, kids profiles, and PIN management.

These are features configured or registered in the package. The APK alone does not prove that every feature is enabled, available to every account, or currently working on the server.

## Other package observations

- `assets/sub_def_la.json` lists 13 subtitle language options: English, Hindi, French, Indonesian, Portuguese, Arabic, Urdu, Bengali, Punjabi, Hausa, Swahili, Chinese, and Russian. This confirms UI options, not subtitle availability for every title.
- Native library names include `libmpv`, FFmpeg-related libraries, an AV1 decoder, downloader/player components, and MMKV. Names identify bundled components; their exact runtime use was not examined.

## Backend findings

The internal router includes client modules for login, catalog/detail pages, search, playback, and downloads. This analysis did **not** recover the API base URL, endpoint schemas, authentication/token flow, request signing, request/response bodies, or user data sent to servers. These details are left unknown rather than inferred from module names.

## Limitations

This is a static APK inventory. The app was not run and no network traffic was captured. REA Android tools and JADX were unavailable in the environment, so the binary `AndroidManifest.xml` and DEX were not decompiled. As a result, the following remain unresolved:

- Application ID, Android permissions, exported components, and deep-link declarations
- Exact backend endpoints and request/response data flows
- Data stored locally and data transmitted to servers
- Encryption/signing implementation and runtime-only behavior

## Coverage summary

| Area | Status |
|---|---|
| Main video, playback, download, and account features | Identified from package configuration and routes |
| Internal routes | 113 counted and grouped; full route listing omitted for brevity |
| Native/media libraries | Names and CPU architectures inventoried |
| Manifest and permissions | Unknown; manifest was not decoded |
| API/backend and data flows | Unknown; no decompilation or traffic capture |
| Runtime behavior | Not tested |

**Summary:** The APK contains evidence of a Moviebox client with video catalog, playback, downloads, and account controls. A full backend and data-flow analysis requires manifest/DEX decompilation and authorized runtime network evidence.
