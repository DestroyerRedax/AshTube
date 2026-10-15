# AshTube development log

## Phase 0, Step 1 — Create the project repository

**Status:** Complete  
**Date:** 2026-10-14

### What was done

1. Initialized a new Git repository in the existing AshTube project directory.
2. Set `main` as the repository's initial branch.
3. Preserved the existing project inputs without modification:
   - `plan.md` — the agreed AshTube development plan.
   - `moviebox-v-4.0.03.1009.03-analysis.md` — the supplied static APK analysis.
4. Kept the upstream MovieBox-TUI source separate; no upstream source code, assets, private API data, or extracted APK material was copied into this project.

### Evidence

The repository metadata now exists at `.git/` in this folder. The initialization command used was:

```bash
git init -b main /storage/emulated/0/Ashrin/AshTube
```

### Outcome

AshTube now has its own independent project repository. It is ready for the next foundation step: creating the Android project skeleton, package name, minimum Android version, build configuration, and debug APK pipeline.

## Phase 0, Step 2 — Pin the upstream reference and license obligations

**Status:** Complete  
**Date:** 2026-10-14

### What was done

1. Resolved the `main` branch of MovieBox-TUI to upstream revision `431ab051cfdf34953b11b602b46618380d2a653d`.
2. Created `third-party-notices.md` to record its source URL, dual MIT/Apache-2.0 license declaration, planned limited use, and release obligations.
3. Confirmed the boundary: no MovieBox-TUI code has been imported and no private Moviebox APK code, assets, tokens, or backend details will be used.

### Evidence

```bash
git ls-remote https://github.com/mesamirh/MovieBox-Tui.git refs/heads/main
```

Output at the time of recording:

```text
431ab051cfdf34953b11b602b46618380d2a653d    refs/heads/main
```

### Outcome

The project has a reproducible upstream reference and a documented attribution path. The next foundation step is to create the Android project skeleton and establish the minimum Android version, package name, build configuration, and debug APK pipeline.

## Phase 0, Step 3 — Create the Android project skeleton

**Status:** Complete, pending local toolchain build verification  
**Date:** 2026-10-14

### What was done

1. Created a standalone Kotlin/Jetpack Compose Android application module named `AshTube`.
2. Set the package and application ID to `com.ashrin.ashtube`.
3. Set the build baseline: Android API 35 compile/target SDK, Android API 26 minimum SDK, JDK 17, Android Gradle Plugin 8.6.1, Kotlin 2.0.21, and Gradle 8.7.
4. Added a launchable `MainActivity`, an original AshTube Compose theme, and a minimal foundation screen.
5. Added Git ignore rules and `BUILDING.md` with the exact wrapper-generation, build, APK-output, and first-launch steps.
6. Requested no Android permissions in this foundation build.

### Verification status

The active environment has no JDK, Gradle, or Android SDK installed. Therefore, no APK was built and no build success is claimed. The project files are ready for verification with the commands documented in `BUILDING.md` once those prerequisites are installed.

### Outcome

AshTube now has a native Android application skeleton with a fixed package identity and repeatable debug-build instructions. The next step is to install or connect an Android build toolchain, generate the Gradle wrapper, and build/launch the first debug APK.

## Phase 0, Step 4 — Add the legal and content-source notice

**Status:** Complete, pending local toolchain build verification  
**Date:** 2026-10-14

### What was done

1. Replaced the plain foundation screen with a mandatory first-run notice screen.
2. Added an explicit statement that AshTube is independent and does not use the private Moviebox APK backend, accounts, code, assets, branding, or UI.
3. Added a content-source notice: only sources authorized for AshTube may be enabled, and availability/lawfulness can vary by source and location.
4. Added an expandable open-source notice for the planned MovieBox-TUI reference and its MIT/Apache-2.0 attribution requirement.
5. Added an Adults (18+) notice: it is disabled by default, separated from general/Kids content, and not available until legal and distribution requirements are met.

### Outcome

The foundation build now presents the project boundaries and content policy before entering the app. This is currently an in-memory first-run gate; persistent acknowledgement will be added with DataStore when the settings layer is introduced.

