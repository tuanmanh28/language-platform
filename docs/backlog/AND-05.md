# AND-05 — Vocabulary: save words and FSRS review

- **Type / branch:** `feat` / `feat/vocabulary-fsrs`
- **Lane:** android
- **Depends on:** AND-03
- **Verify:** `./gradlew spotlessCheck :core:srs:jvmTest :shared:jvmTest :app-android:assembleDebug :app-desktop:compileKotlin`

## Goal
Save words while practising and review them with spaced repetition.

## Scope
- New KMP module `core/srs`: Kotlin port of the FSRS algorithm (reference: open-spaced-repetition `ts-fsrs`, MIT). Port the scheduler and default parameters; include tests ported from the reference test vectors.
- `shared`: `VocabularyRepository` (SQLDelight table for words: text, meaning, example, source test/question, FSRS card state; `.sqm` migration), `VocabularyViewModel` (due cards, rating Again/Hard/Good/Easy).
- `ui-compose`: Từ vựng tab — due count, flashcard review screen, word list with search.
- Saving API used by AND-04: `VocabularyRepository.save(word, context)`.
- `core/srs` implements **FSRS-6 completely**, not only the interval formula: card states New/Learning/Review/Relearning, configurable learning and relearning steps (default 1m, 10m), desired retention (default 0.9), maximum interval, interval fuzz, the 21 default parameters, retrievability, and `preview(card, now)` returning the next state for all four ratings so buttons can show their intervals.
- Review log per rating (state before/after, elapsed days) stored for statistics and future parameter optimisation; undo of the last rating.

## Acceptance criteria
- `core/srs` tests match reference intervals; repository and ViewModel tests in `shared`.
- Every FSRS-6 reference test vector from `_reference/ts-fsrs` passes, including learning steps, relearning and fuzz with a fixed seed.

## Definition of done
- Verify command passes.
- Follows `CLAUDE.md` (scope, architecture, tests, no AI attribution in commits).
- Committed on branch `feat/vocabulary-fsrs` as `feat: <summary>` (no task id in the message).
