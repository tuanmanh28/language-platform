package com.app.platform.language.backend.audio

import com.github.michaelbull.result.mapBoth
import io.ktor.http.HttpHeaders
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.principal
import io.ktor.server.plugins.partialcontent.PartialContent
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondPath
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail

const val LOCAL_AUDIO_PATH = "/api/v1/listening/audio"

private const val CACHE_CONTROL = "private, max-age=3600"

fun Route.audioRoutes(service: AudioService) {
  route(LOCAL_AUDIO_PATH) {
    install(PartialContent)

    get("/{testId}/{fileName}") {
      service
        .audioFile(call.principal(), call.parameters.getOrFail("testId"), call.parameters.getOrFail("fileName"))
        .mapBoth(
          success = { file ->
            call.response.header(HttpHeaders.CacheControl, CACHE_CONTROL)
            call.respondPath(file)
          },
          failure = { error -> call.respondError(error) },
        )
    }
  }
}

private suspend fun RoutingCall.respondError(error: AudioError) {
  if (error is AudioError.Unexpected) application.log.error("Audio request failed", error.cause)
  val (status, body) = error.toHttp()
  respond(status, body)
}