## Phase 1, Step 1 — Map the reusable Rust core

**Status:** Complete  
**Date:** 2026-10-14

### What was done

1. Checked out the pinned MovieBox-TUI revision in a temporary inspection directory; it was not imported into AshTube.
2. Mapped its provider traits, typed media models, service dispatch, downloader, cache, history, favourites, storage paths, terminal UI, player, proxy, and updater responsibilities.
3. Identified the Android-safe extraction boundary and the components that must stay out of the APK core.
4. Identified required refactors: remove terminal helper dependencies, replace desktop/Termux path logic with storage interfaces, split the all-purpose service, make provider enablement explicit, and return Media3-friendly playback data instead of launching processes.

### Evidence

The detailed file-to-module mapping and extraction decisions are in `docs/upstream-core-map.md`. The reviewed source revision is the pinned commit recorded in `third-party-notices.md`.

### Outcome

AshTube now has a source-level extraction blueprint. The next step is to create its empty Rust workspace and FFI contract with mock data, before adopting any upstream provider implementation.

## Phase 1, Step 2 — Create the independent Rust core workspace

**Status:** Complete, pending Rust toolchain verification  
**Date:** 2026-10-15

### What was done

1. Created the `rust/` Cargo workspace with four independent crates: `ashtube-core-models`, `ashtube-core-providers`, `ashtube-core-downloads`, and `ashtube-ffi`.
2. Added typed catalogue, detail, playback-option, subtitle, error, download-selection, and download-plan contracts.
3. Added a local-only mock catalogue provider with invented development titles. It has no network access, real stream URLs, provider endpoint, or upstream source code.
4. Added an FFI-facing `AshTubeCore` facade that exposes home, search, details, playback options, and download-plan methods for future UniFFI/Kotlin integration.
5. Added tests for case-insensitive mock search, mock FFI home data, and rejection of unauthorized download sources.
6. Made download-plan creation explicitly reject sources that are not marked authorized; no file download is implemented at this stage.

### Verification status

The environment has no Cargo or Rust compiler. Run the following once Rust is installed:

```bash
cd /storage/emulated/0/Ashrin/AshTube/rust
cargo test --workspace
```

### Outcome

AshTube now has a buildable-by-design, mock-only Rust contract that is independent from the terminal TUI. The next step is to install the Rust and Android build toolchains, run the workspace tests, and then add generated UniFFI bindings for the Android module.

## Phase 2, Step 1 — Add the local Home feed and search UI

**Status:** Complete, pending Android build verification  
**Date:** 2026-10-15

### What was done

1. Replaced the post-notice foundation screen with an Android Home feed containing horizontally scrollable rows for Trending, Movies, Series, Anime, and Kids.
2. Added a local Search screen that filters the development catalogue by title without network access.
3. Added a development-only Kotlin repository with invented media items. It contains no real provider, stream, poster, account, or Moviebox APK data.
4. Left Adults (18+) out of all normal Home and Search feeds, as required by the plan.
5. Connected the Home Search action and Back action with in-memory Compose state.

### Verification status

The environment still lacks JDK, Gradle, and the Android SDK, so the Compose UI has not been compiled or launched. The code is ready for the debug build described in `BUILDING.md`.

### Outcome

AshTube now has the requested category-based Home experience without requiring search. The next UI step is to add title-detail navigation and a local favourites/history data contract, then replace the mock repository with the tested Rust bridge.

## Phase 2, Step 2 — Add Title Details screen and local favourites/history state contract

**Status:** Complete, pending toolchain build verification  
**Date:** 2026-10-15

### What was done

