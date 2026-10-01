# AND-06 — Progress and streak

- **Type / branch:** `feat` / `feat/progress-streak`
- **Lane:** android
- **Depends on:** AND-03, AND-18, DS-02
- **Verify:** `./gradlew spotlessCheck :shared:jvmTest :ui-compose:screenshots :app-android:assembleDebug :app-desktop:compileKotlin :shared:compileKotlinIosSimulatorArm64`
- **Reviews:** code-reviewer, design-reviewer

## Goal
Motivation: show daily streak and progress per skill.

## Scope
- `shared`: `ProgressRepository` computing streak (local timezone; a day counts when ≥ 1 attempt or ≥ 10 reviews), attempts per day, best/average band per skill from local data.
- `ui-compose`: Tiến độ tab — streak ring, last-30-days activity, band trend per skill.
- Daily reminder notification on Android (WorkManager) with a time picker in Tôi tab; can be turned off.
- Design: follow `docs/design/navigation-home/` (spec and mockups) and the `ux-design` skill, including transitions and motion.

## Acceptance criteria
- Streak logic unit-tested across day boundaries and timezones.

## Definition of done
- Verify command passes.
- Follows `CLAUDE.md` (scope, architecture, tests, no AI attribution in commits).
- Committed on branch `feat/progress-streak` as `feat: <summary>` (no task id in the message).
