# AND-05 — Vocabulary: save words and FSRS review

- **Type / branch:** `feat` / `feat/vocabulary-fsrs`
- **Lane:** android
- **Depends on:** AND-03
- **Verify:** `./gradlew :core:srs:jvmTest :shared:jvmTest :app-android:assembleDebug :app-desktop:compileKotlin`

## Goal
Save words while practising and review them with spaced repetition.

## Scope
- New KMP module `core/srs`: Kotlin port of the FSRS algorithm (reference: open-spaced-repetition `ts-fsrs`, MIT). Port the scheduler and default parameters; include tests ported from the reference test vectors.
- `shared`: `VocabularyRepository` (SQLDelight table for words: text, meaning, example, source test/question, FSRS card state; `.sqm` migration), `VocabularyViewModel` (due cards, rating Again/Hard/Good/Easy).
- `ui-compose`: Từ vựng tab — due count, flashcard review screen, word list with search.
- Saving API used by AND-04: `VocabularyRepository.save(word, context)`.

## Acceptance criteria
- `core/srs` tests match reference intervals; repository and ViewModel tests in `shared`.

## Definition of done
- Verify command passes.
- Follows `CLAUDE.md` (scope, architecture, tests, no AI attribution in commits).
- Committed on branch `feat/vocabulary-fsrs` as `feat: <summary>` (no task id in the message).
