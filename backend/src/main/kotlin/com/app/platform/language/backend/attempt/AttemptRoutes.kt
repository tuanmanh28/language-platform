package com.app.platform.language.backend.attempt

import com.app.platform.language.backend.user.currentUser
import com.app.platform.language.core.model.SubmitAttemptRequest
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.mapBoth
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.server.application.log
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlin.time.Instant

fun Route.attemptRoutes(service: AttemptService) {
  route("/api/v1/attempts") {
    post {
      val request = call.receive<SubmitAttemptRequest>()
      service.submit(call.currentUser().id, call.principal(), request).mapBoth(
        success = { submitted ->
          call.respond(if (submitted.isNew) HttpStatusCode.Created else HttpStatusCode.OK, submitted.attempt)
        },
        failure = { error -> call.respondError(error) },
      )
    }

    get {
      call.request.queryParameters
        .toAttemptQuery()
        .andThen { query -> service.listAttempts(call.currentUser().id, query) }
        .mapBoth(
          success = { page -> call.respond(page) },
          failure = { error -> call.respondError(error) },
        )
    }
  }
}

private fun Parameters.toAttemptQuery(): Result<AttemptQuery, AttemptError> {
  val since = this["since"]
  val cursor = this["cursor"]
  val limit = this["limit"]
  val parsedSince = since?.let(Instant::parseOrNull)
  val parsedCursor = cursor?.let(AttemptCursor::decodeOrNull)
  val parsedLimit = limit?.toIntOrNull()
  return when {
    since != null && parsedSince == null -> Err(AttemptError.InvalidRequest("since must be an ISO-8601 timestamp"))
    cursor != null && parsedCursor == null -> Err(AttemptError.InvalidRequest("cursor is invalid"))
    limit != null && parsedLimit == null -> Err(AttemptError.InvalidRequest("limit must be a number"))
    else -> Ok(AttemptQuery(parsedSince, parsedCursor, parsedLimit ?: AttemptQuery.DEFAULT_LIMIT))
  }
}

private suspend fun RoutingCall.respondError(error: AttemptError) {
  if (error is AttemptError.Unexpected) application.log.error("Attempt request failed", error.cause)
  val (status, body) = error.toHttp()
  respond(status, body)
}
