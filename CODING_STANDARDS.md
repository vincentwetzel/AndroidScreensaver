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
- Treat `SettingsManager` reads and writes as suspend operations. Access them from coroutines instead of adding `runBlocking` bridges in UI code.
- When saving UI state around a suspend read-modify-write, snapshot all view values and adapter state before the first suspension, then merge that captured state into the fresh DataStore config. Do not read mutable view state after a DataStore read resumes.
- Model hierarchical folder selection with explicit selected and deselected states. Do not treat the absence of a selection as enough when parent cascade behavior exists.

## Concurrency And Lifecycle

- Run network, filesystem, and database work on `Dispatchers.IO`.
- Use structured concurrency. ViewModel work should run in `viewModelScope`; UI collection should use lifecycle-aware APIs such as `repeatOnLifecycle`.
- Cancel stale jobs for fast-changing UI inputs such as search fields. Prefer `collectLatest`, `debounce`, or explicit job cancellation.
- Shared repository caches must use thread-safe collections such as `ConcurrentHashMap` when background prefetch and slideshow loading can overlap.
- Clean up long-lived UI/system resources aggressively: unregister receivers, remove `Handler` callbacks, close players/clients, and clear view references in `onDestroy` or `onDetachedFromWindow`.

## Data, Media, And Caching

- Apply Content Type filters as early as possible in repository queries and counts, not only in UI code.
- Pass `MediaTypeFilter` values through the UI/ViewModel/repository stack directly instead of converting them into lowercased string aliases.
- Do not download full media upfront. Repositories should fetch metadata and let Coil, ExoPlayer, or repository cache paths load media just in time.
- Do not load large datasets into memory for filtering when platform or remote query APIs can do the filtering.
- Namespace memory cache keys and disk cache filenames with source and `accountId` where applicable.
- Sanitize disk filenames and URL-encode arbitrary persisted strings such as folder paths, account IDs, and emails.

## Security And Privacy

