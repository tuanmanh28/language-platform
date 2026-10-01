# AND-06 — Progress and streak

- **Lane:** android
- **Depends on:** AND-03
- **Verify:** `./gradlew :shared:jvmTest :app-android:assembleDebug :app-desktop:compileKotlin`

## Goal
Motivation: show daily streak and progress per skill.

## Scope
- `shared`: `ProgressRepository` computing streak (local timezone; a day counts when ≥ 1 attempt or ≥ 10 reviews), attempts per day, best/average band per skill from local data.
- `ui-compose`: Tiến độ tab — streak ring, last-30-days activity, band trend per skill.
- Daily reminder notification on Android (WorkManager) with a time picker in Tôi tab; can be turned off.

## Acceptance criteria
- Streak logic unit-tested across day boundaries and timezones.

## Definition of done
- Verify command passes.
- Follows `CLAUDE.md` (scope, architecture, tests, no AI attribution in commits).
- Committed on the task branch as `AND-06: <summary>`.
