package com.app.platform.language.core.model

sealed interface ListeningError {
  data object NotFound : ListeningError

  data object Offline : ListeningError

  data class Unexpected(
    val cause: Throwable,
  ) : ListeningError
}
