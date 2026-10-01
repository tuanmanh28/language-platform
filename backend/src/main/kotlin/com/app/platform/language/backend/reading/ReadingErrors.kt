package com.app.platform.language.backend.reading

import com.app.platform.language.core.model.ApiError
import com.app.platform.language.core.model.ReadingError
import io.ktor.http.HttpStatusCode

fun ReadingError.toHttp(): Pair<HttpStatusCode, ApiError> =
  when (this) {
    ReadingError.NotFound -> HttpStatusCode.NotFound to ApiError("Reading test not found")
    ReadingError.Offline, is ReadingError.Unexpected -> HttpStatusCode.InternalServerError to ApiError("Internal error")
  }
