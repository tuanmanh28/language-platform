# AND-17 — Spaced repetition settings and statistics

- **Type / branch:** `feat` / `feat/srs-settings-and-stats`
- **Lane:** android
- **Depends on:** AND-06, AND-16, AND-18, DS-05
- **Verify:** `./gradlew spotlessCheck :shared:jvmTest :ui-compose:screenshots :app-android:assembleDebug :app-desktop:compileKotlin :shared:compileKotlinIosSimulatorArm64`
- **Reviews:** code-reviewer, design-reviewer

## Goal
Control the review load and see memory improving.

## Scope
- Settings per deck: new cards per day, maximum reviews per day, desired retention (0.80–0.97 with the expected workload shown), learning/relearning steps, maximum interval.
- Statistics: reviews per day (heatmap), true retention, cards by state, forecast of due cards for the next 30 days, average time per card, hardest cards (most lapses).
- Daily vocabulary goal and reminder hooked into AND-06's streak and notifications.
- Design: follow `docs/design/vocabulary/` (spec and mockups) and the `ux-design` skill, including transitions and motion.

## Rules
- Real IELTS/Cambridge material never enters git, tests, fixtures, logs or commit messages. Tests use small original fixtures you write yourself.
- No secret is needed to finish: anything that talks to an external service sits behind an interface with a fake used in tests.
- UI uses design-system components only; user-facing strings in Vietnamese resources; all logic in `shared`/`core` with tests.

## Out of scope
FSRS parameter optimisation from review logs (later).

## Acceptance criteria
- `shared` tests for limits, forecast and retention calculations.

## Definition of done
- Verify command passes and the code review approves.
- Follows `CLAUDE.md` and the skills.
- Committed on branch `feat/srs-settings-and-stats` as `feat: <summary>` (no task id in the message).
