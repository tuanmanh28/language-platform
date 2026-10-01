# AND-14 — Dictation practice (chép chính tả) like Study4

- **Type / branch:** `feat` / `feat/dictation`
- **Lane:** android
- **Depends on:** AND-13, AND-18, DS-04
- **Verify:** `./gradlew spotlessCheck :shared:jvmTest :ui-compose:screenshots :app-android:assembleDebug :app-desktop:compileKotlin :shared:compileKotlinIosSimulatorArm64`
- **Reviews:** code-reviewer, design-reviewer

## Goal
Train listening precision by typing every sentence you hear.

## Scope
- `core/exam-engine` `DictationChecker`: word-level diff between typed text and the sentence (case and punctuation insensitive; contractions, numbers vs words, British/American spelling and hyphen variants accepted), returning correct/wrong/missing/extra tokens.
- Choose any Listening test → section(s); sentences come from BE-17 timings; difficulty: full sentence, only key words (cloze), or only the words you missed last time.
- Per sentence: play, replay (keyboard shortcut and button), slow playback, type, check; result shows each word coloured (correct/wrong/missing) with the right word; hints reveal the next word's first letter or the whole word (hints lower the score); skip; show transcript.
- Session score (accuracy per sentence and section), weakest sounds/words list, resume an unfinished session, history per test.
- Words you missed can be added to flashcards in one tap with the audio range.
- Desktop: works fully with the keyboard (Enter checks, Ctrl+R replays, Ctrl+Space slows).
- Design: follow `docs/design/listening-study/` (spec and mockups) and the `ux-design` skill, including transitions and motion.

## Rules
- Real IELTS/Cambridge material never enters git, tests, fixtures, logs or commit messages. Tests use small original fixtures you write yourself.
- No secret is needed to finish: anything that talks to an external service sits behind an interface with a fake used in tests.
- UI uses design-system components only; user-facing strings in Vietnamese resources; all logic in `shared`/`core` with tests.

## Out of scope
Speaking shadowing (Phase 4).

## Acceptance criteria
- Exhaustive `DictationChecker` tests (variants above, empty input, extra words); `shared` tests for session flow, hints scoring and resume.

## Definition of done
- Verify command passes and the code review approves.
- Follows `CLAUDE.md` and the skills.
- Committed on branch `feat/dictation` as `feat: <summary>` (no task id in the message).
