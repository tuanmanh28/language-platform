package com.app.platform.language.backend.listening

import com.app.platform.language.backend.media.PrivateMediaService
import com.app.platform.language.backend.media.respondPrivateMedia
import io.ktor.server.application.install
import io.ktor.server.auth.principal
import io.ktor.server.plugins.partialcontent.PartialContent
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail

const val LOCAL_AUDIO_PATH = "/api/v1/listening/audio"

fun Route.listeningAudioRoutes(audio: PrivateMediaService) {
  route(LOCAL_AUDIO_PATH) {
    install(PartialContent)

    get("/{testId}/{fileName}") {
      val file =
        audio.file(
          call.principal(),
          call.parameters.getOrFail("testId"),
          call.parameters.getOrFail("fileName"),
        )
      call.respondPrivateMedia(file, notFoundMessage = "Audio not found")
    }
  }
}
