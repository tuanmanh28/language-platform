---
name: navigation
description: Use when adding a screen route, a top-level tab, a deep link, or changing back-stack behaviour in the Compose app (Navigation 3) or the SwiftUI app.
---

# Navigation (Navigation 3 + SwiftUI NavigationStack)

The app uses JetBrains' multiplatform **Navigation 3** (`org.jetbrains.androidx.navigation3:navigation3-ui`, latest stable).
The back stack is plain state that we own; screens never get a navigation controller.

References (copy the structure, adapt names):
- `_reference/kotlinconf-app/app/shared/src/commonMain/kotlin/org/jetbrains/kotlinconf/navigation/Routes.kt` — `@Serializable` route types.
- `.../navigation/NavState.kt` — per-tab back stacks saved with `rememberSerializable`, entry decorators
  (`rememberSaveableStateHolderNavEntryDecorator`, `rememberViewModelStoreNavEntryDecorator`).
- `.../navigation/Navigator.kt` — `add`, `goBack`, tab activation/reselection.
- `.../navigation/NavHost.kt` — `NavDisplay` + `entryProvider { entry<Route> { … } }`.
- `_reference/nowinandroid/feature/foryou/api/` and `feature/foryou/impl/.../navigation/ForYouEntryProvider.kt` — one entry provider per feature.

## Structure in this repo

```
ui-compose/.../ui/navigation/
├── Routes.kt          sealed AppRoute / TopLevelRoute, @Serializable, ids only as arguments
├── NavState.kt        back stacks per tab
├── Navigator.kt       the only API screens use to move
└── AppNavHost.kt      NavDisplay + entryProvider that delegates to feature entry functions
ui-compose/.../ui/<feature>/<Feature>Entries.kt
                       fun EntryProviderScope<AppRoute>.<feature>Entries(navigator: Navigator)
```

## Adding a screen

1. Add a route: `@Serializable @SerialName("ReadingSession") data class ReadingSessionRoute(val testId: String) : AppRoute`.
   Arguments are ids or small primitives, never objects.
2. Register it in the feature's entries function: `entry<ReadingSessionRoute> { route -> ReadingSessionScreen(route.testId, onBack = navigator::goBack) }`.
3. Pass navigation as lambdas to the screen (`onOpenTest = { navigator.add(ReadingSessionRoute(it)) }`).
4. ViewModels with arguments get them from the route via Koin parameters; each entry owns its ViewModels through the
   ViewModel-store decorator, so leaving a screen clears them.
5. Top-level tab: add to `TopLevelRoute`, the tab bar/rail items and `NavState`'s tab set.

## SwiftUI

Mirror routes as a Swift `enum Route: Hashable` inside the feature, use `NavigationStack(path:)` with
`.navigationDestination(for: Route.self)`. Tabs use `TabView` with one `NavigationStack` each.

## Checklist

- [ ] No `NavController`/navigator passed below the screen's stateful composable.
- [ ] Back works on Android (system back), desktop (toolbar) and iOS (swipe).
- [ ] Back stack survives configuration change and process death (routes are `@Serializable`).
- [ ] Leaving a screen clears its ViewModels.