- **Secret Management:** Do not hardcode production OAuth keys, API tokens, or secrets. Use `local.properties`, environment variables, or Gradle-injected `buildConfigField` values. *Note: OAuth Client IDs are public by necessity, but Client Secrets must never be shipped in the APK.*
- **Token Storage:** Always store sensitive credentials (such as OAuth access and refresh tokens) securely using Android's `EncryptedSharedPreferences`. Standard non-sensitive preferences can remain in `DataStore`.
- Initialize encrypted token storage lazily when it may otherwise block startup or hit keystore I/O on the main thread.
- **Data Leakage & Logging (OWASP Mobile):** Do not log OAuth tokens, refresh tokens, authorization headers, or sensitive user personal data (PII).
- **Network Security:** All remote internet traffic must use HTTPS (TLS 1.2/1.3). Cleartext HTTP traffic must remain disabled.
- **Data Privacy (GDPR Compliance):** The app acts as a local client. All cached user data, photos, and tokens must remain strictly on-device and must be comprehensively deleted when the user signs out or clears the app cache.
- Release builds must strictly disable file-based logcat mirroring and verbose debug diagnostics.
- **Release Hardening:** Release builds must enable code shrinking and obfuscation via R8/ProGuard.
- **IPC Security:** Register internal broadcast receivers with `ContextCompat.registerReceiver` and `RECEIVER_NOT_EXPORTED`. All `PendingIntent` creations must specify `PendingIntent.FLAG_IMMUTABLE` unless mutability is strictly required by an API. Use explicit intents for all internal component navigation.
- **Backup Security:** Explicitly exclude sensitive local data (such as `EncryptedSharedPreferences` containing OAuth tokens) from Android Auto-Backup rules to prevent token migration across devices.
- **Input Validation & Deep Links:** Strictly validate all incoming data from external intents, deep links, or OAuth redirect URIs to prevent intent spoofing or open redirect attacks.
- **Secure Web Usage:** Avoid using `WebView` for external links or auth flows. Rely on official SDKs, the system browser, or Chrome Custom Tabs to prevent cookie-theft and XSS within the app sandbox.
- **Cryptographic Standards:** Never use deprecated cryptographic algorithms (e.g., MD5, SHA-1). Always rely on Android Jetpack Security (`EncryptedSharedPreferences`) or the Android Keystore system for any new cryptographic operations.
- **Supply Chain Security:** Regularly audit third-party dependencies (e.g., Coil, ExoPlayer, cloud SDKs) and update them to patch known vulnerabilities (CVEs).
- **Tapjacking Protection:** Protect critical UI components (such as OAuth sign-in buttons or destructive actions) against malicious screen overlays by ensuring touches are filtered when obscured.
- **Certificate Pinning:** Enhance network security by pinning SSL/TLS certificates or public keys for primary cloud APIs (e.g., Google, Dropbox) via OkHttp or Network Security Config, preventing MitM attacks from compromised CAs.
- **Screen Security:** Consider using `WindowManager.LayoutParams.FLAG_SECURE` on screens displaying highly sensitive auth flows or when users opt-in to privacy protections for their personal media.
- **Component Security:** All Android components (`<activity>`, `<service>`, `<receiver>`, `<provider>`) in the Manifest must explicitly set `android:exported="false"` unless external system access is strictly required (e.g., `MainActivity`, `PhotoScreensaverService`).
- **Network Security Configuration:** Enforce the disablement of cleartext HTTP traffic at the OS level by defining `cleartextTrafficPermitted="false"` in a dedicated `res/xml/network_security_config.xml` file.
- **Environment Integrity:** Acknowledge that device rooting breaks the Android sandbox and compromises `EncryptedSharedPreferences`. Consider warning the user if compromised device integrity places their stored OAuth tokens at risk.
- **Local Storage Privacy:** All cached cloud media and sensitive files must reside strictly in app-internal storage (`Context.getFilesDir()` or `Context.getCacheDir()`). Never cache private media to public external storage where other apps can read it.
- **Telemetry Sanitization:** If crash reporting or analytics are used, explicitly sanitize all exception messages and breadcrumbs before transmission to ensure OAuth tokens, secrets, and user PII are never sent to third-party servers.
- **Secure Randomness:** Never use `kotlin.random.Random` for security-sensitive operations (e.g., OAuth state tokens, crypto IVs). Always use `java.security.SecureRandom`.
- **OAuth Principle of Least Privilege:** Always request the absolute minimum necessary OAuth scopes (e.g., read-only access to Drive/Dropbox). Never request write, delete, or full-account access for a media viewer application.
- **File Sharing Security:** If exposing local cached media to external components or intents, use a strictly configured `FileProvider` with temporary `FLAG_GRANT_READ_URI_PERMISSION`. Never pass raw `file://` URIs across IPC boundaries.
- **Memory & State Clearance:** Upon account sign-out, ensure all in-memory variables, `ViewModel` states, and singletons holding access tokens or user PII are explicitly cleared or nulled out to prevent data from lingering in heap memory.
- **Anti-Tampering & Anti-Debugging:** Release builds must actively check for and reject attached debuggers (`android.os.Debug.isDebuggerConnected()`). Consider runtime APK signature verification to prevent repackaging attacks.
- **Dynamic Code Loading Restrictions:** The app must never download and execute dynamic code (e.g., DEX, JAR, APK, or remote JavaScript). All executable code must be statically packaged within the original APK.
- **Intent Redirection & Payload Sanitization:** Strictly validate and sanitize all `Intent` extras and `Bundle` data. Never use unvalidated `Intent` extras to dynamically start other components or access file paths, preventing Intent Redirection and Path Traversal vulnerabilities.
- **OAuth CSRF & PKCE Protection:** All OAuth authentication flows must implement a cryptographically secure `state` parameter to prevent Cross-Site Request Forgery (CSRF). Where supported, enforce PKCE (Proof Key for Code Exchange) to protect against authorization code interception.
- **Recent Tasks Snapshot Privacy:** To prevent the OS from leaking private photos or account settings into the "Recent Apps" carousel, obscure sensitive UI states in `onPause()` or apply `FLAG_SECURE` to the Window.
- **Automated Vulnerability Scanning:** Integrate automated security scanning (e.g., Dependabot, CodeQL, or OSV-Scanner) into the repository to proactively block known CVEs in third-party and transitive dependencies.
- Always check runtime permissions before protected API access and expose a clear typed error when permission is denied.

## UI And User State

- Authenticated remote source status must display `Signed in as [account email]`; do not use generic `Authenticated` or `Connected` states.
- Sign-in toasts must show `Successfully signed in as [account email]`.
- Use account email where available, not display name.
- Ensure accessibility (WCAG mobile equivalents): Provide minimum 48dp touch targets and `contentDescription` for interactive icons.
- Loading indicators, overlays, and empty states must be explicitly hidden or updated on both success and error paths.
- **Burn-in Protection (OLED/AMOLED):** All persistent on-screen overlays (such as clocks, weather, and photo metadata) must implement pixel-shifting or periodic repositioning to prevent screen burn-in. Avoid purely static, high-contrast elements that remain in the exact same coordinates for extended periods.
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
