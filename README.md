# Language Platform

An IELTS practice platform built natively for **Android, iOS, macOS and Windows** (plus Web in Phase 3).
Shared logic is written once in **Kotlin Multiplatform (KMP)**; the UI is native on each platform.

| Platform | UI | Directory |
| --- | --- | --- |
| Android | Jetpack Compose | `app-android/` + `ui-compose/` |
| Windows (and Linux/macOS via the JVM) | Compose Desktop | `app-desktop/` + `ui-compose/` |
| iOS + macOS | SwiftUI | `app-apple/` |
| Web | Next.js — Phase 3 | `web/` |
| Backend | Ktor (Kotlin), Docker | `backend/` |

Status: **Phase 1 scaffold** — one complete vertical slice for Reading:
test list → timed test → scoring with an estimated band, working offline too.

## Structure

```
language-platform/
├── core/
│   ├── model/          # KMP: test + result models (shared by apps & backend); embeds sample tests at build time
│   └── exam-engine/    # KMP: scoring, answer normalisation, band conversion (unit tested)
├── shared/             # KMP: networking (Ktor), cache (SQLDelight), repository, ViewModels, Koin
│                       #      → exports the "Shared" framework to Swift via SKIE
├── ui-compose/         # Compose screens shared by Android + Desktop
├── app-android/        # Android entry point
├── app-desktop/        # Desktop entry point → .msi/.exe installers for Windows
├── app-apple/          # SwiftUI for iOS + macOS; Xcode project generated from project.yml (XcodeGen)
├── backend/            # Ktor API + Dockerfile
├── content/
│   ├── reading/        # Reading tests as JSON (single source of truth for sample tests)
│   └── schema/         # JSON Schema for tests
├── web/                # Phase 3
└── docker-compose.yml  # API + PostgreSQL for local development
```

Data flow: the UI only renders the ViewModel's `state` (a StateFlow in `shared`) and calls actions
(`answer`, `submit`, …). The repository tries the API first, then the SQLite cache, then the bundled tests.

## Requirements

- A recent Android Studio (with AGP 9 support) + Android SDK
- JDK 17+ (Gradle downloads JDK 17 via foojay if needed)
- Xcode 26 + `brew install xcodegen` (for iOS/macOS)
- Docker (for the local backend)

> Open the root directory in Android Studio once so it creates `local.properties` (the Android SDK path).
> When running Gradle from a terminal without that file, set `ANDROID_HOME`.

## Running

### 1. Backend

```bash
docker compose up db                      # PostgreSQL 17 on localhost:5432
./gradlew :backend:run                    # run directly, http://localhost:8080/health
# or everything with Docker:
./gradlew :backend:shadowJar && docker compose up --build
```

Phase 1 API:

| Method | Path | Description |
| --- | --- | --- |
| GET | `/health` | `{"status", "version", "env", "database"}`; `503` with `"status": "degraded"` when the database is down |
| GET | `/api/v1/reading/tests` | List tests |
| GET | `/api/v1/reading/tests/{id}` | Test details |
| POST | `/api/v1/reading/tests/{id}/submit` | Score answers (`{"answers": {"q1": "TRUE", ...}}`) |

Configuration (environment variables; the server refuses to start on an invalid value):

| Variable | Default | Notes |
| --- | --- | --- |
| `PORT` | `8080` | |
| `APP_ENV` | `local` | `local`, `staging` or `prod`; non-local envs log JSON |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/language_platform` | Required outside `local` |
| `DATABASE_USER` | `app` | Required outside `local` |
| `DATABASE_PASSWORD` | `app` | Required outside `local` |
| `CORS_ALLOWED_ORIGINS` | `*` in `local`, none elsewhere | Comma-separated origins (`https://app.example.com`); `*` only in `local` |

Every response carries an `X-Request-Id` header (taken from the request when valid, generated otherwise); the same id
appears in the logs.

Without a running backend the apps still work with the bundled tests (and show an offline label).

### 2. Android

Run the `app-android` configuration in Android Studio, or:

```bash
./gradlew :app-android:installDebug
```

The emulator reaches the backend at `http://10.0.2.2:8080` (set in `app-android/build.gradle.kts`).

### 3. Windows / Desktop

```bash
./gradlew :app-desktop:run
./gradlew :app-desktop:packageMsi   # run on Windows to produce an .msi installer
```

### 4. iOS / macOS

```bash
cd app-apple
xcodegen                     # generates LanguagePlatform.xcodeproj from project.yml
open LanguagePlatform.xcodeproj
```

Pick the `LanguagePlatformiOS` (Simulator) or `LanguagePlatformMac` scheme and Run. An Xcode build phase calls
`./gradlew :shared:embedAndSignAppleFrameworkForXcode` to build the Kotlin framework.
Running on a real device requires setting your Team under *Signing & Capabilities*.

### Tests

```bash
./gradlew :core:model:jvmTest :core:exam-engine:jvmTest :shared:jvmTest :backend:test
```

## Planning and AI agents

- [`docs/ROADMAP.md`](docs/ROADMAP.md) — phases, priorities and architecture decisions.
- [`docs/backlog/`](docs/backlog/README.md) — small tasks with acceptance criteria and verify commands (current focus: backend + Android).
- [`scripts/agents/`](scripts/agents/README.md) — runs several Claude Code agents in parallel on ready tasks, one git worktree each.
- [`CLAUDE.md`](CLAUDE.md) — rules every AI agent follows in this repo.

## Adding a test

1. Add a JSON file to `content/reading/` following `content/schema/reading-test.schema.json`
   (question numbers run consecutively from 1; MCQ answers must be option `key`s).
2. Rebuild: the test is embedded into the apps and the backend; `BundledContentTest` validates it.

Test content must be **original or properly licensed** — never use material from Cambridge books or real exams.

## Technical notes

- The conversion tables in `BandScale` are reference tables; the UI always says "estimated band".
- Phase 1 ships answer keys to the client for offline scoring. Scoring moves to the server once Premium exists.
- Only `macosArm64` (Apple Silicon) is built. Add a `macosX64` target if Intel Macs are needed.
- Windows uses Compose Desktop (JVM). PeopleInSpace also has a fully native WinUI 3 client that calls
  Kotlin/Native through NuGet (see `_reference/PeopleInSpace/windows/`) — still experimental, to revisit later.
- UI strings are in Vietnamese for now (hard-coded); they will move to string resources when localisation is added.

## Credits

The initial KMP setup was based on [PeopleInSpace](https://github.com/joreilly/PeopleInSpace) (Apache-2.0) — see `NOTICE`.
A reference clone lives in `_reference/PeopleInSpace` (not part of the repo; listed in `.gitignore`).
