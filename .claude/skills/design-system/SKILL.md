---
name: design-system
description: Use when adding or changing design tokens (colors, typography, spacing, radius, motion) or reusable UI components for Compose and SwiftUI, or when a screen needs a component that does not exist yet.
---

# Design system

One token source, generated for every platform. Components exist once per UI toolkit with the same name and API.

References:
- `_reference/kotlinconf-app/app/ui-components/src/commonMain/kotlin/org/jetbrains/kotlinconf/ui/theme/` — theme object,
  `CompositionLocal`s, colors, typography, shapes; `.../ui/components/` — component catalogue.
- `_reference/nowinandroid/core/designsystem/src/main/kotlin/com/google/samples/apps/nowinandroid/core/designsystem/` —
  `theme/Theme.kt`, `component/*.kt`.
- Style Dictionary docs (latest major) for formats and transforms.

## Layout

```
design/
├── tokens/            color.json, typography.json, spacing.json, radius.json, motion.json  (W3C Design Tokens)
├── config.mjs         Style Dictionary config
└── package.json
ui-compose/.../ui/designsystem/
├── theme/             LpTheme.kt (+ generated/ tokens)
└── components/        LpButton.kt, LpAnswerChip.kt, …
app-apple/Sources/DesignSystem/
├── Generated/         tokens as Swift
└── Components/        LpButton.swift, LpAnswerChip.swift, …
```

## Tokens

- Semantic names only in components: `color.surface`, `color.onSurface`, `color.feedback.correct`, `space.md`, `radius.lg`.
  Raw palette values (`blue.600`) are referenced only by semantic tokens.
- Light and dark values for every color token. Text/background pairs meet WCAG AA (4.5:1 body, 3:1 large text).
- Generated files are committed and never edited by hand. Change JSON → run `npm --prefix design run build`.

## Theme (Compose)

`LpTheme { }` provides a Material 3 color scheme and typography built from tokens, plus `LocalLpSpacing`, `LocalLpRadius`,
`LocalLpColors` (for colors Material has no slot for, e.g. correct/wrong). Access through `object LpTheme { val colors …;
val spacing …; }` like `KotlinConfTheme`.

## Components

- Prefix `Lp`, one component per file, same name and parameters in Compose and SwiftUI.
- Stateless: they receive values and lambdas; no ViewModel or repository access.
- Each has explicit visual states as an `enum` or sealed type (e.g. `AnswerChipState { Idle, Selected, Correct, Wrong }`)
  instead of several booleans.
- Every state has a Compose `@Preview` (light/dark) and a SwiftUI `#Preview`.
- Add the component to the table in `design/README.md` (name, purpose, states).

## Checklist

- [ ] No raw colors or sizes outside tokens/theme.
- [ ] Compose and SwiftUI versions match in name, parameters and states.
- [ ] Contrast, dark mode, 200 % font scale and screen-reader labels checked.
- [ ] Token build is reproducible (running it twice gives no diff).
