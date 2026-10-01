package com.app.platform.language.backend.writing

import com.app.platform.language.backend.user.currentUser
import com.app.platform.language.core.model.SubmitWritingRequest
import com.github.michaelbull.result.mapBoth
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail

fun Route.writingRoutes(service: WritingService) {
  route("/api/v1/writing") {
    get("/prompts") {
      service.listPrompts(call.principal()).mapBoth(
        success = { prompts -> call.respond(prompts) },
        failure = { error -> call.respondError(error) },
      )
    }

    route("/submissions") {
      post {
        val request = call.receive<SubmitWritingRequest>()
        service.submit(call.currentUser().id, call.principal(), request).mapBoth(
          success = { submission -> call.respond(HttpStatusCode.Created, submission) },
          failure = { error -> call.respondError(error) },
        )
      }

      get {
        service.listSubmissions(call.currentUser().id).mapBoth(
          success = { submissions -> call.respond(submissions) },
          failure = { error -> call.respondError(error) },
        )
      }

      get("/{id}") {
        service.getSubmission(call.currentUser().id, call.parameters.getOrFail("id")).mapBoth(
          success = { submission -> call.respond(submission) },
          failure = { error -> call.respondError(error) },
        )
      }
    }
  }
}

private suspend fun RoutingCall.respondError(error: WritingError) {
  if (error is WritingError.Unexpected) application.log.error("Writing request failed", error.cause)
  val (status, body) = error.toHttp()
  respond(status, body)
}
