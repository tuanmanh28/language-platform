package com.app.platform.language.backend.reading

import com.app.platform.language.core.model.ReadingError
import com.app.platform.language.core.model.SubmitAnswersRequest
import com.github.michaelbull.result.mapBoth
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail

fun Route.readingRoutes(service: ReadingService) {
  route("/api/v1/reading/tests") {
    get {
      call.respond(service.listTests())
    }

    get("/{id}") {
      service.getTest(call.parameters.getOrFail("id")).mapBoth(
        success = { test -> call.respond(test) },
        failure = { error -> call.respondError(error) },
      )
    }

    post("/{id}/submit") {
      val request = call.receive<SubmitAnswersRequest>()
      service.submit(call.parameters.getOrFail("id"), request).mapBoth(
        success = { result -> call.respond(result) },
        failure = { error -> call.respondError(error) },
      )
    }
  }
}

private suspend fun RoutingCall.respondError(error: ReadingError) {
  val (status, body) = error.toHttp()
  respond(status, body)
}
