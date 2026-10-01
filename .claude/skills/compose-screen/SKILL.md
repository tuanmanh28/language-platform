---
name: compose-screen
description: Use when building or changing a Compose Multiplatform screen for Android or Desktop - layout, state collection, previews, adaptive layouts and accessibility.
---

# Compose screens (Android + Desktop)

Screens are thin: they render a `UiState` and forward user actions to the ViewModel.

References:
- `_reference/nowinandroid/feature/foryou/impl/src/main/kotlin/com/google/samples/apps/nowinandroid/feature/foryou/impl/ForYouScreen.kt` — stateful/stateless split, previews.
- `_reference/kotlinconf-app/app/shared/src/commonMain/kotlin/org/jetbrains/kotlinconf/screens/ScheduleScreen.kt` and `.../utils/ErrorLoadingContent.kt` — KMP screen, loading/error handling.

## Structure of a screen file

```kotlin
@Composable
fun VocabularyScreen(
  onOpenWord: (String) -> Unit,
  viewModel: VocabularyViewModel = koinViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  VocabularyScreen(state = state, onRate = viewModel::rate, onOpenWord = onOpenWord)
}

@Composable
internal fun VocabularyScreen(
  state: VocabularyUiState,
  onRate: (Card, Rating) -> Unit,
  onOpenWord: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  when (state) {
    VocabularyUiState.Loading -> LpLoading(modifier)
    is VocabularyUiState.Failed -> LpErrorState(state.message, modifier = modifier)
    is VocabularyUiState.Ready -> ReadyContent(state, onRate, onOpenWord, modifier)
  }
}
```

- The public overload takes navigation callbacks and the ViewModel; the `internal` overload takes only state and lambdas
  and is what previews and UI tests use.
- Split big bodies into private composables named after what they show (`ReadyContent`, `PassagePane`), not `Content1`.
- Lambdas are named `on<Action>`; `modifier` is the first optional parameter and applied to the root only.
- No ViewModel, repository or Koin access below the stateful overload.

## Rules

- Only design-system components and theme tokens (`LpTheme.colors`, `LpTheme.spacing`, …). No raw colors, `dp` literals
  outside the design system, or hard-coded typography. See the `design-system` skill.
- User-facing text comes from Compose resources (`Res.string.*`), Vietnamese first. No string literals in UI code.
- Adaptive layout: decide with window size class or `BoxWithConstraints` once at the screen level (one pane vs two panes);
  components stay size-agnostic.
- Accessibility: every icon-only control has a content description; touch targets ≥ 48dp; the screen works at 200 % font
  scale; selection state is exposed through semantics, not color alone.
- Side effects use `LaunchedEffect` keyed on what they depend on; one-off messages are cleared by calling the ViewModel back.
- Remember expensive derived values with `remember(key)`; never compute in composition what the ViewModel can provide.

## Previews

One `@Preview` per meaningful state (loading, ready, empty, error), light and dark, built from fake state objects in a
`<Feature>PreviewData.kt` file. Previews never touch Koin.

## Checklist

- [ ] Stateful/stateless split; previews for each state.
- [ ] No hard-coded colors, sizes or strings.
- [ ] Works on a phone and on a wide desktop window.
- [ ] `./gradlew :app-android:assembleDebug :app-desktop:compileKotlin` passes.
