# MovieBox-TUI core extraction map

## Purpose

This document maps the pinned MovieBox-TUI revision `431ab051cfdf34953b11b602b46618380d2a653d` into a future AshTube Rust core. It is an architecture map only: no upstream source files have been copied into this repository.

AshTube will use only source adapters that have been separately approved for authorized content. The upstream code license does not grant content-distribution rights.

## Evidence reviewed

- `src/lib.rs` exposes library modules separately from the executable entry point.
- `src/main.rs` owns terminal setup, raw-mode lifecycle, the TUI app loop, and command-line behavior.
- `src/providers/mod.rs` defines provider and release-provider traits plus typed catalogue models.
- `src/service.rs` dispatches search, details, home catalogue, subtitles, poster handling, and provider clients.
- `src/download.rs` contains the resumable HTTP range downloader.
- `src/config.rs`, `cache.rs`, `history.rs`, and `favorites.rs` currently select desktop/Termux paths and JSON files directly.

## Target module boundaries

| AshTube target | Upstream candidates | Extraction decision |
|---|---|---|
| `ashtube-core-models` | `providers/models.rs`, selected plain models from `models.rs` | Reuse/adapt typed media, catalogue, release, subtitle, and provider error models. Remove TUI-only formatting methods. |
| `ashtube-core-providers` | `providers/mod.rs`, provider-specific adapters, `net.rs`, selected `service.rs` dispatch | Reuse only approved provider adapters. Expose typed async search, details, home, release, and subtitle methods. |
| `ashtube-core-downloads` | `download.rs` | Adapt the resumable range logic behind a platform-neutral download interface. Add Android-managed destination and scoped request-header support before use. |
| `ashtube-core-storage` | selected identity logic in `favorites.rs`, `history.rs`, `cache.rs` | Retain value/identity models only. Replace direct JSON files and `dirs` path discovery with a storage trait implemented by Android/Room or app-private storage. |
| `ashtube-ffi` | new code | Define the UniFFI bridge models and APIs used by Kotlin. No terminal strings or platform process calls cross this boundary. |
| Android/Kotlin | poster decoding, player, screen state, database integration, notifications | Keep Media3 playback, image loading, UI state, Android storage permissions, and foreground service behavior out of the Rust core. |

## Keep out of the Android core

| Upstream area | Reason |
|---|---|
| `tui/` and `main.rs` terminal setup | Depends on Ratatui, Crossterm, terminal raw mode, mouse capture, and CLI lifecycle. |
| `player.rs` and `proxy.rs` | Launches external players/processes and has desktop/Termux assumptions. AshTube will use Media3 instead. |
| `updater/` | Self-updating binaries are not an Android APK update mechanism. |
| `ratatui`, `ratatui-crossterm`, `ratatui-image`, `crossterm` dependencies | Required only for terminal rendering and input. |
| Direct `dirs`, XDG, Termux, `MOVIEBOX_*` environment-path logic | Android storage must use app-private storage, MediaStore, or the Storage Access Framework. |
| Upstream provider defaults | They must not be enabled merely because they exist upstream; each one needs an authorization and policy decision. |

## Required refactors before code reuse

1. Replace calls from `service.rs`, `history.rs`, and related code to `tui::text` with a small neutral utility module for parsing, formatting, and language-label cleanup.
2. Introduce a `Storage` trait for history, favourites, provider cache, session state, and download metadata. The Android implementation must not rely on desktop paths or environment variables.
3. Split `MovieBoxService` into focused services: catalogue/search, details/release resolution, subtitles, and artwork bytes. Do not expose its provider instances directly through FFI.
4. Add an explicit provider registry with a disabled-by-default policy. Only approved providers may be registered in an AshTube build.
5. Replace Android external-player/process launching with a `PlaybackSource` result for Kotlin/Media3.
6. Change the downloader interface to receive an approved destination abstraction and request metadata. Android will control file selection, foreground execution, notifications, and cleanup.
7. Define FFI-safe request/result/error types. Errors must be actionable but must never reveal private stream URLs, cookies, or authorization headers.

## First FFI surface after extraction

```text
CoreClient::home(section, page) -> Page<CatalogItem>
CoreClient::search(query, filters, page) -> Page<CatalogItem>
CoreClient::details(media_id) -> MediaDetails
CoreClient::playback_options(media_id, season?, episode?) -> PlaybackOptions
CoreClient::subtitles(media_id, resource_id) -> List<SubtitleTrack>
CoreClient::download_plan(selection) -> DownloadPlan
```

The initial Android build must wire these calls to a mock provider first. A real provider must not be enabled before its source authorization, terms, privacy effect, age classification, and distribution constraints are reviewed.

## Next implementation step

Create an empty Rust workspace inside AshTube with separate `core-models`, `core-providers`, `core-downloads`, and `ffi` crates. Add only the new interfaces and mock test data first; do not copy an upstream provider implementation until the module boundary builds and is tested.
