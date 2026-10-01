# Design tokens

One source of design tokens for every UI toolkit. Tokens live in `tokens/*.json` (W3C Design Tokens format) and
[Style Dictionary](https://styledictionary.com) generates code from them.

```bash
npm --prefix design ci          # once
npm --prefix design run build   # runs the token tests, then generates every platform
```

| Output | Location | Committed |
| --- | --- | --- |
| Kotlin for Compose | `ui-compose/src/commonMain/kotlin/com/app/platform/language/ui/theme/generated/` | yes |
| Swift for SwiftUI | `app-apple/Sources/DesignSystem/Generated/` | yes |
| CSS variables | `design/build/css/tokens.css` | no |

Generated files are never edited by hand: change the JSON and rebuild. The build is deterministic, so running it on
unchanged tokens produces no diff.

## Token files

| File | Contents |
| --- | --- |
| `palette.json` | Raw tonal palettes (`palette.violet.40`). Only semantic tokens reference them. |
| `color-light.json`, `color-dark.json` | Semantic colors per mode (`color.light.primary`, `color.dark.correct`). Both define the same names. |
| `typography.json` | Font family, weights and the 15 Material type styles (`typography.bodyLarge`). |
| `spacing.json` | 4-pt spacing scale: `none` 0, `xs` 4, `sm` 8, `md` 12, `lg` 16, `xl` 24, `xxl` 32, `xxxl` 48. |
| `radius.json` | Corner radii: `none` 0, `xs` 4, `sm` 8, `md` 12, `lg` 20, `xl` 28. |
| `elevation.json` | Elevation levels `level0`–`level5`: 0, 1, 3, 6, 8, 12. |
| `motion.json` | Durations: `short` 150 ms, `medium` 250 ms, `long` 400 ms. |

Dimensions are written in `px` and become `dp`/`sp` in Compose and points in SwiftUI. Type styles use a `px` line height
instead of the spec's unitless ratio so every platform gets the exact value. The only supported font family is
`system-ui` (the platform's system font); the build fails on any other family until fonts are bundled.

In Compose, `LanguagePlatformTheme { }` builds the Material 3 color scheme, typography and shapes from these tokens.
Colors Material has no slot for (`success`, `warning`, `correct`, `wrong`) and the spacing and radius scales are read
through `LanguagePlatformTheme.colors`, `.spacing` and `.radius` (backed by `LocalColors`, `LocalSpacing`, `LocalRadius`).

## Palette

A calm violet brand with a fresh teal and a warm rose accent, on slightly violet-tinted neutrals. Tones follow the
CIELAB lightness scale (tone 40 is darker than tone 90), so contrast is predictable across hues.

| Role | Hue | Light mode | Dark mode |
| --- | --- | --- | --- |
| Primary | violet | `#7543BA` | `#DBB8FF` |
| Secondary | teal | `#056A69` | `#6ED7D5` |
| Tertiary | rose | `#AC2B59` | `#FEB1C3` |
| Success / correct | green | `#016E2A` | `#7CDB89` |
| Warning | amber | `#895200` | `#FFB86C` |
| Error / wrong | red | `#BF0125` | `#FFB4AB` |
| Surface | neutral | `#FBF8FF` | `#151218` |
| On surface | neutral | `#1D1B20` | `#E4E1E9` |

## Accessibility

Every text/background pair meets WCAG AA (4.5:1). `npm run build` fails otherwise: `test/tokens.test.mjs` checks each
`on*` color against its container and every text color against every surface level in both modes. `outline` must reach
3:1 against the surface.

| Pair | Light | Dark |
| --- | --- | --- |
| `onPrimary` on `primary` | 6.4:1 | 7.7:1 |
| `onPrimaryContainer` on `primaryContainer` | 13.3:1 | 7.3:1 |
| `onSecondary` on `secondary` | 6.4:1 | 7.7:1 |
| `onTertiary` on `tertiary` | 6.5:1 | 7.7:1 |
| `onError` on `error` | 6.5:1 | 7.7:1 |
| `onSuccess` on `success` | 6.4:1 | 7.7:1 |
| `onWarning` on `warning` | 6.4:1 | 7.7:1 |
| `onSurface` on `surface` | 16.2:1 | 14.4:1 |
| `onSurfaceVariant` on `surface` | 8.9:1 | 10.9:1 |
| `primary` on `surface` | 6.1:1 | 10.9:1 |
| `correct` on `surface` | 6.1:1 | 10.9:1 |
| `wrong` on `surface` | 6.2:1 | 10.9:1 |
| `warning` on `surface` | 6.1:1 | 10.9:1 |
| `inverseOnSurface` on `inverseSurface` | 11.6:1 | 10.2:1 |
| `outline` on `surface` | 4.2:1 | 5.9:1 |

## Components

No `Lp…` components exist yet. Each new component is listed here with its name, purpose and states.
