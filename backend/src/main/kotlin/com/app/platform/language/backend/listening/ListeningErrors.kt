package com.app.platform.language.backend.listening

import com.app.platform.language.core.model.ApiError
import com.app.platform.language.core.model.ListeningError
import io.ktor.http.HttpStatusCode

private val internalError = HttpStatusCode.InternalServerError to ApiError("Internal error")

fun ListeningError.toHttp(): Pair<HttpStatusCode, ApiError> =
  when (this) {
    ListeningError.NotFound -> HttpStatusCode.NotFound to ApiError("Listening test not found")
    ListeningError.Offline, is ListeningError.Unexpected -> internalError
  }
