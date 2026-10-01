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
./gradlew :backend:seedContent            # upsert content/ (public) and CONTENT_DIR (private) into the database (idempotent)
./gradlew :backend:run                    # run directly, http://localhost:8080/health
# or everything with Docker:
./gradlew :backend:shadowJar && docker compose up --build
```

Without the Android SDK, add `-PbackendOnly` to Gradle commands to configure only `core` and `backend`.
Deploying to Cloud Run + Neon: [`docs/deploy.md`](docs/deploy.md).

Phase 1 API:

| Method | Path | Description |
| --- | --- | --- |
| GET | `/health` | `{"status", "version", "env", "database"}`; `503` with `"status": "degraded"` when the database is down |
| GET | `/api/v1/reading/tests` | List tests |
| GET | `/api/v1/reading/tests/{id}` | Test details |
| POST | `/api/v1/reading/tests/{id}/submit` | Score answers (`{"answers": {"q1": "TRUE", ...}}`) |
| GET | `/api/v1/listening/tests` | List tests |
| GET | `/api/v1/listening/tests/{id}` | Test details: 4 sections with absolute audio URLs, transcripts and questions |
| POST | `/api/v1/listening/tests/{id}/submit` | Score answers on the Listening band scale |
| GET | `/api/v1/listening/audio/{testId}/{fileName}` | Private audio from `CONTENT_DIR/audio`, owners only (`AUDIO_STORAGE=local`) |
| GET | `/api/v1/me` | Signed-in user `{"id", "email", "displayName"}`; needs `Authorization: Bearer <Firebase ID token>`, `401` otherwise |
| GET | `/api/v1/writing/prompts` | Writing prompts with absolute image URLs; signed-in only |
| POST | `/api/v1/writing/submissions` | Store an essay (`{"promptId", "text"}`) as `pending` with its IELTS word count; signed-in only |
| GET | `/api/v1/writing/submissions` | The signed-in user's essays, newest first |
| GET | `/api/v1/writing/submissions/{id}` | One of the signed-in user's essays; `404` for anyone else's |
| GET | `/api/v1/writing/images/{promptId}/{fileName}` | Private prompt images from `CONTENT_DIR/images`, owners only (`AUDIO_STORAGE=local`) |

Configuration (environment variables; the server refuses to start on an invalid value):

| Variable | Default | Notes |
| --- | --- | --- |
| `PORT` | `8080` | |
| `APP_ENV` | `local` | `local`, `staging` or `prod`; non-local envs log JSON |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/language_platform` | Required outside `local` |
| `DATABASE_USER` | `app` | Required outside `local` |
| `DATABASE_PASSWORD` | `app` | Required outside `local` |
| `CORS_ALLOWED_ORIGINS` | `*` in `local`, none elsewhere | Comma-separated origins (`https://app.example.com`); `*` only in `local` |
| `CONTENT_SOURCE` | `db` | `db` serves published tests from PostgreSQL; `bundled` serves the tests compiled into `core/model` |
| `AUDIO_BASE_URL` | `http://localhost:9000/audio` in `local` | Required outside `local`. Public `http(s)` base URL of the audio bucket or CDN (e.g. a Cloudflare R2 public domain); listening audio and public writing images are stored as paths relative to it |
| `FIREBASE_PROJECT_ID` | `demo-language-platform` | Required outside `local`; ID tokens must be issued for this project |
| `CONTENT_DIR` | `~/LanguagePlatform/content` | Private content outside the repo; see [`backend/README.md`](backend/README.md) |
| `OWNER_EMAILS` | none | Comma-separated verified emails that may see private content |
| `DEV_AUTH_TOKEN` | none | `local` only: bearer token that signs in as the first owner without Firebase |
| `AUDIO_STORAGE` | `local` (required outside `local`) | Private audio and writing images: `local` (local env only) streams `CONTENT_DIR/audio` and `CONTENT_DIR/images` (base URL `API_BASE_URL`), `r2` presigns URLs (`R2_ACCOUNT_ID`, `R2_BUCKET`, `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`) |

Sign-in (Google, Apple, email) happens in the apps with Firebase Authentication; the backend only verifies the ID token
against Google's public keys and creates the user on its first authenticated request. To find the project id, open the
[Firebase console](https://console.firebase.google.com/), select the project, then **Project settings → General →
Project ID** (it is also `project_id` in `google-services.json` and `PROJECT_ID` in `GoogleService-Info.plist`). The reading
and listening endpoints stay public (a token additionally unlocks private tests for owners); user-specific endpoints
require a token.

Seeding publishes new tests and bumps a test's `version` only when its content changed. The reading `GET` endpoints
return an `ETag` derived from that version with `Cache-Control: public, no-cache` (`private, no-cache` when signed in),
and answer `304` to a matching `If-None-Match`.

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
./gradlew :ui-compose:screenshots   # every Compose @Preview → ui-compose/build/screenshots/<Preview>_<variant>.png
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

Listening tests go to `content/listening/` following `content/schema/listening-test.schema.json` (four sections;
`audioUrl` is a path relative to `AUDIO_BASE_URL`; transcript segments ordered and within the section duration) and are
validated by `BundledListeningContentTest`. Audio files live in object storage, never in git.

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
