package com.app.platform.language.backend.writing

import com.app.platform.language.backend.media.PrivateMediaService
import com.app.platform.language.backend.media.respondPrivateMedia
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.util.getOrFail

const val LOCAL_WRITING_IMAGE_PATH = "/api/v1/writing/images"

fun Route.writingImageRoutes(images: PrivateMediaService) {
  get("$LOCAL_WRITING_IMAGE_PATH/{promptId}/{fileName}") {
    val file =
      images.file(call.principal(), call.parameters.getOrFail("promptId"), call.parameters.getOrFail("fileName"))
    call.respondPrivateMedia(file, notFoundMessage = "Writing image not found")
  }
}
