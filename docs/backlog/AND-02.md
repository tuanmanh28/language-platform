# AND-02 — Core component library

- **Type / branch:** `feat` / `feat/component-library`
- **Lane:** android
- **Depends on:** AND-01
- **Verify:** `./gradlew spotlessCheck :shared:jvmTest :app-android:assembleDebug :app-desktop:compileKotlin`

## Goal
Reusable Compose components every screen is built from.

## Scope (in `ui-compose/.../ui/components/`)
`PrimaryButton`, `SecondaryButton`, `TextButton`, `AnswerChip` (TRUE/FALSE/NOT GIVEN, with selected/correct/wrong states), `OptionRow` (MCQ), `GapField` (word-limit hint + counter), `TimerBar` (warning state under 60 s), `BandBadge`, `TestCard`, `ProgressRing`, `EmptyState`, `ErrorState`, `ExplainSheet` (bottom sheet for explanations).
- Every component: uses theme tokens only, supports dark mode and font scaling, has content descriptions/semantics, and a `@Preview` per state.
- Replace ad-hoc UI in the existing Reading screens with these components (no behaviour changes).

## Acceptance criteria
- Previews compile; a simple Compose UI test (desktop/JVM) renders each component in each state without crashing.

## Definition of done
- Verify command passes.
- Follows `CLAUDE.md` (scope, architecture, tests, no AI attribution in commits).
- Committed on branch `feat/component-library` as `feat: <summary>` (no task id in the message).
