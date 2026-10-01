# Backlog

Small, independently verifiable tasks for the current focus (**Backend + Android**). Agents follow `CLAUDE.md` and the
skills in `.claude/skills/`; every task passes its verify command and a mandatory code review before you merge it. Each task file is a complete
brief for an AI agent or a human: goal, scope, decisions, acceptance criteria and the exact verify command.

`tasks.json` is the machine-readable index used by [`scripts/agents/orchestrator.py`](../../scripts/agents/README.md).
A task becomes **ready** when all its dependencies are merged into `main`.

| Task | Title | Lane | Depends on |
| --- | --- | --- | --- |
| [F-01](F-01.md) | Build green on JVM, Android and iOS-sim targets | core | — |
| [U-01](U-01.md) | Upgrade the whole stack to the latest stable versions | core | F-01 |
| [S-01](S-01.md) | Formatting and lint tooling | core | U-01 |
| [S-02](S-02.md) | Reformat the codebase to 2-space indentation | core | S-01 |
| [R-01](R-01.md) | Align existing code with the skills | core | S-02 |
| [S-03](S-03.md) | Blank line between sibling UI elements | core | R-01 |
| [BE-01](BE-01.md) | Backend configuration, environments and observability | be | R-01 |
| [BE-02](BE-02.md) | PostgreSQL with Flyway, Exposed and Testcontainers | be | BE-01 |
| [BE-03](BE-03.md) | Serve Reading tests from the database with content seeding | be | BE-02 |
| [BE-04](BE-04.md) | Firebase Auth token verification and users | be | BE-02 |
| [BE-05](BE-05.md) | Attempts sync API | be | BE-03, BE-04 |
| [BE-06](BE-06.md) | OpenAPI spec and Swagger UI | be | BE-03 |
| [BE-07](BE-07.md) | Deploy pipeline: Cloud Run + Neon | be | BE-02 |
| [BE-08](BE-08.md) | Listening content model, API and audio storage | be | BE-03 |
| [AND-01](AND-01.md) | Design tokens pipeline | android | R-01 |
| [AND-02](AND-02.md) | Core component library | android | AND-01, S-03 |
| [AND-03](AND-03.md) | App navigation shell | android | R-01, S-03 |
| [AND-04](AND-04.md) | Reading experience v2 | android | AND-02, AND-03, AND-05 |
| [AND-05](AND-05.md) | Vocabulary: save words and FSRS review | android | AND-03 |
| [AND-06](AND-06.md) | Progress and streak | android | AND-03 |
| [AND-07](AND-07.md) | Sign-in with Google and authenticated API client | android | AND-03, BE-04 |
| [AND-08](AND-08.md) | Listening player and practice | android | AND-02, AND-03, BE-08 |
| [BE-09](BE-09.md) | Private content store and owner-only access | be | BE-03, BE-04, BE-08 |
| [BE-10](BE-10.md) | Detailed explanations, evidence and paraphrases per question | be | BE-08 |
| [BE-11](BE-11.md) | Content import tool: extract, validate, seed | be | BE-09, BE-10 |
| [BE-12](BE-12.md) | Claude Code skill to import a full IELTS test | be | BE-11 |
| [BE-13](BE-13.md) | Strategy tips per question type | be | BE-10 |
| [AND-09](AND-09.md) | Answer review with evidence and paraphrases | android | AND-04, AND-08, BE-10 |
| [AND-10](AND-10.md) | Accuracy by question type and tips | android | AND-06, AND-09, BE-13 |
| [BE-14](BE-14.md) | Writing prompts and submissions | be | BE-09 |
| [BE-15](BE-15.md) | Detailed AI grading for Writing | be | BE-14 |
| [BE-16](BE-16.md) | Calibrate AI grading against examiner scores | be | BE-15, BE-11 |
| [AND-11](AND-11.md) | Writing room | android | AND-03, AND-07, BE-14 |
| [AND-12](AND-12.md) | Writing feedback screen | android | AND-11, BE-15, AND-15 |
| [BE-17](BE-17.md) | Sentence and word timestamps for Listening transcripts | be | BE-11, BE-19 |
| [AND-13](AND-13.md) | Intensive listening: replay section by section and sentence by sentence | android | AND-09, BE-17 |
| [AND-14](AND-14.md) | Dictation practice (chép chính tả) like Study4 | android | AND-13 |
| [BE-18](BE-18.md) | Dictionary lookup for flashcards | be | BE-04 |
| [AND-15](AND-15.md) | Flashcard decks like Quizlet | android | AND-05, BE-18 |
| [AND-16](AND-16.md) | Flashcard study modes with spaced repetition | android | AND-15 |
| [AND-17](AND-17.md) | Spaced repetition settings and statistics | android | AND-16, AND-06 |
| [BE-19](BE-19.md) | Ingest a library of IELTS books: unpack, dedupe, OCR, audio | be | BE-11 |
| [BE-20](BE-20.md) | Unattended import runner for the whole library | be | BE-12, BE-17, BE-19, BE-14 |

