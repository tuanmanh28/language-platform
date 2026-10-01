# AND-15 — Flashcard decks like Quizlet

- **Type / branch:** `feat` / `feat/flashcard-decks`
- **Lane:** android
- **Depends on:** AND-05, AND-18, BE-18, DS-05
- **Verify:** `./gradlew spotlessCheck :shared:jvmTest :ui-compose:screenshots :app-android:assembleDebug :app-desktop:compileKotlin :shared:compileKotlinIosSimulatorArm64`
- **Reviews:** code-reviewer, design-reviewer

## Goal
Organise vocabulary into decks with rich cards.

## Scope
- Decks: create, rename, delete, colour/icon; default decks "Từ đã lưu" (from tests) and per-test decks created automatically when saving words.
- Card: term, IPA, audio (play), part of speech, Vietnamese meaning, English definition, examples, collocations, word family, optional image, personal note, source sentence with a link back to the test/audio range. Auto-filled from BE-18, editable.
- Card editor with live preview; bulk add by pasting a word list; import/export CSV and Quizlet-style text (`term<TAB>definition` per line).
- Deck screen: card list with search, sort (due, added, difficulty), suspend/unsuspend, move between decks; counts of new / learning / due.
- SQLDelight schema with `.sqm` migration from AND-05's words table (no data loss).
- Design: follow `docs/design/vocabulary/` (spec and mockups) and the `ux-design` skill, including transitions and motion.

## Rules
- Real IELTS/Cambridge material never enters git, tests, fixtures, logs or commit messages. Tests use small original fixtures you write yourself.
- No secret is needed to finish: anything that talks to an external service sits behind an interface with a fake used in tests.
- UI uses design-system components only; user-facing strings in Vietnamese resources; all logic in `shared`/`core` with tests.

## Out of scope
Study modes (AND-16), statistics (AND-17).

## Acceptance criteria
- Migration test from the AND-05 schema; import/export round-trip tests; repository tests.

## Definition of done
- Verify command passes and the code review approves.
- Follows `CLAUDE.md` and the skills.
- Committed on branch `feat/flashcard-decks` as `feat: <summary>` (no task id in the message).
