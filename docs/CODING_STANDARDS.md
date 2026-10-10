# Coding Standards

This document is the source of truth for engineering rules in Android Screensaver. Keep architecture and settings design in [`ARCHITECTURE.md`](ARCHITECTURE.md), build and setup instructions in [`BUILD.md`](BUILD.md), and task history in [`CHANGELOG.md`](CHANGELOG.md).

The words **must**, **should**, and **may** indicate required, recommended, and optional practices. A rule may be relaxed only when the reason is documented in the change and the resulting behavior is verified.

## Project Principles

- Prefer simple, explicit designs over clever abstractions.
- Keep UI, state, persistence, network access, and media loading in their appropriate layers.
- This is a pre-release project: do not add migrations or legacy fallbacks for obsolete settings or data structures. Replace old structures deliberately and update the documentation.
- Make changes in small, reviewable units. Remove dead code and stale comments when behavior changes.
- Update the relevant documentation for every feature, bug fix, behavior change, or architecture change.

## Kotlin And Android Style

- Use idiomatic Kotlin: `PascalCase` for types, `camelCase` for members and functions, and `UPPER_SNAKE_CASE` for constants.
- Prefer `val`, immutable collections, early returns, null-safe APIs, and small focused functions.
- Avoid `!!`, broad exception swallowing, and `runBlocking` in application/UI code. If a non-null assertion or broad catch is unavoidable, document the invariant or recovery reason.
- Use enums or sealed classes for domain state such as source types, media filters, and typed errors instead of ad-hoc string values.
- Keep resource names lowercase with underscores and use resource strings for user-visible text.
- Comments should explain non-obvious behavior, lifecycle constraints, platform quirks, or security decisions—not restate the code.

## Architecture And State

- Activities and custom views render state and forward user actions. They must not contain repository or cloud API policy.
- ViewModels expose screen state through `StateFlow` or `LiveData`, perform work in `viewModelScope`, and must not retain views or long-lived `Context` references.
- Repositories implement source-specific access behind `PhotoRepository`. Shared behavior belongs in existing base classes or focused utilities rather than duplicated in each source.
- Every remote operation must use an explicit `accountId`. Never route by a singleton's current account, the first account, or the last account used.
- Inject concrete repositories and assemble the repository map explicitly in `RepositoryModule`; do not use Hilt map multibindings for `PhotoRepository` collections.
- Persist app settings and folder selections with DataStore. Settings changes must save immediately; do not add manual Save buttons for these flows.
- Treat `SettingsManager` reads and writes as suspend operations. Call them from a coroutine instead of adding blocking bridges.
- For a suspend read-modify-write, snapshot all mutable UI and adapter state before the first suspension, then merge that snapshot into the freshly read configuration.
- Model hierarchical folder selection with explicit selected and deselected state when parent/child cascade behavior exists.

## Concurrency, Lifecycle, And Caching

- Run network, filesystem, MediaStore, and database work on `Dispatchers.IO` unless an API explicitly requires another dispatcher.
- Use structured concurrency. Cancel stale work for fast-changing inputs with `collectLatest`, `debounce`, or an explicit job.
- Collect flows with lifecycle-aware APIs such as `repeatOnLifecycle` and stop work when the screen or service is no longer active.
- Shared caches accessed by background prefetch and slideshow loading must use thread-safe collections and have explicit TTL or invalidation behavior.
- Cache keys, filenames, and account routing must be namespaced by source and `accountId` where applicable. Sanitize filesystem names and encode arbitrary persisted identifiers.
- Fetch metadata first and load media just in time through Coil, Media3/ExoPlayer, or an app-internal repository cache. Do not download an entire source before playback can begin.
- Apply `MediaTypeFilter` in repository queries and counts as early as the source API allows; pass the enum through the stack without lowercased string aliases.
- Release resources deterministically: unregister receivers, cancel jobs, remove callbacks, release players/clients, and clear references in lifecycle teardown.

## Authentication, Security, And Privacy

