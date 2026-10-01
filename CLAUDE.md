# CLAUDE.md

Rules for every AI agent in this repo. Read this, then your task spec in `docs/backlog/`, then the skills that apply.

## Project

IELTS practice platform: Kotlin Multiplatform shared core, native UI per platform, Ktor backend.
`README.md` explains how to run it; `docs/ROADMAP.md` explains what and why.

| Module | Contents |
| --- | --- |
| `core/model` | `@Serializable` models shared by apps and backend; bundled sample content |
| `core/exam-engine` | Pure scoring logic |
| `shared` | KMP data layer, repositories, ViewModels, Koin (android, jvm, iosArm64, iosSimulatorArm64, macosArm64) |
| `ui-compose` | Compose Multiplatform UI for Android + Desktop: design system, navigation, screens |
| `app-android`, `app-desktop` | Entry points only |
| `app-apple` | SwiftUI for iOS + macOS; Xcode project generated from `project.yml` |
| `backend` | Ktor server, Flyway, Exposed |
| `content/` | Test content JSON + schema |
| `_reference/` | Read-only reference repos (not part of the project) |

## Skills — use them

| When you… | Skill |
| --- | --- |
| write code that can fail, design errors | `error-handling` |
| add a feature end to end | `kmp-feature` |
| touch API client, SQLDelight, repositories, sync | `shared-data-layer` |
| build a Compose screen | `compose-screen` |
| add tokens or UI components | `design-system` |
| add routes, tabs, deep links | `navigation` |
| expose Kotlin to Swift or write SwiftUI | `swiftui-interop` |
| add a backend endpoint | `backend-endpoint` |
| change the database | `backend-database` |
| write tests | `testing` |
| add or upgrade dependencies, edit Gradle | `gradle-dependency` |
| name anything, format code | `code-conventions` |
| branch, commit, merge, release | `git-workflow` |

## Engineering standards

- **Naming and formatting** follow `code-conventions`; run `./gradlew spotlessApply` (once it exists) before committing.
- **Clean architecture, simple logic.** Clear layers, small functions, intention-revealing names, immutable data. The
  simplest design that is correct wins; no premature abstraction, no clever code, no dead code.
- **Comments:** avoid them. Code must explain itself. Write a comment only to explain *why* something non-obvious is done,
  in English, on one line. No KDoc on obvious members, no section dividers, no comments narrating the code.
- **Errors:** `Result<V, E>` from kotlin-result with sealed error types (`error-handling`). Exceptions are for bugs only.
  `Result` never crosses into Swift or into a `StateFlow`.
- **Latest libraries:** new dependencies use the latest stable version, looked up at the source (`gradle-dependency`).
- **Business logic** lives in `core`/`shared`/backend services — never in Compose or SwiftUI code.
- `shared` must keep compiling for iOS/macOS: no JVM-only APIs in `commonMain`.
- **Tests** for all new logic at the level the `testing` skill prescribes.
- UI uses only design-system components and tokens; user-facing strings come from resources (Vietnamese).
- Database changes only through new Flyway migrations (backend) or `.sqm` migrations (app).

## Commands

```bash
./gradlew :core:model:jvmTest :core:exam-engine:jvmTest   # core logic
./gradlew :shared:jvmTest                                  # repositories, ViewModels
./gradlew :backend:test                                    # API + database tests
./gradlew :app-android:assembleDebug                       # Android
./gradlew :app-desktop:compileKotlin                       # Desktop
./gradlew :shared:compileKotlinIosSimulatorArm64          # shared still compiles for Apple
```

Your task spec lists the exact verify command. It must pass before you commit.

## Working rules

1. **Scope:** implement only your task. List unrelated problems in your final summary instead of fixing them.
2. **Git (`git-workflow`):** stay on your branch; never push, rebase, reset, change git config or use `--no-verify`.
   Subjects are `<type>: <summary>` (`feat`, `fix`, `refactor`, `update`, `perf`, `test`, `docs`, `build`, `ci`, `chore`),
   lowercase, imperative, no scope, no trailing period, max 72 characters. Never put task ids (BE-01, AND-03…) in branch
   names or commit messages. No `Co-Authored-By`, "Generated with", or any AI attribution anywhere.
   `.githooks/commit-msg` rejects commits that break these rules: fix the message, never bypass the hook.
3. **Do not touch** `_reference/`, generated files, CI secrets, or other tasks' areas.
4. **Blocked** (missing secret, unclear requirement, failure outside your scope): write `BLOCKED.md` explaining exactly what
   you need, commit it, stop.
5. **Review is mandatory:** after verify passes, the `code-reviewer` subagent reviews your branch. `blocker`/`major`
   findings come back to you to fix.

## Final summary (print at the end)

- What changed (files/modules)
- How you verified it (commands + result)
- Follow-ups or risks for the reviewer
