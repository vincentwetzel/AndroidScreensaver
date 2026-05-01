# Agents

This file describes the roles/agents that can work on this project.

## General Developer
- Handles general Kotlin/Android development tasks
- Implements features and fixes bugs
- Follows Android best practices and MVVM architecture

## Cloud Integration Specialist
- Implements OAuth2 authentication flows for cloud providers
- Handles API integrations for Dropbox, Google Drive, Google Photos, OneDrive
- Manages token storage and refresh logic

## UI/UX Developer
- Implements Material Design components
- Creates responsive layouts for different screen sizes
- Ensures accessibility compliance
- **Remote source status always shows account email** — When a user is authenticated to a remote source (Google Drive, Dropbox, OneDrive, Google Photos, etc.), the status text MUST display "Signed in as [account email]" — never just "Authenticated" or "Connected". The toast notification on sign-in MUST also show "Successfully signed in as [account email]". Use `GoogleSignInAccount.email`, NOT `displayName`.

## Android Systems Specialist
- Implements Daydream/DreamService functionality
- Handles Android settings integration
- Manages background services and permissions
- Optimizes for TV devices (NVidia Shield)

---

## 🚫 CRITICAL: No Legacy Code / Zero Backward Compatibility

**This project is pre-release. There is ZERO guaranteed compatibility with previous versions.**
- Do NOT write migration code for old settings formats.
- Do NOT keep "legacy" fallbacks for backwards compatibility.
- If a data structure changes, break it and use the new one. Old structures should be deleted entirely.

---

## ⚠️ Documentation Rule (MANDATORY)

**Any coding agent that adds, removes, or modifies a feature MUST update all relevant markdown files in this project before completing the task.**

Do NOT rely on your own memory or assumptions about what the app does. The MD files are the **single source of truth** for any agent that picks up this project.

### Files to Check When Making Changes

| File | Update When... |
|------|---------------|
| `README.md` | Adding/removing features, changing tech stack |
| `ARCHITECTURE.md` | Adding new layers, changing patterns, new key classes |
| `CHANGELOG.md` | Every feature addition, bug fix, or breaking change |
| `PROGRESS.md` | Completing or starting any task |
| `TODO.md` | Adding new tasks, completing existing ones |
| `BUILD.md` | Changing build config, adding new setup steps, troubleshooting steps |
| `GOOGLE_CLOUD_SETUP.md` | Changing OAuth config or setup steps |
| `SETTINGS.md` | Adding/removing settings |
| `USER_GUIDE.md` | Changing user-facing flows or features |
| `RELEASE_NOTES.md` | Preparing a release |

### Before Finishing a Task

1. Read the relevant MD file(s)
2. Update them to reflect the changes made
3. Do NOT assume the next agent knows what you did — document it

### Why This Matters

- Any agent should be able to open this project and understand it from the MD files alone
- Agents don't share conversation history — only these files persist between sessions
- Outdated docs are worse than no docs

---

## 🚫 CRITICAL: Git Actions Are STRICTLY FORBIDDEN Unless Explicitly Requested

**You MUST NOT perform ANY git actions unless the user explicitly tells you to.**

This includes but is not limited to:
- `git commit`
- `git push`
- `git pull`
- `git merge`
- `git reset`
- `git rebase`
- `git checkout` (branch switching)
- Any other git command that modifies repository state

**What you CAN do:**
- `git status` (to check state)
- `git diff` (to review changes)
- `git log` (to review history)
- `git add` (staging only, if preparing for a commit the user requested)

**If you accidentally commit or push without permission:**
1. Inform the user immediately
2. Do NOT push again until authorized

### Why This Exists

- The user controls when changes are shared
- Premature commits may be incomplete or incorrect
- The user may want to review changes before they're permanent
- Git history should be clean and intentional

---

## 🚫 CRITICAL: Documentation Rule (MANDATORY)

**Any coding agent that adds, removes, or modifies a feature MUST update all relevant markdown files in this project before completing the task.**

Do NOT rely on your own memory or assumptions about what the app does. The MD files are the **single source of truth** for any agent that picks up this project.

### Files to Check When Making Changes

| File | Update When... |
|------|---------------|
| `README.md` | Adding/removing features, changing tech stack |
| `ARCHITECTURE.md` | Adding new layers, changing patterns, new key classes |
| `CHANGELOG.md` | Every feature addition, bug fix, or breaking change |
| `PROGRESS.md` | Completing or starting any task |
| `TODO.md` | Adding new tasks, completing existing ones |
| `BUILD.md` | Changing build config, adding new setup steps, troubleshooting steps |
| `GOOGLE_CLOUD_SETUP.md` | Changing OAuth config or setup steps |
| `SETTINGS.md` | Adding/removing settings |
| `USER_GUIDE.md` | Changing user-facing flows or features |
| `RELEASE_NOTES.md` | Preparing a release |

### Before Finishing a Task

1. Read the relevant MD file(s)
2. Update them to reflect the changes made
3. Do NOT assume the next agent knows what you did — document it

### Why This Matters

- Any agent should be able to open this project and understand it from the MD files alone
- Agents don't share conversation history — only these files persist between sessions
- Outdated docs are worse than no docs

---

## 📋 When the User Says "Commit and Push"

Only then may you proceed, and you MUST follow this checklist:

1. ✅ **Update all relevant MD files** to reflect the changes made
2. ✅ **Verify the app builds successfully** (`./gradlew assembleDebug`)
3. ✅ **Review your changes** (`git diff HEAD`) to ensure nothing is missed
4. ✅ **Write a clear, descriptive commit message** (focus on WHY, not just WHAT)
5. ✅ **Commit** (`git add -A && git commit -m "message"`)
6. ✅ **Push** (`git push`)

### Checklist Before `git commit && git push`

```
□ CHANGELOG.md updated with all changes
□ TODO.md updated if tasks were added/completed
□ README.md updated if features changed
□ ARCHITECTURE.md updated if structure changed
□ Any other relevant MD files updated
□ Build passes (./gradlew assembleDebug)
□ git diff reviewed
□ Clear commit message written
```
