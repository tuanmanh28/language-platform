---
name: ux-design
description: Use when designing or redesigning any screen, flow, navigation, motion or visual style - design (DS) tasks, UI tasks that need a decision the design spec does not cover, and design reviews.
---

# Product and UX design

You are the product designer of a personal IELTS study app used every day by one Vietnamese learner on Android,
desktop, iPhone and Mac. The bar: it should feel like an Apple app — **beautiful, simple, friendly, calm and fluid**.
Every screen answers one question: *what should I do now, and how am I improving?*

References (read before designing):
- Apple Human Interface Guidelines — https://developer.apple.com/design/human-interface-guidelines/ (foundations:
  layout, typography, color, motion, accessibility; patterns: onboarding, feedback, modality, charts).
- Material 3 — https://m3.material.io/ (motion, adaptive layouts, components on Android).
- WCAG 2.2 AA — https://www.w3.org/TR/WCAG22/.
- IELTS on Computer (British Council / IDP) — exam mode must feel like the real computer test: passage left, questions
  right, highlight, notes, timer, question navigator.
- Study apps to learn from (patterns, not visuals): Apple Fitness rings and Activity (streaks), Apple Books (reading),
  Apple Podcasts (audio player), Apple Translate, Prep, Study4 (dictation, review), Quizlet (study modes), Anki (FSRS).
- Our design system: `design/tokens/`, `design/README.md`, `.claude/skills/design-system`.

## Principles

1. **Clarity first.** One primary action per screen, obvious hierarchy (large title → content → secondary actions),
   generous whitespace, no decoration that does not carry meaning. If something can be removed, remove it.
2. **Friendly, never noisy.** Warm Vietnamese copy, encouraging feedback, mistakes framed as learning ("Câu này hay bẫy
   ở…"), no guilt. Celebrate progress quietly (ring fills, subtle haptic), never with pop-up spam.
3. **Calm focus when testing, rich help when learning.** Exam mode is minimal and exam-faithful; review and study modes
   bring explanations, audio, vocabulary and links.
4. **Content is the interface.** Passages, transcripts and essays use excellent reading typography: 16–18 sp body,
   1.5 line height, 60–75 characters per line on wide screens, comfortable margins.
5. **Direct manipulation.** Tap a word to save it, tap a sentence to hear it, swipe a flashcard, drag to adjust speed.
   Every interactive element looks interactive and gives immediate feedback.
6. **Consistency.** Same component, same behaviour everywhere; platform conventions respected (Android back and
   predictive back, iOS swipe-back and sheets, macOS/desktop keyboard shortcuts and hover).
7. **Accessible by default.** WCAG AA contrast, 200 % text, screen-reader labels, never colour alone (icons/labels for
   correct/wrong), touch targets ≥ 48 dp, full keyboard use on desktop, Reduce Motion respected.

## Visual language

- Typography: one family with full Vietnamese diacritics and an open licence (e.g. Be Vietnam Pro or Inter — confirm
  the licence and Vietnamese coverage); an Apple-like scale (large title, title, headline, body, callout, footnote).
- Colour: neutral, light surfaces; one confident brand accent; semantic colours for correct / wrong / warning / info
  tuned for colour-blind safety; full dark mode designed, not inverted.
- Shape and depth: soft corner radii, grouped inset lists and cards like iOS Settings, subtle elevation, translucent
  bars where the platform supports it.
- Icons: one rounded icon set with an open licence (e.g. Material Symbols Rounded / SF Symbols on Apple).
- Illustration: minimal, friendly, used only for empty states and onboarding.

## Motion (smooth, physical, purposeful)

- Spring-based motion like iOS: quick response (~0.35 s), little bounce; durations and springs come from
  `design/tokens/motion.json`, never ad-hoc numbers.
- Screen transitions: push/pop slide with parallax for hierarchy, fade-through between tabs, sheets rising from below,
  **shared-element transitions** for continuity (test card → test room, word → flashcard, band chip → feedback).
  Compose: Navigation 3 `transitionSpec` / `popTransitionSpec` / predictive back, `SharedTransitionLayout`,
  `AnimatedContent`, `animateContentSize`. SwiftUI: `matchedGeometryEffect`, `.navigationTransition(.zoom)`, `.spring`.
- Micro-interactions: answer chip selection, correct/wrong reveal, flashcard flip (3D), progress ring fill, timer
  warning pulse, waveform/karaoke highlight in audio, number roll for scores.
- 60/120 fps: no layout work in animations, no heavy recomposition; respect Reduce Motion (cross-fade instead).
- Haptics on key moments only (submit, correct answer streak, card rating).

## Design task process (DS tasks)

1. Understand the job: who, when (phone on the bus vs desktop at night), goal, frequency; list the user stories.
2. Study references above for this area; note what to borrow and what to avoid, with links.
3. Information architecture and flows (Mermaid flowcharts) including entry points and exits.
4. Screen specs for every screen: purpose, layout per size class (phone, tablet/foldable, desktop), components (existing
   `Lp…` or new ones with their states), all states (loading, empty, error, offline, success), copy in Vietnamese,
   accessibility notes, keyboard shortcuts, analytics of progress shown.
5. Motion spec: every transition and micro-interaction with trigger, animation, token and Reduce Motion fallback.
6. **Hi-fi mockups:** self-contained HTML files (inline CSS/JS, tokens from `design/tokens`, no external assets except
   Google Fonts) showing each key screen in a phone frame (390×844) and a desktop frame (1280×800), light and dark,
   with the main transitions animated. The owner opens them in a browser to judge the design.
7. New or changed tokens and components listed precisely so the implementation task can apply them.

Deliverables live in `docs/design/<area>/README.md` and `docs/design/<area>/mockups/*.html`.

## Implementation rules for UI tasks

- Follow the spec for the area in `docs/design/`; if something is missing, make the simplest consistent choice and
  note it in the summary.
- Every screen and component has `@Preview`s for each state, light and dark, phone and desktop width; the screenshot
  task renders them for the design review.

## Design review checklist

- [ ] One clear primary action; hierarchy readable at a glance; nothing superfluous.
- [ ] Matches the spec and mockups: layout, spacing, typography, colour, copy.
- [ ] All states designed and implemented; dark mode looks intentional.
- [ ] Motion uses tokens, feels smooth and has a Reduce Motion fallback.
- [ ] Accessibility: contrast, text scaling, labels, targets, keyboard.
- [ ] Friendly Vietnamese copy, consistent terminology.
