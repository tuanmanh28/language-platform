---
name: testing
description: Use when writing, fixing or reviewing tests anywhere in the repo - ViewModels, repositories, API clients, pure logic, Ktor routes, database queries or UI.
---

# Testing

Tests are fast, deterministic and read like a specification. Use real implementations where cheap, hand-written fakes
otherwise. Never mock our own classes.

References:
- `_reference/nowinandroid/core/testing/src/main/kotlin/com/google/samples/apps/nowinandroid/core/testing/repository/` — fake repositories with test hooks.
- `_reference/nowinandroid/feature/foryou/impl/src/test/` — ViewModel tests collecting `state` in `backgroundScope`.
- `_reference/KaMPKit/shared/src/commonTest/kotlin/` — Turbine, Ktor `MockEngine`, in-memory DB.
- `_reference/kotlinconf-app/backend/src/test/kotlin/ApiTest.kt` — `testApplication` with config.

## What to use where

| Code | Test type | Tools | Location |
| --- | --- | --- | --- |
| Pure logic (`core/*`) | Unit, exhaustive | `kotlin.test` | `commonTest` |
| ViewModel | Unit with fakes | `kotlinx-coroutines-test`, Turbine, `Dispatchers.setMain` | `shared/src/commonTest` |
| Repository | Unit with fake API + in-memory DB | `JdbcSqliteDriver(IN_MEMORY)` | `shared/src/jvmTest` |
| API client | Unit | Ktor `MockEngine` | `shared/src/commonTest` |
| Backend route | Integration | `testApplication`, fake services or real ones with test DB | `backend/src/test` |
| Backend repository | Integration | Testcontainers Postgres + Flyway | `backend/src/test` |
| Compose UI | Previews; UI tests for critical flows | `runComposeUiTest` (desktop/JVM) | `ui-compose/src/jvmTest` |

## Fakes

- Live next to the tests that use them (`<module>/src/commonTest/.../fake/`), named `Fake<Interface>`.
- Backed by `MutableStateFlow`/lists, with explicit hooks: `fun emit(items: List<T>)`, `var nextError: E? = null`.
- Shared between modules only when two modules need them.

## ViewModel test shape

```kotlin
class VocabularyViewModelTest {
  private val repository = FakeVocabularyRepository()
  private val dispatcher = StandardTestDispatcher()

  @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
  @AfterTest fun tearDown() = Dispatchers.resetMain()

  @Test
  fun dueCardsAreShownWhenRepositoryEmits() = runTest(dispatcher) {
    val viewModel = VocabularyViewModel(repository)
    viewModel.state.test {
      assertEquals(VocabularyUiState.Loading, awaitItem())
      repository.emit(listOf(card))
      assertEquals(VocabularyUiState.Ready(listOf(card), 1), awaitItem())
    }
  }
}
```

## Rules

- Test names describe behaviour: `offlineRefreshKeepsCachedTests`, not `test1`.
- One behaviour per test; arrange-act-assert visible at a glance.
- No real time: inject `Clock`, use the test dispatcher's virtual time; no `Thread.sleep` or real `delay`.
- Every `Result`-returning function: test `Ok` and each `Err` case.
- A bug fix starts with a failing test that reproduces it.
- Tests needing Docker or network skip with a clear message when unavailable.

## Checklist

- [ ] New logic has tests at the right level from the table.
- [ ] Error and empty states covered, not only the happy path.
- [ ] Suite stays fast; no flaky timing.
