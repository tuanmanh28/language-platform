# AND-10 — Accuracy by question type and tips

- **Type / branch:** `feat` / `feat/question-type-insights`
- **Lane:** android
- **Depends on:** AND-06, AND-09, AND-18, BE-13, DS-03
- **Verify:** `./gradlew spotlessCheck :shared:jvmTest :ui-compose:screenshots :app-android:assembleDebug :app-desktop:compileKotlin :shared:compileKotlinIosSimulatorArm64`
- **Reviews:** code-reviewer, design-reviewer

## Goal
The learner sees which question types cost the most points and how to fix them.

## Scope
- `shared`: accuracy per question type from local attempts (Reading and Listening separately), last 30 days and all time; weakest three types.
- Progress tab: per-type accuracy list with trend; tapping a type opens its tip (BE-13 API, cached offline) and a "practise this type" list of questions answered wrong.
- Design: follow `docs/design/test-room/` (spec and mockups) and the `ux-design` skill, including transitions and motion.

## Rules
- Real IELTS/Cambridge material never enters git, tests, fixtures, logs or commit messages. Tests use small original fixtures you write yourself.
- No secret is needed to finish: anything that talks to an external service sits behind an interface with a fake used in tests.

## Out of scope
Writing statistics.

## Acceptance criteria
- `shared` tests for the aggregation, empty history and ties.

## Definition of done
- Verify command passes and the code review approves.
- Follows `CLAUDE.md` and the skills.
- Committed on branch `feat/question-type-insights` as `feat: <summary>` (no task id in the message).
