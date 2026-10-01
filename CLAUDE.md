# CLAUDE.md — guide for AI agents working in this repo

Read this before changing anything. Then read the task spec you were given in `docs/backlog/`.

## Project

IELTS practice platform. Kotlin Multiplatform shared core, native UI per platform, Ktor backend.
See `README.md` (how to run) and `docs/ROADMAP.md` (what and why).

| Module | What lives there |
| --- | --- |
| `core/model` | Serializable models shared by apps and backend. Sample tests from `content/` are embedded at build time. |
| `core/exam-engine` | Pure scoring logic (answer normalisation, band tables). No I/O. |
| `shared` | KMP data layer: Ktor client, SQLDelight cache, repositories, ViewModels, Koin DI. Targets android, jvm, iosArm64, iosSimulatorArm64, macosArm64. |
| `ui-compose` | Compose Multiplatform screens/components for Android + Desktop. |
| `app-android`, `app-desktop` | Thin entry points. |
| `app-apple` | SwiftUI (iOS + macOS). Xcode project is generated from `project.yml` with XcodeGen. |
| `backend` | Ktor server (JVM 21), Dockerfile, tests with `testApplication`. |
| `content/` | Test content as JSON + JSON Schema. |

## Commands

```bash
./gradlew :core:model:jvmTest :core:exam-engine:jvmTest   # core logic
./gradlew :shared:jvmTest                                  # repositories, ViewModels (JVM)
./gradlew :backend:test                                    # API tests
./gradlew :app-android:assembleDebug                       # Android build
./gradlew :app-desktop:compileKotlin                       # Desktop (shares ui-compose)
./gradlew :shared:compileKotlinIosSimulatorArm64          # keep shared iOS-compatible (slow, run when touching shared)
```

Your task spec lists the exact verification command. **It must pass before you commit.**

## Rules

1. **Stay in scope.** Implement only your task. If you find unrelated problems, list them in your final summary instead of fixing them.
2. **Keep the architecture:**
   - Business logic goes in `shared` / `core`, never in Android or SwiftUI code. UI renders `StateFlow` state and calls ViewModel actions.
   - New ViewModels: `androidx.lifecycle.ViewModel` in `shared`, registered in `shared/.../di/Koin.kt`, exposed to Swift via `ViewModels`.
   - Anything with I/O behind an interface so it can be faked in tests.
   - `shared` must stay compilable for iOS/macOS: no JVM-only APIs in `commonMain`.
3. **Dependencies** go in `gradle/libs.versions.toml`. Use the latest stable version compatible with Kotlin 2.4.20 and check that it exists on Maven Central/Google Maven before using it. Do not upgrade existing versions unless the task says so.
4. **Tests are required** for logic you add (`commonTest`/`jvmTest` in KMP modules, `src/test` in backend). Use fakes, not mocks of our own classes.
5. **UI:** use design-system components and theme tokens from `ui-compose` once they exist (task AND-01/AND-02); never hard-code colors or dimensions in screens. User-facing strings are Vietnamese for now.
6. **Database:** backend schema changes only through new Flyway migrations (`V<n>__description.sql`); never edit an applied migration. App cache schema changes through SQLDelight `.sqm` migrations.
7. **Do not touch:** `_reference/`, generated files, other tasks' areas, CI secrets.
8. **Git:**
   - Work only on the branch you were started on. Never push, never rebase or rewrite `main`.
   - Commit message: `<TASK-ID>: <short summary>`.
   - **Do not add `Co-Authored-By`, "Generated with Claude", or any AI attribution** to commits or files.
   - Do not change git config.
9. **If blocked** (missing secret, ambiguous requirement, failing build you cannot fix), stop. Write `BLOCKED.md` at the repo root explaining what you need, commit it, and finish.

## Final summary (print at the end)

- What you changed (files/modules)
- How you verified it (commands + result)
- Follow-ups or risks for the reviewer
