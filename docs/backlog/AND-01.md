# AND-01 — Design tokens pipeline

- **Type / branch:** `feat` / `feat/design-tokens`
- **Lane:** android
- **Depends on:** R-01
- **Verify:** `npm --prefix design ci && npm --prefix design run build && git diff --exit-code -- ui-compose && ./gradlew :shared:jvmTest :app-android:assembleDebug :app-desktop:compileKotlin`

## Goal
A single source of design tokens that generates the Compose theme (and later SwiftUI and CSS).

## Scope
- `design/tokens/*.json` in W3C Design Tokens format: color (light + dark, semantic: primary, surface, onSurface, success/correct, error/wrong, warning, outline…), typography scale, spacing (4-pt), radius, elevation, motion durations.
- `design/package.json` with **Style Dictionary** (latest major) and a build script producing:
  - Kotlin for Compose in `ui-compose/src/commonMain/kotlin/com/app/platform/language/ui/theme/generated/` (committed),
  - Swift in `app-apple/Sources/DesignSystem/Generated/` (committed),
  - CSS variables in `design/build/css/` (not committed).
- Replace the placeholder `Theme.kt` so `LanguagePlatformTheme` builds Material 3 color schemes and typography from the generated tokens; expose spacing/radius via a `CompositionLocal` (`LocalSpacing`, etc.).
- Choose a friendly, modern palette with an original identity (not PREP's colors); all text/background pairs must meet WCAG AA. Document the palette in `design/README.md`.

## Acceptance criteria
- Running the build twice produces no diff (the verify command checks this).
- No hard-coded colors remain in `ui-compose` screens.

## Definition of done
- Verify command passes.
- Follows `CLAUDE.md` (scope, architecture, tests, no AI attribution in commits).
- Committed on branch `feat/design-tokens` as `feat: <summary>` (no task id in the message).
