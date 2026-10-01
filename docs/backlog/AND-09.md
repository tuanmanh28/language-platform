# AND-09 — Answer review with evidence and paraphrases

- **Type / branch:** `feat` / `feat/answer-evidence-review`
- **Lane:** android
- **Depends on:** AND-04, AND-08, BE-10
- **Verify:** `./gradlew spotlessCheck :shared:jvmTest :app-android:assembleDebug :app-desktop:compileKotlin :shared:compileKotlinIosSimulatorArm64`

## Goal
After a test, every question shows why the key is right and where it comes from.

## Scope
- `shared`: review state per question from `QuestionResult` (your answer, key, `Explanation`), filters all / wrong / flagged.
- Reading review: tapping a question scrolls the passage to the evidence paragraph and highlights the quote; paraphrase pairs shown side by side; trap shown for wrong answers.
- Listening review: transcript per section with the evidence range highlighted and tap-to-seek in the player from AND-08.
- Design-system components only; strings in resources (Vietnamese).

## Rules
- Real IELTS/Cambridge material never enters git, tests, fixtures, logs or commit messages. Tests use small original fixtures you write yourself.
- No secret is needed to finish: anything that talks to an external service sits behind an interface with a fake used in tests.

## Out of scope
Statistics (AND-10).

## Acceptance criteria
- `shared` tests for filters and evidence mapping (quote → paragraph/time range), including missing evidence.

## Definition of done
- Verify command passes and the code review approves.
- Follows `CLAUDE.md` and the skills.
- Committed on branch `feat/answer-evidence-review` as `feat: <summary>` (no task id in the message).
