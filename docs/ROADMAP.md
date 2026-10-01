# Roadmap

Language Platform is an IELTS practice app built by one developer, part-time, who will also use it to study.
The goal is to be able to study with it daily by the end of Phase 2, then add AI-graded Writing and Speaking.

**Current focus: Backend + Android.** iOS/macOS follow each feature shortly after (shared logic lives in `shared/`);
Windows and Web come last.

## Architecture (summary)

- **Shared core (KMP):** `core/model`, `core/exam-engine`, `shared` (network, cache, repositories, ViewModels).
- **UI:** Compose for Android + Desktop (`ui-compose`), SwiftUI for iOS + macOS (`app-apple`), Next.js for Web (later).
- **Backend:** Ktor modular monolith, Docker image, PostgreSQL. Long-running AI grading runs as async jobs.
- **Hosting while personal:** Cloud Run (API) + Neon (Postgres) + Cloudflare R2 (audio) + Firebase Auth.
- **AI:** Claude API for Writing band scoring; local models on the Mac mini (16 GB) for Whisper and light feedback.
  A `ScoringProvider` interface keeps both swappable.
- **Design system:** design tokens in JSON → Style Dictionary → Compose / SwiftUI / CSS. No hard-coded colors or sizes.

## Phases (estimates for one part-time developer)

| Phase | When | Scope | Gate to move on |
| --- | --- | --- | --- |
| 1. Foundation | Month 1 | Build green, CI, backend on Postgres + deploy, design tokens + core components, navigation | App runs on your phone with tests served by the backend |
| 2. Daily study | Months 2–3 | Reading + Listening with per-question explanations, tap-to-save words, vocabulary review (FSRS), streak and progress | You use this app daily instead of another one |
| 3. Writing + AI | Months 4–5 | Writing room (Task 1/2 graded on 4 criteria, sentence fixes), "Ask AI" in lessons, sign-in + sync | AI band error vs. teacher scores is within the agreed threshold |
| 4. Speaking | Months 6–7 | Record Part 1–3, pronunciation + LLM feedback, shadowing/dictation | Pronunciation scores are stable across retries |
| 5. Mock tests | Months 8–9 | Entry test, full 4-skill mock tests, study plan by target band; then macOS, Windows, Web | — |

## Feature priorities (inspired by PREP, own identity)

| Priority | Feature |
| --- | --- |
| P1 | Reading/Listening practice with explanations · save words while reading · FSRS vocabulary review · streak + progress |
| P2 | Writing room with AI grading · "Ask AI" inside lessons |
| P3 | Speaking room · entry test + study plan · full mock tests |
| Skipped | Video lessons (too costly to produce solo) — replaced by short tips per question type |

## How work is executed

Work is split into small, independently verifiable tasks in [`docs/backlog/`](backlog/README.md).
Several Claude Code agents can work on them in parallel on the Mac, each in its own git worktree
(see [`scripts/agents/`](../scripts/agents/README.md)). Every task lists its dependencies, acceptance criteria and
the exact Gradle command that must pass. You review and merge; merged tasks unblock the next ones.
