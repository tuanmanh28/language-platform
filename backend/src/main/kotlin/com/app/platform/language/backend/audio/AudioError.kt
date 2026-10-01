package com.app.platform.language.backend.audio

import com.app.platform.language.core.model.ApiError
import io.ktor.http.HttpStatusCode

sealed interface AudioError {
  data object NotFound : AudioError

  data class Unexpected(
    val cause: Throwable,
  ) : AudioError
}

fun AudioError.toHttp(): Pair<HttpStatusCode, ApiError> =
  when (this) {
    AudioError.NotFound -> HttpStatusCode.NotFound to ApiError("Audio not found")
    is AudioError.Unexpected -> HttpStatusCode.InternalServerError to ApiError("Internal error")
  }
