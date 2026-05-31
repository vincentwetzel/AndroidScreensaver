# Coding Standards

This file defines engineering standards for Android Screensaver. Architecture details belong in `ARCHITECTURE.md`; build commands and troubleshooting belong in `BUILD.md`.

## Core Principles

- **DRY shared behavior:** Centralize repeated caching, prefetching, account routing, and serialization behavior in base classes or utilities such as `AbstractPhotoRepository`, `BaseCloudPhotoRepository`, and `SettingsManager`.
- **Separation of concerns:** UI components observe ViewModels and do not call repositories directly. ViewModels expose UI state through `StateFlow` or `LiveData` and do not hold long-lived Android `Context` or View references.
- **Zero backward compatibility:** This project is pre-release. Do not write migrations or legacy fallbacks for obsolete settings, source formats, or data structures. Replace the old structure completely.
- **Documentation-driven changes:** Feature changes, bug fixes, and architecture changes must update the relevant markdown files before completion.

## Kotlin And Android Style

- Follow idiomatic Kotlin naming: `PascalCase` for types, `camelCase` for members/functions, and `UPPER_SNAKE_CASE` for constants.
- Prefer immutable values (`val`) and small focused functions.
- Keep Android resource names lowercase with underscores, grouped by feature where practical.
- Keep comments sparse and useful. Use comments to explain non-obvious behavior, lifecycle constraints, platform quirks, or security decisions.
- Avoid stringly typed domain state where practical. Prefer enums or sealed classes for media filters, source states, errors, and UI modes.

## Architecture Rules

- Repositories implement source-specific data access behind `PhotoRepository`.
- Remote source operations must route by explicit `accountId`. Do not rely on singleton, implicit, "first available", or "last used" account state.
- Do not use Hilt `@IntoMap` multibindings for `PhotoRepository` collections. Inject concrete repositories and assemble the map explicitly in `RepositoryModule`.
- Use DataStore for persisted app settings. Settings and folder selections persist immediately on user interaction; do not add manual Save buttons.
- Model hierarchical folder selection with explicit selected and deselected states. Do not treat the absence of a selection as enough when parent cascade behavior exists.

## Concurrency And Lifecycle

- Run network, filesystem, and database work on `Dispatchers.IO`.
- Use structured concurrency. ViewModel work should run in `viewModelScope`; UI collection should use lifecycle-aware APIs such as `repeatOnLifecycle`.
- Cancel stale jobs for fast-changing UI inputs such as search fields. Prefer `collectLatest`, `debounce`, or explicit job cancellation.
- Shared repository caches must use thread-safe collections such as `ConcurrentHashMap` when background prefetch and slideshow loading can overlap.
- Clean up long-lived UI/system resources aggressively: unregister receivers, remove `Handler` callbacks, close players/clients, and clear view references in `onDestroy` or `onDetachedFromWindow`.

## Data, Media, And Caching

- Apply Content Type filters as early as possible in repository queries and counts, not only in UI code.
- Do not download full media upfront. Repositories should fetch metadata and let Coil, ExoPlayer, or repository cache paths load media just in time.
- Do not load large datasets into memory for filtering when platform or remote query APIs can do the filtering.
- Namespace memory cache keys and disk cache filenames with source and `accountId` where applicable.
- Sanitize disk filenames and URL-encode arbitrary persisted strings such as folder paths, account IDs, and emails.

## Security And Privacy

- Do not hardcode production OAuth keys, API tokens, or secrets. Use `local.properties`, environment variables, or Gradle-injected `buildConfigField` values.
- Do not log OAuth tokens, refresh tokens, authorization headers, or sensitive user data.
- Release builds must not enable file-based logcat mirroring or verbose debug diagnostics.
- Register internal broadcast receivers with `ContextCompat.registerReceiver` and `RECEIVER_NOT_EXPORTED`.
- Always check runtime permissions before protected API access and expose a clear typed error when permission is denied.

## UI And User State

- Authenticated remote source status must display `Signed in as [account email]`; do not use generic `Authenticated` or `Connected` states.
- Sign-in toasts must show `Successfully signed in as [account email]`.
- Use account email where available, not display name.
- Loading indicators, overlays, and empty states must be explicitly hidden or updated on both success and error paths.
- Settings, toggles, sliders, and checkbox changes must persist immediately.

## Dependencies And Build Hygiene

- Pin dependency versions exactly. Do not use dynamic versions such as `5.+`.
- Keep build configuration documentation in sync with Gradle files, including AGP, Gradle, `compileSdk`, `minSdk`, and `targetSdk`.
- Do not reintroduce the standalone `org.jetbrains.kotlin.android` plugin while AGP built-in Kotlin support is in use.
- Keep debug-only `BuildConfig` flags disabled for release builds.

## Testing And Verification

- Run `./gradlew assembleDebug` before committing or handing off substantial code changes.
- Run unit tests for pure Kotlin/domain logic changes.
- Run instrumentation or manual device tests for folder browsing, permissions, DreamService behavior, OAuth sign-in, media playback, and TV-specific UI.
- Add or update tests when changing shared repositories, settings serialization, account routing, media filtering, or lifecycle cleanup.
- Document any verification that could not be run and why.

## Review Checklist

- [ ] Relevant markdown files updated.
- [ ] New or changed source flows preserve explicit account routing.
- [ ] Repository work uses appropriate dispatchers and thread-safe shared state.
- [ ] UI collection and cleanup are lifecycle-aware.
- [ ] Permission, loading, empty, and error states are handled.
- [ ] Secrets and sensitive identity data are not logged.
- [ ] Dependencies are pinned.
- [ ] Build/tests/manual verification completed or documented.
