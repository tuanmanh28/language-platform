package com.app.platform.language.backend.reading

import com.app.platform.language.core.model.ReadingError
import com.app.platform.language.core.model.SubmitAnswersRequest
import com.github.michaelbull.result.mapBoth
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail

// Clients may store responses but must revalidate with the ETag, so newly seeded content shows up at once.
private const val CACHE_CONTROL = "public, no-cache"

fun Route.readingRoutes(service: ReadingService) {
  route("/api/v1/reading/tests") {
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

private suspend inline fun <reified T : Any> RoutingCall.respondVersioned(versioned: Versioned<T>) {
  val etag = "\"${versioned.version}\""
  response.header(HttpHeaders.ETag, etag)
  response.header(HttpHeaders.CacheControl, CACHE_CONTROL)
  if (request.hasMatchingEtag(etag)) respond(HttpStatusCode.NotModified) else respond(versioned.value)
}

private fun ApplicationRequest.hasMatchingEtag(etag: String): Boolean =
  headers
    .getAll(HttpHeaders.IfNoneMatch)
    .orEmpty()
    .flatMap { it.split(',') }
    .map { it.trim().removePrefix("W/") }
    .any { it == etag || it == "*" }

private suspend fun RoutingCall.respondError(error: ReadingError) {
  if (error is ReadingError.Unexpected) application.log.error("Reading request failed", error.cause)
  val (status, body) = error.toHttp()
  respond(status, body)
}
