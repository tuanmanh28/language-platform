package com.app.platform.language.backend.plugins

import com.app.platform.language.core.model.ApiError
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

fun Application.configureStatusPages() {
  install(StatusPages) {
    // Ktor throws this for malformed parameters or bodies, before any route code runs.
    exception<BadRequestException> { call, _ ->
      call.respond(HttpStatusCode.BadRequest, ApiError("Bad request"))
    }
    exception<Throwable> { call, cause ->
      call.application.log.error("Unhandled error", cause)
      call.respond(HttpStatusCode.InternalServerError, ApiError("Internal server error"))
    }
  }
}