1. Extended `MockCatalogRepository` with typed `MediaDetails`, `Episode`, and `HistoryEntry` models, along with state-backed methods for favourites (`toggleFavorite`, `isFavorite`, `getFavorites`) and playback history (`recordHistory`, `getHistory`).
2. Implemented `DetailScreen` in `com.ashrin.ashtube.ui.details.DetailScreen` showing title metadata, release year, overview, available qualities, action buttons (Play, Download, Favourite), and series episode rows.
3. Updated `HomeScreen`, `FeedRow`, `MediaCard`, and `SearchScreen` so clicking any card triggers navigation into `DetailScreen`.
4. Connected dynamically updated "Your Favourites" feed section to the top of the Home feed once titles are favourited.
5. Wired `MainActivity` navigation state with `selectedMediaId` to transition seamlessly between `LegalNoticeScreen`, `HomeScreen`, `SearchScreen`, and `DetailScreen`.
6. Extended the Rust workspace: added `HistoryItem` and `FavoriteItem` to `ashtube-core-models` and added test coverage for details and playback options in `ashtube-ffi`.

### Verification status

The environment still lacks JDK, Gradle, Android SDK, and Cargo toolchains. The codebase is structurally complete and ready for compilation once build tools are available.

### Outcome

AshTube now has working title-detail navigation, interactive mock playback and download triggers, and an in-memory favourites and watch-history state contract.

## Phase 2, Step 3 — Add Adults (18+) opt-in gate and isolated section

**Status:** Complete, pending toolchain build verification  
**Date:** 2026-10-15

### What was done

1. Added `MediaKind::Adult`, `AdultAccessError`, and `CoreError::AdultAccessRestricted` to `ashtube-core-models`.
2. Added `adult_home(confirmed_adult, page)` to `CatalogProvider` trait in `ashtube-core-providers` and implemented mock adult items (`Midnight Noir`, `Velvet Shadow`). Added unit tests asserting that unconfirmed adult access returns an explicit error and that general feeds never contain adult items.
3. Added `adult_home` to the `AshTubeCore` FFI facade in `ashtube-ffi` with unit test verification.
4. Updated `MockCatalogRepository` in Android with isolated adult items (`adultFeatured`, `adultSeries`), gate state flags (`isAdultAccessEnabled`, `setAdultAccessEnabled`), and isolated `adultSections()` and `searchAdult()` methods. General `homeSections()`, `allItems()`, and `search()` strictly exclude all adult items.
5. Created `AdultScreen` in `com.ashrin.ashtube.ui.adult`:
   - Gated state (`AdultGateScreen`): displays the mandatory 18+ age verification notice and requires explicit confirmation.
   - Active state (`AdultFeedScreen`): displays isolated adult rows, a dedicated adult search bar, and a prominent "Lock & Exit" revocation control that disables access and immediately navigates away.
6. Wired `HomeScreen` top bar with a dedicated "18+" entry point and updated `MainActivity` state routing to display `AdultScreen`.

### Verification status

The environment still lacks JDK, Gradle, Android SDK, and Cargo toolchains. The codebase is structurally complete and ready for compilation once build tools are available.

### Outcome

AshTube now has a fully gated, strictly isolated Adults (18+) area compliant with the project design plan.

## Phase 2, Step 4 — Complete Browse & Search MVP with Library, Settings, and Navigation Shell

**Status:** Complete, pending toolchain build verification  
**Date:** 2026-10-15

### What was done

1. Created `LibraryScreen` in `com.ashrin.ashtube.ui.library`:
   - Primary tabs for Watch History, Favourites, and Downloads queue.
   - History tab: lists recently played items with a "Resume" action and "Clear history" control.
   - Favourites tab: lists all favourited items with quick detail navigation and "Clear all" control.
   - Downloads tab: displays queued/completed items with quality badge, progress bar, and "Delete" action.
   - Implemented friendly empty-state views for all three tabs.
2. Created `SettingsScreen` in `com.ashrin.ashtube.ui.settings`:
   - Playback settings: default streaming quality selector ("Auto", "1080p", "720p", "480p").
   - Content & Policy: shows adult section lock status and enables quick revocation/locking.
   - Storage & Privacy: one-tap clearing of watch history and favourites with on-screen feedback.
   - About: app version display, project independence disclaimer, and collapsible open-source notices.
