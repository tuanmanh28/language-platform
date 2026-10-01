---
name: error-handling
description: Use when writing code that can fail (network, database, parsing, business rules), when designing error types, or when turning failures into UI state or HTTP responses. Covers kotlin-result usage in this project.
---

# Error handling with kotlin-result

Expected failures are values, not exceptions. Exceptions are only for bugs.

Library: `com.michael-bull.kotlin-result:kotlin-result` and `kotlin-result-coroutines` (exposed as `api` from `core/model`).
Reference: `_reference/kotlin-result/README.md`, sources in `_reference/kotlin-result/kotlin-result/src/commonMain/`,
`runSuspendCatching` and `coroutineBinding` in `_reference/kotlin-result/kotlin-result-coroutines/`, a Ktor service example in
`_reference/kotlin-result/example/`.

## Rules

1. Every fallible function returns `Result<V, E>` where `E` is a feature-specific `sealed interface`.
2. Convert exceptions to `Err` at the boundary where they happen (API client, database, parser). Never let a raw exception travel upward.
3. In suspend code use `runSuspendCatching`, never `runCatching` (it swallows `CancellationException`).
4. Never read `.value` / `.error` (they need an opt-in for a reason). Use `mapBoth`, `getOrElse`, `onOk`, `onErr`.
5. Never expose `Result` to Swift: not from public ViewModel members, not inside `StateFlow`. Fold it into a UI state first.
6. Never return `Result<V, Throwable>` from a repository or service. Map to the feature error type.
7. Never ignore a returned `Result` (the library marks it `@MustUseReturnValue`).

## Modelling errors

One sealed interface per feature, in the feature package, with only the cases callers act on differently.

```kotlin
sealed interface ReadingError {
    data object NotFound : ReadingError
    data object Offline : ReadingError
    data class Unexpected(val cause: Throwable) : ReadingError
}
```

Map raw failures in one place:

```kotlin
internal fun Throwable.toReadingError(): ReadingError = when (this) {
    is ClientRequestException -> if (response.status == HttpStatusCode.NotFound) ReadingError.NotFound else ReadingError.Unexpected(this)
    is IOException -> ReadingError.Offline
    else -> ReadingError.Unexpected(this)
}
```

## Composing

| Need | Use |
| --- | --- |
| Transform success | `map` |
| Transform error | `mapError` |
| Next step that can fail | `andThen` |
| Fallback value or step | `recover`, `getOrElse`, `andThenRecover` |
| Several steps, short-circuit on first error | `binding { a.bind(); b.bind() }` |
| Several suspend steps, run in parallel | `coroutineBinding { val a = async { … }; … }` |
| Nullable to Result | `toResultOr { ReadingError.NotFound }` |
| All of a list | `combine()` / `zipOrAccumulate` to collect every error |
| Side effects (logging) | `onOk`, `onErr` (`onSuccess`/`onFailure` are deprecated) |

## At the edges

UI (inside a ViewModel):

```kotlin
_state.value = repository.getTest(testId).mapBoth(
    success = { test -> ReadingSessionUiState.InProgress(test) },
    failure = { error -> ReadingSessionUiState.Error(error.toUserMessage()) },
)
```

HTTP (backend): one extension per error type, used by every route of that feature.

```kotlin
fun ReadingError.toHttp(): Pair<HttpStatusCode, ApiError> = when (this) {
    ReadingError.NotFound -> HttpStatusCode.NotFound to ApiError("Reading test not found")
    ReadingError.Offline, is ReadingError.Unexpected -> HttpStatusCode.InternalServerError to ApiError("Internal error")
}
```

User-facing messages live in one `toUserMessage()` per error type, next to the UI strings.

## Tests

Test both branches of every public function that returns `Result`. Assert on the error case explicitly:

```kotlin
assertEquals(Err(ReadingError.NotFound), repository.getTest("missing"))
```

## Checklist

- [ ] New error cases are added to the sealed interface, not encoded as strings.
- [ ] No `runCatching` in suspend code, no `.value` / `.error`, no ignored `Result`.
- [ ] No `Result` in a `StateFlow` or in any API Swift can see.
- [ ] Every error branch has a test.
