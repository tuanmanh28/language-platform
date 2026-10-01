package com.app.platform.language.backend.common

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingCall

// Clients may store responses but must revalidate with the ETag, so newly seeded content shows up at once.
@PublishedApi
internal const val CACHE_CONTROL = "public, no-cache"

suspend inline fun <reified T : Any> RoutingCall.respondVersioned(versioned: Versioned<T>) {
  val etag = "\"${versioned.version}\""
  response.header(HttpHeaders.ETag, etag)
  response.header(HttpHeaders.CacheControl, CACHE_CONTROL)
  if (request.hasMatchingEtag(etag)) respond(HttpStatusCode.NotModified) else respond(versioned.value)
}

@PublishedApi
internal fun ApplicationRequest.hasMatchingEtag(etag: String): Boolean =
  headers
    .getAll(HttpHeaders.IfNoneMatch)
    .orEmpty()
    .flatMap { it.split(',') }
    .map { it.trim().removePrefix("W/") }
    .any { it == etag || it == "*" }
