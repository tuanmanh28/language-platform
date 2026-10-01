package com.app.platform.language.backend.writing

import com.app.platform.language.core.model.ApiError
import io.ktor.http.HttpStatusCode

sealed interface WritingError {
  data class InvalidRequest(
    val reason: String,
  ) : WritingError

  data object PromptNotFound : WritingError

  data object SubmissionNotFound : WritingError

  data class Unexpected(
    val cause: Throwable,
  ) : WritingError
}

fun WritingError.toHttp(): Pair<HttpStatusCode, ApiError> =
  when (this) {
    is WritingError.InvalidRequest -> HttpStatusCode.BadRequest to ApiError(reason)
    WritingError.PromptNotFound -> HttpStatusCode.NotFound to ApiError("Writing prompt not found")
    WritingError.SubmissionNotFound -> HttpStatusCode.NotFound to ApiError("Writing submission not found")
    is WritingError.Unexpected -> HttpStatusCode.InternalServerError to ApiError("Internal error")
  }
