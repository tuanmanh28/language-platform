# AND-18 — Apply the design direction to tokens, components and motion

- **Type / branch:** `feat` / `feat/apply-design-direction`
- **Lane:** android
- **Depends on:** DS-01, S-04
- **Verify:** `./gradlew spotlessCheck :shared:jvmTest :ui-compose:screenshots :app-android:assembleDebug :app-desktop:compileKotlin :shared:compileKotlinIosSimulatorArm64`
- **Reviews:** code-reviewer, design-reviewer

## Goal
The design system matches the approved direction everywhere.

## Scope
- Update `design/tokens/*.json` exactly as DS-01 specifies and regenerate Compose and Swift outputs.
- Bundle the chosen font with Compose resources; Apple-like type scale in `LpTheme`.
- Restyle existing `Lp…` components and add the new ones from the inventory, with all states and previews.
- Motion foundation: spring/duration tokens in `LpTheme`, a shared-element scope at the app root, default Navigation 3 transitions (push/pop, tab fade-through, predictive back), a Reduce Motion setting honoured by every animation, a small haptics helper.
- SwiftUI design-system counterparts updated to the same tokens.

## Rules
- Follow the `ux-design` and `design-system` skills and the area spec in `docs/design/`.
- Every screen/component state has light/dark, phone/desktop `@Preview`s so `:ui-compose:screenshots` renders it for the design review.

## Out of scope
Screen redesigns (later UI tasks).

## Acceptance criteria
- Screenshots of every component match the DS-01 mockups; design review approves.

## Definition of done
- Verify command passes and every review approves.
- Follows `CLAUDE.md` and the skills.
- Committed on branch `feat/apply-design-direction` as `feat: <summary>` (no task id in the message).
