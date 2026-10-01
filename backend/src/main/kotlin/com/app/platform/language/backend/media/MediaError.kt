package com.app.platform.language.backend.media

import com.app.platform.language.core.model.ApiError
import io.ktor.http.HttpStatusCode

sealed interface MediaError {
  data object NotFound : MediaError

  data class Unexpected(
    val cause: Throwable,
  ) : MediaError
}

fun MediaError.toHttp(notFoundMessage: String): Pair<HttpStatusCode, ApiError> =
  when (this) {
    MediaError.NotFound -> HttpStatusCode.NotFound to ApiError(notFoundMessage)
    is MediaError.Unexpected -> HttpStatusCode.InternalServerError to ApiError("Internal error")
  }
