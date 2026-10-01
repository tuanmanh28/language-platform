package com.app.platform.language.backend.auth

sealed interface TokenError {
  data class Invalid(
    val reason: String,
  ) : TokenError

  data class Unverifiable(
    val cause: Throwable,
  ) : TokenError
}
