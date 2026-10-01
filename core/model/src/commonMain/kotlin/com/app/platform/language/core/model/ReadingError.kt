package com.app.platform.language.core.model

sealed interface ReadingError {
  data object NotFound : ReadingError

  data object Offline : ReadingError

  data class Unexpected(
    val cause: Throwable,
  ) : ReadingError
}
