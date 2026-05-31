# Agents

This file describes the roles and operating rules for coding agents working on this project.

## General Developer

- Handles general Kotlin/Android development tasks.
- Implements features and fixes bugs.
- Follows Android best practices and MVVM architecture.

## Cloud Integration Specialist

- Implements OAuth2 authentication flows for cloud providers.
- Handles API integrations for Dropbox, Google Drive, Google Photos, and OneDrive.
- Manages token storage and refresh logic.

## UI/UX Developer

- Implements Material Design components.
- Creates responsive layouts for different screen sizes.
- Ensures accessibility compliance.
- **Remote source status always shows account email** - When a user is authenticated to a remote source, the status text MUST display `Signed in as [account email]`, never just `Authenticated` or `Connected`. The toast notification on sign-in MUST show `Successfully signed in as [account email]`. Use `GoogleSignInAccount.email`, not `displayName`.

## Android Systems Specialist

- Implements Daydream/DreamService functionality.
- Handles Android settings integration.
- Manages background services and permissions.
- Optimizes for TV devices such as NVidia Shield.

---

## Critical: No Legacy Code / Zero Backward Compatibility

This project is pre-release. There is zero guaranteed compatibility with previous versions.

- Do NOT write migration code for old settings formats.
- Do NOT keep legacy fallbacks for backwards compatibility.
- If a data structure changes, break it and use the new one. Old structures should be deleted entirely.

---

## Documentation Rule

Any coding agent that adds, removes, or modifies a feature MUST update all relevant markdown files before completing the task.

Do not rely on memory or assumptions about what the app does. The markdown files are the source of truth for agents that pick up this project later.

### Files to Check When Making Changes

| File | Update When... |
|------|---------------|
| `README.md` | Adding/removing features, changing tech stack |
| `ARCHITECTURE.md` | Adding new layers, changing patterns, new key classes |
| `CODING_STANDARDS.md` | Changing engineering standards, review rules, or verification expectations |
| `CHANGELOG.md` | Every feature addition, bug fix, or breaking change |
| `TODO.md` | Adding new tasks, completing existing tasks, changing quality status |
| `BUILD.md` | Changing build config, setup steps, or troubleshooting steps |
| `GOOGLE_CLOUD_SETUP.md` | Changing OAuth config or setup steps, if this file exists |
| `SETTINGS.md` | Adding/removing settings, if this file exists |
| `USER_GUIDE.md` | Changing user-facing flows or features |
| `RELEASE_NOTES.md` | Preparing a release, if this file exists |

### Documentation Ownership

- `CHANGELOG.md` is the release/history log.
- `TODO.md` is the active backlog and quality checklist.
- `README.md`, `ARCHITECTURE.md`, `CODING_STANDARDS.md`, `BUILD.md`, and `USER_GUIDE.md` describe durable product, technical, standards, build, and user-facing behavior.
- Do not recreate `PROGRESS.md`; it was removed because it duplicated `TODO.md` and `CHANGELOG.md`.

### Before Finishing a Task

1. Read the relevant markdown file(s).
2. Update them to reflect the changes made.
3. Do not assume the next agent knows what you did; document it in the correct place.

---

## Git Actions Are Forbidden Unless Explicitly Requested

You MUST NOT perform any git actions unless the user explicitly tells you to.

This includes but is not limited to:

- `git commit`
- `git push`
- `git pull`
- `git merge`
- `git reset`
- `git rebase`
- `git checkout` for branch switching
- Any other git command that modifies repository state

Allowed read-only git commands:

- `git status`
- `git diff`
- `git log`

`git add` is allowed only when preparing for a commit the user explicitly requested.

If you accidentally commit or push without permission:

1. Inform the user immediately.
2. Do not push again until authorized.

---

## When the User Says "Commit and Push"

Only then may you proceed, and you MUST follow this checklist:

1. Update all relevant markdown files to reflect the changes made.
2. Verify the app builds successfully with `./gradlew assembleDebug`.
3. Review changes with `git diff HEAD`.
4. Write a clear, descriptive commit message focused on why the change was made.
5. Commit with `git add -A && git commit -m "message"`.
6. Push with `git push`.

### Checklist Before `git commit && git push`

```text
[ ] CHANGELOG.md updated with all changes
[ ] TODO.md updated if tasks were added/completed
[ ] README.md updated if features changed
[ ] ARCHITECTURE.md updated if structure changed
[ ] CODING_STANDARDS.md updated if engineering standards changed
[ ] Any other relevant MD files updated
[ ] Build passes (./gradlew assembleDebug)
[ ] git diff reviewed
[ ] Clear commit message written
```
