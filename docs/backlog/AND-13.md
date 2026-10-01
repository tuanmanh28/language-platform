# AND-13 — Intensive listening: replay section by section and sentence by sentence

- **Type / branch:** `feat` / `feat/intensive-listening`
- **Lane:** android
- **Depends on:** AND-09, AND-18, BE-17, DS-04
- **Verify:** `./gradlew spotlessCheck :shared:jvmTest :ui-compose:screenshots :app-android:assembleDebug :app-desktop:compileKotlin :shared:compileKotlinIosSimulatorArm64`
- **Reviews:** code-reviewer, design-reviewer

## Goal
Re-listen to any part of a test as often as needed until every word is clear.

## Scope
- Practice mode for any Listening test (also opened from the answer review): choose section → question range → sentence.
- Player controls: play sentence, replay sentence, previous/next sentence, A–B loop on any range, auto-repeat N times, pause between sentences, speed 0.5–1.5×, ±2 s.
- Transcript view synced to playback (current sentence and word highlighted, karaoke style); tap a sentence to play it; hide/show transcript to test yourself; Vietnamese translation per sentence when present in content.
- Questions linked to their evidence sentences: from a wrong answer, "nghe lại đoạn này" plays exactly the evidence range, slowed down if chosen.
- Tap a word → save to flashcards with the sentence and its audio range as context.
- Player state and loop logic live in `shared` with a fake player in tests.
- Design: follow `docs/design/listening-study/` (spec and mockups) and the `ux-design` skill, including transitions and motion.

## Rules
- Real IELTS/Cambridge material never enters git, tests, fixtures, logs or commit messages. Tests use small original fixtures you write yourself.
- No secret is needed to finish: anything that talks to an external service sits behind an interface with a fake used in tests.
- UI uses design-system components only; user-facing strings in Vietnamese resources; all logic in `shared`/`core` with tests.

## Out of scope
Dictation (AND-14).

## Acceptance criteria
- `shared` tests for sentence navigation, A–B loop, repeat count and evidence-range playback.

## Definition of done
- Verify command passes and the code review approves.
- Follows `CLAUDE.md` and the skills.
- Committed on branch `feat/intensive-listening` as `feat: <summary>` (no task id in the message).
