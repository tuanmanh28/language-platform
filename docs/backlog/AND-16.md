# AND-16 — Flashcard study modes with spaced repetition

- **Type / branch:** `feat` / `feat/flashcard-study-modes`
- **Lane:** android
- **Depends on:** AND-15, AND-18, DS-05
- **Verify:** `./gradlew spotlessCheck :shared:jvmTest :ui-compose:screenshots :app-android:assembleDebug :app-desktop:compileKotlin :shared:compileKotlinIosSimulatorArm64`
- **Reviews:** code-reviewer, design-reviewer

## Goal
Study cards in several ways while FSRS schedules every review.

## Scope
- **Review (FSRS):** due cards first, then new cards up to the daily limit; flip card, rate Again/Hard/Good/Easy with the next interval shown on each button (from `core/srs` preview); undo; bury for today; keyboard shortcuts 1–4 and Space on desktop.
- **Learn:** Quizlet-style rounds — multiple choice (meaning ↔ word), then typing the word, then listening and typing; a card graduates after correct answers in every step; results feed FSRS as ratings.
- **Write/Spell:** hear the audio, type the word, letter-level feedback.
- **Match:** timed game matching terms and meanings, best time per deck.
- **Test:** generated quiz (mixed question types) with a score report; wrong answers can be added to a re-study list.
- Choose direction (EN→VI, VI→EN, audio→EN), shuffle, star cards to study only starred.
- Design: follow `docs/design/vocabulary/` (spec and mockups) and the `ux-design` skill, including transitions and motion.

## Rules
- Real IELTS/Cambridge material never enters git, tests, fixtures, logs or commit messages. Tests use small original fixtures you write yourself.
- No secret is needed to finish: anything that talks to an external service sits behind an interface with a fake used in tests.
- UI uses design-system components only; user-facing strings in Vietnamese resources; all logic in `shared`/`core` with tests.

## Out of scope
Statistics (AND-17).

## Acceptance criteria
- `shared` tests for session queues (due vs new limits), learn-mode graduation, answer checking and rating mapping.

## Definition of done
- Verify command passes and the code review approves.
- Follows `CLAUDE.md` and the skills.
- Committed on branch `feat/flashcard-study-modes` as `feat: <summary>` (no task id in the message).
