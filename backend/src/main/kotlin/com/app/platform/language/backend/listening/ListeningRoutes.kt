package com.app.platform.language.backend.listening

import com.app.platform.language.backend.common.respondVersioned
import com.app.platform.language.core.model.ListeningError
import com.app.platform.language.core.model.SubmitAnswersRequest
import com.github.michaelbull.result.mapBoth
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail

fun Route.listeningRoutes(service: ListeningService) {
  route("/api/v1/listening/tests") {
    get {
      service.listTests().mapBoth(
        success = { tests -> call.respondVersioned(tests) },
        failure = { error -> call.respondError(error) },
      )
    }

    get("/{id}") {
      service.getTest(call.parameters.getOrFail("id")).mapBoth(
        success = { test -> call.respondVersioned(test) },
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

private suspend fun RoutingCall.respondError(error: ListeningError) {
  if (error is ListeningError.Unexpected) application.log.error("Listening request failed", error.cause)
  val (status, body) = error.toHttp()
  respond(status, body)
}
