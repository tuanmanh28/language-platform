# AND-11 — Writing room

- **Type / branch:** `feat` / `feat/writing-room`
- **Lane:** android
- **Depends on:** AND-03, AND-07, AND-18, BE-14, DS-06
- **Verify:** `./gradlew spotlessCheck :shared:jvmTest :ui-compose:screenshots :app-android:assembleDebug :app-desktop:compileKotlin :shared:compileKotlinIosSimulatorArm64`
- **Reviews:** code-reviewer, design-reviewer

## Goal
Write Task 1 and Task 2 under exam conditions in the app.

## Scope
- Prompt list (Task 1 / Task 2 tabs), prompt detail with chart image for Academic Task 1.
- Editor: countdown (20 / 40 minutes, can continue overtime with a warning), live IELTS word count from `core/exam-engine`, minimum-words indicator, draft autosaved to SQLDelight and restored after process death.
- Submit to BE-14; offline submit is queued and sent when online.
- History list with status and band once graded.
- Design: follow `docs/design/writing/` (spec and mockups) and the `ux-design` skill, including transitions and motion.

## Rules
- Real IELTS/Cambridge material never enters git, tests, fixtures, logs or commit messages. Tests use small original fixtures you write yourself.
- No secret is needed to finish: anything that talks to an external service sits behind an interface with a fake used in tests.

## Out of scope
Feedback screen (AND-12).

## Acceptance criteria
- `shared` tests for timer, word count, draft restore and offline queue.

## Definition of done
- Verify command passes and the code review approves.
- Follows `CLAUDE.md` and the skills.
- Committed on branch `feat/writing-room` as `feat: <summary>` (no task id in the message).
