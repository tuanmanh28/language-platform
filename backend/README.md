# Backend

Ktor API for the platform. How to run it and the full configuration table: [root README](../README.md#1-backend).
This page covers private content.

## Private content

Public samples written for this project live in `content/` and are served to everyone. The owner's own material
(bought IELTS/Cambridge books, official samples) is never committed: it lives in `CONTENT_DIR`, outside the repository,
and only owners can see it.

```
~/LanguagePlatform/content/   CONTENT_DIR (default)
├── reading/                  reading tests as JSON (same schema as content/reading)
├── listening/                listening tests as JSON (same schema as content/listening)
├── writing/                  writing prompts as JSON (same schema as content/writing)
├── audio/
│   └── <test-id>/section-<n>.mp3
└── images/
    └── <prompt-id>/<file name>   charts and diagrams for writing prompts
```

In a private listening test, `audioUrl` is the path relative to `audio/`, e.g. `my-test-01/section-1.mp3`. In a private
writing prompt, `imageUrl` is the path relative to `images/`, e.g. `my-task-1/chart.png`.

### Seeding

```bash
CONTENT_DIR=~/LanguagePlatform/content ./gradlew :backend:seedContent
```

Seeds `content/` as `public` and `CONTENT_DIR` as `private`. Missing sub-folders are skipped. Reseeding the same files
changes nothing; a test's `version` only increases when its content or visibility changed.

### Who sees private content

| Variable | Default | Notes |
| --- | --- | --- |
| `CONTENT_DIR` | `~/LanguagePlatform/content` | Private content root; `~/` is expanded |
| `OWNER_EMAILS` | none | Comma-separated. A signed-in user whose Firebase email is **verified** and listed is an owner |
| `DEV_AUTH_TOKEN` | none | `local` only: this bearer token signs in as the first owner without Firebase. The server refuses to start with it in `staging` or `prod` |

Everyone else, signed in or not, never sees private tests in lists and gets `404` for their ids (detail, submit,
attempts, audio). Content endpoints accept an optional `Authorization: Bearer <token>`; signed-in responses are sent
with `Cache-Control: private, no-cache` so shared caches never store them.

Using the app locally without Firebase:

```bash
OWNER_EMAILS=you@example.com DEV_AUTH_TOKEN=some-local-secret ./gradlew :backend:run
curl -H "Authorization: Bearer some-local-secret" http://localhost:8080/api/v1/reading/tests
```

### Private audio

Public tests keep their audio at `AUDIO_BASE_URL`. Private audio comes from `AUDIO_STORAGE`, which is required outside
`local` and must be `r2` there:

| `AUDIO_STORAGE` | Variables | Behaviour |
| --- | --- | --- |
| `local` (default, `local` only) | `API_BASE_URL` (default `http://localhost:<PORT>`) | Streams `CONTENT_DIR/audio` at `GET /api/v1/listening/audio/{testId}/{fileName}`, owners only, with range requests for seeking. Set `API_BASE_URL=http://10.0.2.2:8080` for the Android emulator |
| `r2` | `R2_ACCOUNT_ID`, `R2_BUCKET`, `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY` | Returns presigned GET URLs for the object `<audioUrl>` in a private Cloudflare R2 bucket. URLs are signed per 30-minute window, so they and the test's ETag stay stable within it, and each stays valid for at least an hour after it is issued |

A private listening test's `audioUrl` must be `<test-id>/<file name>` (letters, digits, `.`, `_`, `-`); seeding rejects
any other shape, and also rejects a test id that exists in both `content/` and `CONTENT_DIR`. Upload private audio to R2
with the same layout as `CONTENT_DIR/audio`, using an API token that has read access to that bucket only.

### Private writing images

Writing prompt images are stored like audio. Public prompts keep theirs at `AUDIO_BASE_URL`; private ones come from
`AUDIO_STORAGE`: `local` serves `CONTENT_DIR/images` at `GET /api/v1/writing/images/{promptId}/{fileName}` to owners,
`r2` presigns the object `images/<imageUrl>` in the same bucket as private audio, mirroring `CONTENT_DIR/images`. A
private prompt's `imageUrl` must be `<folder>/<file name>`, conventionally `<prompt-id>/<file name>`; seeding rejects any
other shape before writing anything.

### Writing submissions

`/api/v1/writing/*` needs a signed-in user. Essays are stored in `writing_submissions` with the word count computed on the
server (`core/exam-engine` `WritingWordCounter`: numbers and hyphenated words count as one word) and start as `pending`;
grading moves them through `grading` to `graded` or `failed`. Users only ever see their own submissions.

### Keeping material out of git

`.gitignore` blocks `*.mp3`, `*.m4a`, `*.wav`, `*.pdf` and typical private-content folder names as a second line of
defence. Tests use small fixtures written for this project, never real material.
