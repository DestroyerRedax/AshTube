# AshTube development plan

## Goal and scope

Build AshTube as an Android video client with an original interface. It will reuse only the suitable, open-source Rust logic from MovieBox-TUI: provider access, catalogue search, title and episode metadata, stream resolution, subtitle discovery, downloads, favourites, and local watch history. The existing terminal application remains usable as a separate frontend.

AshTube will **not** use, imitate, or depend on the analysed Moviebox APK's private backend, authentication system, code, artwork, branding, or user interface. AshTube must use only content sources and streams for which its operator and users are authorized.

### Included in version 1

- Browse home rows: trending, movies, TV, anime, kids, and other provider-supported categories.
- An optional, separately gated **Adults (18+)** section, limited to content that AshTube is authorized to display.
- Search, filters, rankings, details, seasons, and episodes.
- In-app playback with quality and subtitle selection.
- Download queue with pause, resume, retry, deletion, and progress notifications.
- Local history, favourites, app settings, and local-video playback.
- An original Android interface built with Kotlin and Jetpack Compose.

### Explicitly excluded for now

- Login, cloud accounts, cross-device sync, device management, subscriptions, or payments.
- Family profiles and PIN restrictions.
- Wi-Fi transfer.
- Calls to the existing Moviebox APK's endpoints or use of its tokens, signing scheme, or backend.

All user data for version 1 stays on the device.

### Adults (18+) section rules

The Adults section is disabled by default and must remain separate from Home, search suggestions, rankings, recommendations, notifications, and the Kids area. Before showing it, the app must require an adult-age confirmation and a clear consent notice. Its availability must be restricted or removed where the law, source terms, app-store policy, or content license requires it.

The first release will store only the local enabled/disabled preference. It will not use the excluded profile, PIN, or account systems as a substitute for age assurance. If the distribution channel or jurisdiction requires stronger verification, the feature must stay unavailable until that requirement is implemented lawfully.

## Architecture

```text
Jetpack Compose screens
        |
Android ViewModels / repository layer
        |
Kotlin <-> Rust bridge (UniFFI preferred)
        |
AshTube Rust core
  |- provider adapters and catalogue models
  |- search, details, stream and subtitle resolution
  |- download engine and persistent download state
  |- history and favourites domain logic
        |
Android platform services
  |- Media3 / ExoPlayer playback
  |- Room database and DataStore
  |- WorkManager + foreground download service
  |- MediaStore / Storage Access Framework
```

Do not run the terminal UI inside the APK or parse its text output. Move reusable functions out of `moviebox_tui::tui` and call Rust services directly through typed models. This keeps the terminal program and the APK independent while sharing the media logic.

## Technology choices

| Concern | Choice | Reason |
|---|---|---|
| Android UI | Kotlin, Jetpack Compose, Material 3 | Native, accessible, maintainable Android UI |
| Navigation | Navigation Compose | Clear deep navigation for title, season, player, and library screens |
| Playback | AndroidX Media3 / ExoPlayer | Subtitle, adaptive-stream, audio-focus, and background-playback support |
| Shared media logic | Rust extracted from MovieBox-TUI | Retains usable open-source provider and download logic |
| Kotlin/Rust boundary | UniFFI; JNI only for unsupported cases | Typed APIs, generated Kotlin bindings, less manual native code |
| Local database | Room / SQLite | Reliable offline history, favourites, and download records |
| Small preferences | DataStore | Theme, language, player, download, and privacy settings |
| Background work | WorkManager plus a foreground service | Android-compliant long-running downloads |
| Image loading | Coil | Poster caching and Compose integration |

Build native Rust libraries for `arm64-v8a` first. Add `armeabi-v7a` and `x86_64` after the main flow is stable. Test the final APK on a real Android device, not only an emulator.

## Screen plan

