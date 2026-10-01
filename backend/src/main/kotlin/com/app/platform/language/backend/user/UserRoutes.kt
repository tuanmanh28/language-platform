package com.app.platform.language.backend.user

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.userRoutes() {
  get("/api/v1/me") {
    call.respond(call.currentUser().toProfile())
  }
}
