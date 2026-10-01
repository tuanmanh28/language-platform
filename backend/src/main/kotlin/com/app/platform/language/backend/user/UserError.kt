package com.app.platform.language.backend.user

import com.app.platform.language.core.model.ApiError
import io.ktor.http.HttpStatusCode

sealed interface UserError {
  data class Unexpected(
    val cause: Throwable,
  ) : UserError
}

fun UserError.toHttp(): Pair<HttpStatusCode, ApiError> =
  when (this) {
    is UserError.Unexpected -> HttpStatusCode.InternalServerError to ApiError("Internal error")
  }