| Area | Screens | Main actions |
|---|---|---|
| Discovery | Home, category, ranking, search results | Browse rows, search, filter, open a title |
| Adults (18+) | Age-confirmation gate, separate adult catalogue and detail screens | Opt in, browse authorized adult catalogue, play or download |
| Title | Movie detail, series detail, episode list | View metadata, select episode, play, download, favourite |
| Playback | Player, quality picker, subtitle picker | Play, seek, choose stream/subtitle, update progress |
| Library | Continue watching, favourites, downloads, local videos | Resume, remove, manage downloaded/local files |
| Downloads | Queue, active download detail | Pause, resume, retry, delete, view errors |
| Settings | Playback, downloads, appearance, privacy/about | Select defaults, clear local data, view licenses |

The UI must have its own name, palette, icons, layouts, and assets. It may use familiar media-app patterns, but it must not reproduce the analysed APK's visual identity or resources.

## Core API contract

The Android app should call a small typed API rather than source-specific UI logic. The first bridge surface should include:

```text
get_home_sections() -> List<HomeSection>
get_adult_sections() -> Result<List<HomeSection>, AdultAccessError>
set_adult_access_enabled(confirmed_adult) -> AdultAccessState
search(query, filters, page) -> Page<MediaItem>
get_media_details(id) -> MediaDetails
get_season_episodes(series_id, season) -> List<Episode>
get_playback_options(media_id, episode_id?) -> PlaybackOptions
get_subtitles(stream_id) -> List<SubtitleTrack>
enqueue_download(selection) -> DownloadId
pause_download(id) / resume_download(id) / cancel_download(id)
get_downloads() -> List<DownloadRecord>
record_progress(media_id, position_ms, duration_ms)
get_continue_watching() -> List<HistoryItem>
set_favourite(media_id, enabled) / get_favourites() -> List<MediaItem>
```

`PlaybackOptions` must return explicit stream URLs, headers/cookies only when authorized and needed, stream format, available qualities, expiry time, and error states. Secrets must never be written to logs, screenshots, or analytics.

## Development phases

### Phase 0 — Foundation

1. Create a new Git repository for AshTube; do not modify the upstream project directly until the extraction design is proven.
2. Record the exact upstream commit used and retain the required MIT/Apache-2.0 notices.
3. Create the Android project, minimum Android version, package name, app name, signing configuration, CI build, and code-quality checks.
4. Add an open-source notices screen and a content/legal notice before any provider is enabled.
5. Define the Adults (18+) availability policy for every intended distribution country and store, before enabling any adult provider.

**Done when:** a signed debug APK opens on a physical device and CI builds it from a clean checkout.

### Phase 1 — Extract a reusable Rust core

1. Map upstream modules into `core`, `providers`, `downloads`, `storage`, and `tui` boundaries.
2. Move provider, network, models, stream resolution, and download code out of terminal-specific modules.
3. Remove desktop-only process/player assumptions from the shared core; Android playback must be handled by Media3.
4. Define serializable request/result/error models and add unit tests using saved, non-sensitive provider fixtures.
5. Build the Rust core as a `cdylib` for Android and expose the first functions through UniFFI.

**Done when:** an Android instrumentation test can call the Rust bridge and receive a valid mocked catalogue response.

### Phase 2 — Browse and search MVP

1. Implement app theme, navigation shell, loading/empty/error states, and offline messaging.
2. Build Home, category, search, search-results, and media-detail screens.
3. Add favourites and continue-watching persistence in Room.
4. Cache posters with bounded disk usage and provide text-only fallback for failed artwork.
5. Add the Adults (18+) opt-in gate. Keep adult routes, posters, metadata, search results, and recommendations isolated from all general and Kids flows.

**Done when:** a user can discover a title, open its details, favourite it, close the app, and find it again locally; adult content remains invisible until the adult gate is explicitly enabled.

### Phase 3 — Playback and subtitles

1. Convert a resolved stream into a Media3 `MediaItem` with safe, scoped request headers.
2. Implement player controls, orientation changes, audio focus, network errors, quality picker, and subtitle picker.
3. Record playback progress periodically and on lifecycle transitions.
4. Support episode continuation and the next-episode action.

