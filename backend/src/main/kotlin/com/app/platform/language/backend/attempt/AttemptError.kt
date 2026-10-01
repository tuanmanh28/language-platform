package com.app.platform.language.backend.attempt

import com.app.platform.language.core.model.ApiError
import io.ktor.http.HttpStatusCode

sealed interface AttemptError {
  data class InvalidRequest(
    val reason: String,
  ) : AttemptError

  data object TestNotFound : AttemptError

  data class Unexpected(
    val cause: Throwable,
  ) : AttemptError
}

fun AttemptError.toHttp(): Pair<HttpStatusCode, ApiError> =
  when (this) {
    is AttemptError.InvalidRequest -> HttpStatusCode.BadRequest to ApiError(reason)
    AttemptError.TestNotFound -> HttpStatusCode.NotFound to ApiError("Reading test not found")
    is AttemptError.Unexpected -> HttpStatusCode.InternalServerError to ApiError("Internal error")
  }