```
F-01 ─ U-01 ─ S-01 ─ S-02 ─ R-01

R-01 ─┬─ BE-01 ─ BE-02 ─┬─ BE-03 ─┬─ BE-05 (also BE-04)
      │                 │         ├─ BE-06
      │                 │         └─ BE-08 ─────────────┐
      │                 ├─ BE-04 ─ (BE-05, AND-07)      │
      │                 └─ BE-07                        │
      ├─ AND-01 ─ AND-02 ─────────┬─ AND-04             │
      └─ AND-03 ─┬─ AND-05 ───────┘                     │
                 ├─ AND-06                              │
                 ├─ AND-07 (also BE-04)                 │
                 └─ AND-08 (also AND-02) ───────────────┘
```

## Naming

Each task has a `type` (`feat`, `fix`, `refactor`, `update`…) and a `slug` in `tasks.json`.
Branch: `<type>/<slug>` (e.g. `feat/backend-config`). Merged into `main` as one commit `<type>: <summary>`.
Task ids are only for planning — they never appear in branch names or commit messages.

## Adding a task

1. Create `docs/backlog/<ID>.md` following an existing file (goal, scope, decisions, acceptance criteria, verify).
2. Add it to `tasks.json` with its type, slug, dependencies and verify command.
3. Keep tasks small enough for one agent session (roughly half a day of human work) and touching as few modules as possible.

### Phase 2–3 batch: private content, explanations, Writing AI

```
BE-09  ← BE-03, BE-04, BE-08        private content store, owner-only access
BE-10  ← BE-08                      explanations, evidence, paraphrases
BE-11  ← BE-09, BE-10               import tool (extract, audio, validate, seed)
BE-12  ← BE-11                      Claude Code import skill
BE-13  ← BE-10                      question-type tips
BE-14  ← BE-09                      Writing prompts and submissions
BE-15  ← BE-14                      detailed AI grading
BE-16  ← BE-15, BE-11               grading calibration
AND-09 ← AND-04, AND-08, BE-10      answer review with evidence
AND-10 ← AND-06, AND-09, BE-13      accuracy by question type + tips
AND-11 ← AND-03, AND-07, BE-14      Writing room
AND-12 ← AND-11, BE-15, AND-15      Writing feedback (sentence by sentence)
```

### Study tools: intensive listening, dictation, flashcards (Study4 / Quizlet style)

```
BE-17  ← BE-11, BE-19               transcript sentence/word timestamps (whisper.cpp alignment)
AND-13 ← AND-09, BE-17              intensive listening: section / sentence replay, A–B loop
AND-14 ← AND-13                     dictation like Study4
BE-18  ← BE-04                      dictionary lookup (IPA, audio, meanings, AI enrichment)
AND-15 ← AND-05, BE-18              flashcard decks like Quizlet
AND-16 ← AND-15                     study modes with FSRS
AND-17 ← AND-16, AND-06             SRS settings and statistics
```
Real IELTS/Cambridge material is personal study content: it lives only in `CONTENT_DIR` outside the repo and is served
to the owner's account only (BE-09). The repo keeps original samples.

### Personal library import

```
BE-19  ← BE-11                      ingest archives: unpack, dedupe, OCR, audio normalise, page classification
BE-20  ← BE-12, BE-17, BE-19, BE-14 unattended import runner (scripts/content/import-library.py)
```