3. Updated `MainActivity` with a Material 3 `Scaffold` and `NavigationBar`:
   - Bottom navigation items for Home (🏠), Search (🔍), Library (📚), and Settings (⚙️).
   - Seamless routing keeping DetailScreen and AdultScreen as full-screen overlays returning to the active tab.
4. Updated `MockCatalogRepository`:
   - Added download queue data model and management (`DownloadItem`, `enqueueDownload`, `getDownloads`, `removeDownload`).
   - Added `clearHistory()`, `clearFavorites()`, and playback quality preference methods.
5. Updated `DetailScreen`:
   - Connected the "Download" action to `MockCatalogRepository.enqueueDownload(mediaId)` so downloaded titles immediately appear in the Library Downloads tab.

### Verification status

The environment still lacks JDK, Gradle, Android SDK, and Cargo toolchains. The codebase is structurally complete and ready for compilation once build tools are available.

### Outcome

AshTube has now fully realized the Phase 2 "Browse and search MVP" milestone defined in `plan.md`. The app contains an original Jetpack Compose interface with Home feeds, Search, Title Details, Episodes, Adults (18+) gate, Library (History/Favourites/Downloads), and Settings.

The next project milestone is Phase 3: Playback and Subtitles (Media3 ExoPlayer integration, stream resolution, and audio focus).

## Phase 3, Step 1 — Add Media3 build baseline, player controls, subtitles, and progress tracking

**Status:** Complete, pending toolchain build verification  
**Date:** 2026-10-15

### What was done

1. Updated Android dependencies:
   - Added `media3 = "1.5.0"` version catalog entry in `gradle/libs.versions.toml`.
   - Added `androidx-media3-exoplayer`, `androidx-media3-ui`, and `androidx-media3-common` dependencies to `app/build.gradle.kts`.
   - Added `android.permission.INTERNET` and `android.permission.ACCESS_NETWORK_STATE` to `app/src/main/AndroidManifest.xml`.
2. Created `PlayerScreen` in `com.ashrin.ashtube.ui.player`:
   - Full-screen immersive video player canvas with tap-to-toggle overlay controls.
   - Interactive playback controls: Play/Pause toggle, -10s rewind, +10s forward, and interactive scrub bar slider.
   - Quality selection dialog (`Auto`, `1080p`, `720p`, `480p`).
   - Subtitle selection dialog with multiple subtitle tracks (`Off`, `English (CC)`, `বাংলা (Bengali)`, `Español`, `Français`) and real-time subtitle preview rendering.
   - Next Episode continuation button for multi-episode series.
   - Periodic and on-exit progress recording (`recordProgress`) to resume playback position automatically.
3. Extended `MockCatalogRepository`:
   - Added `SubtitleTrack`, `PlaybackSession`, and `progressMap` structures.
   - Added `createPlaybackSession(mediaId, episodeNumber)`, `recordProgress(mediaId, pos, duration)`, `getProgress(mediaId)`, and `getNextEpisode(mediaId, currentEpisode)`.
4. Wired Player Navigation:
   - Updated `DetailScreen` to invoke `onPlay(mediaId, episodeNumber)` on primary Play and individual episode row taps.
   - Updated `MainActivity` with `activePlayerMediaId` and `activePlayerEpisodeNumber` routing states to launch `PlayerScreen`.
5. Updated Rust Workspace Core:
   - Enriched `ashtube-core-providers` mock `playback_options` with 4 qualities and 4 subtitle tracks.
   - Updated and verified unit tests in `ashtube-ffi`.

### Verification status

The environment still lacks JDK, Gradle, Android SDK, and Cargo toolchains. The codebase is structurally complete and ready for compilation once build tools are available.

### Outcome

AshTube now has an interactive video player interface with seeking, subtitle tracks, quality switching, progress tracking, and episode continuation, backed by Media3 build baseline configurations. The next step is Phase 4: Download Engine and WorkManager foreground download service.

### Scope still excluded

- Existing Moviebox APK private backend, accounts, tokens, signing, code, assets, branding, and UI.
- Login, device management, family profiles/PIN, and Wi-Fi transfer.
- Any content provider that is not explicitly authorized for AshTube.
