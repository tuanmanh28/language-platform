package com.app.platform.language.backend.media

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapBoth
import io.ktor.http.HttpHeaders
import io.ktor.server.application.log
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondPath
import io.ktor.server.routing.RoutingCall
import java.nio.file.Path

private const val CACHE_CONTROL = "private, max-age=3600"

suspend fun RoutingCall.respondPrivateMedia(
  file: Result<Path, MediaError>,
  notFoundMessage: String,
) {
  file.mapBoth(
    success = { path ->
      response.header(HttpHeaders.CacheControl, CACHE_CONTROL)
      respondPath(path)
    },
    failure = { error ->
      if (error is MediaError.Unexpected) application.log.error("Private media request failed", error.cause)
      val (status, body) = error.toHttp(notFoundMessage)
      respond(status, body)
    },
  )
}
