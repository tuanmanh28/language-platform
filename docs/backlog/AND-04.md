# AND-04 — Reading experience v2

- **Type / branch:** `feat` / `feat/reading-v2`
- **Lane:** android
- **Depends on:** AND-02, AND-03, AND-05
- **Verify:** `./gradlew :shared:jvmTest :app-android:assembleDebug :app-desktop:compileKotlin`

## Goal
Make Reading practice genuinely useful for study.

## Scope
- After submitting: per-question review with the learner's answer, the key and the `explanation` in an `ExplainSheet`; jump from a question to the relevant paragraph.
- Passage tools: tap a word → bottom sheet with the word and "Lưu từ" (uses the vocabulary repository from AND-05); highlight text (kept in memory for the session is fine).
- Question navigator (grid of numbers, answered/unanswered state) and "flag for review".
- Remember an in-progress attempt (answers + remaining time) across process death via the shared layer.
- All state logic in `shared` ViewModels with tests; Compose only renders.

## Acceptance criteria
- `shared` tests cover review state, navigator state and restore.

## Definition of done
- Verify command passes.
- Follows `CLAUDE.md` (scope, architecture, tests, no AI attribution in commits).
- Committed on branch `feat/reading-v2` as `feat: <summary>` (no task id in the message).
