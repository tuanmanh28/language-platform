# AND-19 — Home, onboarding and navigation per the design

- **Type / branch:** `feat` / `feat/home-and-navigation`
- **Lane:** android
- **Depends on:** DS-02, AND-03, AND-06, AND-18
- **Verify:** `./gradlew spotlessCheck :shared:jvmTest :ui-compose:screenshots :app-android:assembleDebug :app-desktop:compileKotlin :shared:compileKotlinIosSimulatorArm64`
- **Reviews:** code-reviewer, design-reviewer

## Goal
The app opens on today's plan and moves between areas smoothly.

## Scope
- Adapt the navigation shell from AND-03 to DS-02: tabs, adaptive bar/rail/sidebar, transitions.
- "Hôm nay" home with daily plan, continue card, due flashcards, streak and goal ring, weakest type.
- Onboarding flow and settings from DS-02, stored in `shared` with tests.

## Rules
- Follow the `ux-design` and `design-system` skills and the area spec in `docs/design/`.
- Every screen/component state has light/dark, phone/desktop `@Preview`s so `:ui-compose:screenshots` renders it for the design review.

## Out of scope
Features of the other tabs.

## Acceptance criteria
- `shared` tests for the daily plan and onboarding state; design review approves screenshots.

## Definition of done
- Verify command passes and every review approves.
- Follows `CLAUDE.md` and the skills.
- Committed on branch `feat/home-and-navigation` as `feat: <summary>` (no task id in the message).