**Done when:** supported media plays reliably, a subtitle can be selected, and playback resumes after reopening the app.

### Phase 4 — Downloads

1. Define download state: queued, resolving, downloading, paused, completed, failed, and cancelled.
2. Implement a foreground download service, persistent notification, retry policy, storage-space checks, and cancellation.
3. Use MediaStore or the Storage Access Framework for user-visible files; avoid broad file permissions.
4. Add a downloads UI with progress, pause/resume, retry, delete, and clear error explanation.
5. Test interruption by network loss, app restart, device restart, low storage, and expired streams.

**Done when:** a selected, authorized video can download in the background, resume safely, and play offline.

### Phase 5 — Local media and hardening

1. Add a user-selected local folder/file picker using Android's Storage Access Framework.
2. Index only files for which the user has granted access; do not scan unrelated storage.
3. Add local-video detail and playback screens.
4. Audit logging, TLS behavior, error messages, storage cleanup, crash handling, accessibility, and battery/network use.

**Done when:** local media plays after reboot while permission remains granted, and no sensitive URL, cookie, or token appears in logs.

### Phase 6 — Release readiness

1. Test ARM64 physical devices across supported Android versions and networks.
2. Run unit, integration, UI, and manual regression tests.
3. Generate an SBOM; review all Rust, Kotlin, and binary dependency licenses.
4. Publish a privacy policy stating that version 1 has no account and stores history/favourites locally.
5. Produce a release-signed APK/AAB only after content-provider authorization and distribution-policy review.

**Done when:** the release checklist passes, license notices are included, and the app has no dependence on the analysed APK or private APIs.

## Data stored on device

| Data | Store | Retention |
|---|---|---|
| Watch progress and history | Room | Until user clears it |
| Favourites | Room | Until user removes them or clears app data |
| Download queue and state | Room | Until completed/deleted/cleared |
| Downloaded files | User-selected app media location | Until user deletes them |
| Theme and playback settings | DataStore | Until reset or app data is cleared |
| Adults-section opt-in state | DataStore | Until the user disables it or clears app data |
| Poster cache | App cache | Evicted by size policy or Android |

No account identity, device identifier, analytics identifier, or private source credential should be collected in version 1.

## Quality and safety requirements

- Never include a provider's access keys, user cookies, or tokens in the APK.
- Use HTTPS and certificate validation; do not add a trust-all TLS mode.
- Treat all provider text and URLs as untrusted input; validate URLs, file names, subtitles, and metadata.
- Keep download URLs and authorization headers private to the playback/download process and redact them from logs.
- Request only necessary Android permissions. Prefer the Storage Access Framework over broad storage permissions.
- Keep all adult metadata, artwork, notifications, search suggestions, and recommendations out of general and Kids surfaces unless the Adults section is actively opened after the gate.
- Do not enable adult content where its lawful distribution, source authorization, age-assurance method, or store policy has not been verified.
- Provide clear attribution and license notices for MovieBox-TUI and every included dependency.
- Do not claim that every catalogue item is available, downloadable, or legal in every country.

## Initial backlog order

1. Project skeleton and original AshTube visual identity.
2. Rust core extraction with one authorized test provider.
3. Home, search, details, favourites, and the isolated Adults (18+) gate.
4. Media3 playback with one stream quality and one subtitle track.
5. Persistent history and resume playback.
6. Download queue and foreground service.
7. Quality/subtitle pickers, local videos, full test pass, and release hardening.

## Success criteria for the first usable version

- The app installs and functions without Termux.
- The terminal TUI remains functional and is not embedded or screen-scraped.
- A user can browse authorized content, search, view details, select an episode, play it, select a subtitle, and resume later.
- Downloads persist across app restarts and report failures clearly.
- Favourites, history, settings, and downloaded content are device-local.
- Adult content is opt-in, segregated from general/Kids views, and unavailable if its distribution requirements are not satisfied.
- The codebase contains no private Moviebox APK endpoints, copied assets, copied UI resources, or extracted credentials.
