# AND-12 — Writing feedback screen

- **Type / branch:** `feat` / `feat/writing-feedback`
- **Lane:** android
- **Depends on:** AND-11, BE-15
- **Verify:** `./gradlew spotlessCheck :shared:jvmTest :app-android:assembleDebug :app-desktop:compileKotlin :shared:compileKotlinIosSimulatorArm64`

## Goal
Read and learn from the detailed grading.

## Scope
- Polls submission status with backoff until graded; failed state with retry.
- Overview: overall band, four criterion bands with strengths/weaknesses/reason, task checks, top 3 priorities.
- Annotated essay: marks coloured by category; tapping one opens original → correction and the Vietnamese explanation; filter by category.
- Tabs for paragraph feedback, vocabulary upgrades (save to vocabulary from AND-05) and the rewritten version with differences highlighted.
- Band history chart per criterion across submissions.

## Rules
- Real IELTS/Cambridge material never enters git, tests, fixtures, logs or commit messages. Tests use small original fixtures you write yourself.
- No secret is needed to finish: anything that talks to an external service sits behind an interface with a fake used in tests.

## Out of scope
Speaking.

## Acceptance criteria
- `shared` tests for polling, annotation mapping to text ranges and history aggregation.

## Definition of done
- Verify command passes and the code review approves.
- Follows `CLAUDE.md` and the skills.
- Committed on branch `feat/writing-feedback` as `feat: <summary>` (no task id in the message).
