---
name: kmp-feature
description: Use when adding a new user-facing feature or screen end to end - model, repository, ViewModel, Compose screen, SwiftUI screen and tests - or when restructuring an existing feature to the standard layout.
---

# Adding a feature

A feature is a vertical slice: data → ViewModel → UI on every platform → tests. Build it in this order and keep each layer
boring and small.

References (read before starting):
- `_reference/nowinandroid/feature/foryou/impl/src/main/kotlin/com/google/samples/apps/nowinandroid/feature/foryou/impl/` — screen/ViewModel split, `UiState`.
- `_reference/kotlinconf-app/app/shared/src/commonMain/kotlin/org/jetbrains/kotlinconf/screens/ScheduleViewModel.kt` — KMP ViewModel with `stateIn`.
- Existing Reading feature in this repo (`shared/.../reading/`, `ui-compose/.../reading/`, `app-apple/Sources/`).

## Where things go

| Layer | Location | Notes |
| --- | --- | --- |
| Models shared with backend | `core/model/.../model/` | `@Serializable` data classes only |
| Pure domain logic | `core/<area>/` (e.g. `core/exam-engine`) | No I/O, fully unit tested |
| Repository, API, DB | `shared/src/commonMain/kotlin/com/app/platform/language/shared/<feature>/data/` | See `shared-data-layer` skill |
| UiState + ViewModel | `shared/.../<feature>/` | One ViewModel per screen |
| DI | `shared/.../di/Koin.kt` | Register repository and ViewModel |
| Compose UI | `ui-compose/.../ui/<feature>/` | See `compose-screen` skill |
| Navigation | `ui-compose/.../ui/navigation/` | See `navigation` skill |
| SwiftUI | `app-apple/Sources/<Feature>/` | See `swiftui-interop` skill |

Keep `shared` a single module; split by package per feature.

## Steps

1. **Model** the data in `core/model` if the backend shares it; otherwise keep it in the feature package.
2. **Errors:** add `sealed interface <Feature>Error` (see `error-handling`).
3. **Repository:** interface + `OfflineFirst<Feature>Repository` implementation. Reads are `Flow`, writes are `suspend` returning `Result`.
4. **UiState:** a sealed interface with only the states the screen really shows.

   ```kotlin
   sealed interface VocabularyUiState {
       data object Loading : VocabularyUiState
       data class Ready(val dueCards: List<Card>, val totalWords: Int) : VocabularyUiState
       data class Failed(val message: String) : VocabularyUiState
   }
   ```

5. **ViewModel:** derive state from repository flows; expose one `StateFlow`; user actions are plain functions.

   ```kotlin
   class VocabularyViewModel(
       private val repository: VocabularyRepository,
   ) : ViewModel() {

       private val _message = MutableStateFlow<String?>(null)
       val message: StateFlow<String?> = _message.asStateFlow()

       val state: StateFlow<VocabularyUiState> = repository.observeDueCards()
           .map<List<Card>, VocabularyUiState> { cards -> VocabularyUiState.Ready(cards, cards.size) }
           .catch { emit(VocabularyUiState.Failed(it.toUserMessage())) }
           .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VocabularyUiState.Loading)

       fun rate(card: Card, rating: Rating) {
           viewModelScope.launch {
               repository.review(card, rating).onErr { error -> _message.value = error.toUserMessage() }
           }
       }

       fun messageShown() {
           _message.value = null
       }
   }
   ```

   - No Android, Compose or Swift types in ViewModels.
   - Constructor parameters are interfaces; navigation arguments are plain ids.
   - Time comes from an injected `Clock`; dispatchers are not hard-coded in logic that needs testing.
6. **DI:** `viewModelOf(::VocabularyViewModel)` (or `viewModel { (id: String) -> … }` for arguments) in `Koin.kt`, plus a getter on
   `ViewModels` for Swift.
7. **Compose screen + route** (`compose-screen`, `navigation`).
8. **SwiftUI view** (`swiftui-interop`).
9. **Tests** (`testing`): ViewModel with a fake repository, repository with fakes of API/DB, pure logic exhaustively.

## Code style for this project

- Small functions with intention-revealing names; no comments that restate code. A comment only explains *why*, in English, one line.
- Prefer immutable data and pure functions; keep side effects at the edges.
- No premature abstraction: no base ViewModels, no generic "manager" classes, no use-case class unless it combines several repositories.
- Explicit types on public API; `internal` by default for anything not used outside the module.

## Checklist

- [ ] Logic lives in `shared`/`core`; UI only renders state and forwards actions.
- [ ] `./gradlew :shared:jvmTest :shared:compileKotlinIosSimulatorArm64` passes.
- [ ] Compose screen has previews for each state; SwiftUI view builds for iOS and macOS.
- [ ] ViewModel and repository tests cover success, empty and error states.
