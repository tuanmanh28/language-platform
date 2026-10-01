---
name: swiftui-interop
description: Use when exposing shared Kotlin code to Swift, writing SwiftUI views that use shared ViewModels, or changing the Xcode project (XcodeGen project.yml) for iOS and macOS.
---

# Kotlin ↔ SwiftUI (SKIE)

Swift sees the `Shared` framework produced by `:shared`. SKIE turns sealed classes into Swift enums (`onEnum(of:)`),
`StateFlow` into `AsyncSequence`, and `suspend` into `async`.

References:
- `_reference/KaMPKit/ios/KaMPKitiOS/BreedListScreen.swift` — `.task`, `for await`, `withTaskCancellationHandler`, `clear()`.
- `_reference/KaMPKit/shared/src/iosMain/kotlin/co/touchlab/kampkit/KoinIOS.kt` and `ios/KaMPKitiOS/Koin.swift` — Koin entry points for Swift.
- `_reference/PeopleInSpace/PeopleInSpaceSwiftUI/` — SKIE `Observing`.

## What Swift may see

| Allowed | Not allowed (wrap or hide it) |
| --- | --- |
| Data classes, sealed UI states, enums | `Result` / value classes (exported as `Any?`) |
| `StateFlow` of a UI state | `Flow` of domain models, `StateFlow<Result<…>>` |
| Plain functions and `suspend` functions | Default arguments, Kotlin lambdas with receivers |
| `Int` (becomes `Int32`), `Long` (`Int64`), `String`, `List` | Generic helper types from libraries |

Keep everything Swift does not need `internal`.

## ViewModel lifetime

The ViewModel must be cleared when the view goes away. Shared code exposes an owner:

```kotlin
class ViewModelOwner<VM : ViewModel> internal constructor(create: () -> VM, type: KClass<VM>) {
  private val store = ViewModelStore()
  val viewModel: VM = ViewModelProvider.create(store, viewModelFactory { initializer { create() } })[type]

  fun clear() = store.clear()
}

object ViewModels : KoinComponent {
  fun readingSession(testId: String) =
    ViewModelOwner({ get<ReadingSessionViewModel> { parametersOf(testId) } }, ReadingSessionViewModel::class)
}
```

Verify `ViewModelProvider.create` / `viewModelFactory` against the current androidx lifecycle API before using it.

## SwiftUI view pattern

```swift
struct ReadingSessionView: View {
  let testId: String
  @State private var owner: ViewModelOwner<ReadingSessionViewModel>?
  @State private var state: ReadingSessionUiState = ReadingSessionUiState.Loading.shared

  var body: some View {
    ReadingSessionContent(state: state, actions: owner?.viewModel)
      .task {
        let owner = ViewModels.shared.readingSession(testId: testId)
        self.owner = owner
        await withTaskCancellationHandler {
          for await value in owner.viewModel.state { state = value }
        } onCancel: {
          owner.clear()
        }
      }
  }
}
```

- One blank line between sibling views and modifiers blocks at the same level (`code-conventions`).
- The content view takes plain values and closures, so it has `#Preview`s without Kotlin.
- Render states with `switch onEnum(of: state)`; every case handled, no `default`.
- Use design-system components from `app-apple/Sources/DesignSystem/`.
- Platform differences go behind `#if os(iOS)` / `#if os(macOS)` in the smallest possible scope.

## Project file

`app-apple/project.yml` is the source of truth (XcodeGen). New folders under `Sources/` are picked up automatically; new
targets, entitlements, Info.plist keys or build settings are edited there, never in a generated `.xcodeproj`.

## Checklist

- [ ] No `Result`, value classes or default arguments in Swift-facing API.
- [ ] Every ViewModel obtained from Swift is cleared on disappear/cancellation.
- [ ] `xcodegen && xcodebuild -scheme LanguagePlatformiOS -sdk iphonesimulator build` and the macOS scheme build.
- [ ] Previews exist for each state of the content view.
