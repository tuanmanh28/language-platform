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
| [BE-01](BE-01.md) | Backend configuration, environments and observability | be | R-01 |
| [BE-02](BE-02.md) | PostgreSQL with Flyway, Exposed and Testcontainers | be | BE-01 |
| [BE-03](BE-03.md) | Serve Reading tests from the database with content seeding | be | BE-02 |
| [BE-04](BE-04.md) | Firebase Auth token verification and users | be | BE-02 |
| [BE-05](BE-05.md) | Attempts sync API | be | BE-03, BE-04 |
| [BE-06](BE-06.md) | OpenAPI spec and Swagger UI | be | BE-03 |
| [BE-07](BE-07.md) | Deploy pipeline: Cloud Run + Neon | be | BE-02 |
| [BE-08](BE-08.md) | Listening content model, API and audio storage | be | BE-03 |
| [AND-01](AND-01.md) | Design tokens pipeline | android | R-01 |
| [AND-02](AND-02.md) | Core component library | android | AND-01 |
| [AND-03](AND-03.md) | App navigation shell | android | R-01 |
| [AND-04](AND-04.md) | Reading experience v2 | android | AND-02, AND-03, AND-05 |
| [AND-05](AND-05.md) | Vocabulary: save words and FSRS review | android | AND-03 |
| [AND-06](AND-06.md) | Progress and streak | android | AND-03 |
| [AND-07](AND-07.md) | Sign-in with Google and authenticated API client | android | AND-03, BE-04 |
| [AND-08](AND-08.md) | Listening player and practice | android | AND-02, AND-03, BE-08 |

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