- Never commit production secrets, OAuth client secrets, access tokens, refresh tokens, or authorization headers. Public client IDs may be shipped when required by a provider; secrets belong in local or environment-provided configuration.
- Store OAuth tokens and other credentials in encrypted, keystore-backed storage. Keep ordinary preferences such as UI settings in DataStore, and initialize encrypted storage lazily when keystore I/O could affect startup.
- Request the minimum provider scopes required for read-only media access. OAuth flows must validate the redirect/result, use a cryptographically secure `state` value, and use PKCE where the provider supports it.
- Do not log tokens, headers, raw provider responses containing PII, or unnecessary account identifiers. Debug log export and mirroring must remain debug-only and must not become a release data channel.
- Use HTTPS for remote traffic. Do not introduce cleartext endpoints; any provider-specific exception must be narrowly scoped, documented, and reviewed.
- Prefer official provider SDKs, the system browser, or Custom Tabs for authentication and external links. Do not build an in-app `WebView` auth flow without a documented security reason.
- Use explicit intents for internal components. Runtime-registered internal receivers must be non-exported, `PendingIntent`s must be immutable unless mutability is required, and shared files must use a narrowly configured `FileProvider` with temporary read grants.
- Validate external intent extras, OAuth results, deep links, folder identifiers, and filesystem paths before using them. Never let untrusted input select an arbitrary component or path.
- Keep private media, logs, and token material in app-private storage. On sign-out or source removal, clear tokens, related cached media, and in-memory account state.
- Exclude credential stores from backup rules so tokens are not migrated to another device.
- Use `FLAG_SECURE` for authentication screens or other screens where screenshots and recent-task previews would expose sensitive information, as appropriate to the user flow.
- Do not require certificate pinning, root detection, or anti-debugging by default. These controls can break provider endpoints, support, and legitimate debugging; add them only after a documented threat-model decision and provider compatibility test.

## UI And User Experience

- Use Material components and provide layouts that work on phones, tablets, and Android TV. Preserve TV focus order and remote-control usability.
- Interactive controls must have at least 48dp touch targets and meaningful content descriptions. Do not rely on color alone to communicate state.
- Remote source status must say `Signed in as [account email]`; never use only `Authenticated` or `Connected`. Sign-in toasts must say `Successfully signed in as [account email]`, using the account email rather than display name.
- Handle loading, success, empty, permission-denied, authentication, and error states explicitly. Hide or update progress indicators on every terminal path.
- Persist toggles, sliders, spinners, and checkbox changes immediately and restore them from the saved configuration.
- Persistent slideshow overlays must participate in burn-in protection through periodic pixel shifting or repositioning.

## Dependencies And Build Hygiene

- Pin dependency versions. Do not use dynamic versions such as `5.+`.
- Keep README/build documentation synchronized with AGP, Gradle, `compileSdk`, `minSdk`, `targetSdk`, OAuth configuration, and debug/release behavior.
- Do not reintroduce the standalone `org.jetbrains.kotlin.android` plugin while AGP built-in Kotlin support is enabled. Use the current Kotlin compiler DSL and keep KSP compatibility settings documented.
- Keep debug-only flags and diagnostics disabled in release builds. Release configuration must preserve resource shrinking/obfuscation settings when enabled by the build.
- Prefer small dependency additions with a clear owner and removal path. Review licenses, permissions, maintenance health, and known vulnerabilities before adding a library.

## Testing And Verification

- Run `./gradlew assembleDebug` for substantial changes and every build/configuration change.
- Run `python tools/run_tests.py` for the repository-level fail-fast suite. Use `--device on` when ADB coverage is required and `--device off` for a host-only run; do not treat `--skip-build` as full verification.
- Add or update unit tests for pure Kotlin logic, settings serialization, account routing, media filtering, and shared repository behavior.
- Use `--suspects` when tracking recurring runner failures across runs. It is opt-in, stores runner check names in JSON, removes checks after they pass, and leaves skipped checks unchanged; it does not replace reviewing the full report.
- Use instrumentation or manual device tests for permissions, folder browsing, OAuth, MediaStore, DreamService lifecycle, media playback, and Android TV UI.
- Test both success and failure paths for authentication, network calls, empty results, permission denial, cancellation, and lifecycle teardown.
- Record verification that could not be run and why. Do not claim a build, test, or device check passed without running it.

The current quality gaps are tracked in [`TODO.md`](TODO.md): style/static-analysis enforcement, broader repository and folder-browser tests, lint verification, and memory-leak detection remain backlog items until tooling and tests are actually present.

## Definition Of Done

- [ ] The change follows the layer, account-routing, lifecycle, and security rules above.
- [ ] Relevant documentation in the root or `docs/` is updated.
- [ ] Tests and manual checks appropriate to the change are complete, or the limitation is documented.
- [ ] `./gradlew assembleDebug` passes for substantial or build-related changes.
- [ ] No secrets, tokens, unnecessary PII, or raw private file URIs are introduced.
- [ ] Reviewers can identify the behavior change, its failure paths, and its verification from the diff and documentation.
