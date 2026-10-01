package com.app.platform.language.backend

import com.app.platform.language.core.model.ContentJson
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

fun main() {
  val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
  embeddedServer(Netty, port = port, host = "0.0.0.0") {
    module()
  }.start(wait = true)
}

@Serializable
data class ApiError(
  val message: String,
)

@Serializable
data class HealthResponse(
  val status: String,
)

fun Application.module(contentStore: ContentStore = BundledContentStore()) {
  install(ContentNegotiation) {
    json(ContentJson)
  }
  install(CallLogging)
  install(CORS) {
    // TODO: restrict origins once the production web domain exists
    anyHost()
    allowMethod(HttpMethod.Options)
    allowMethod(HttpMethod.Post)
    allowHeader(HttpHeaders.ContentType)
    allowHeader(HttpHeaders.Authorization)
  }
  install(StatusPages) {
    exception<NotFoundException> { call, cause ->
      call.respond(HttpStatusCode.NotFound, ApiError(cause.message ?: "Not found"))
    }
    exception<BadRequestException> { call, cause ->
      call.respond(HttpStatusCode.BadRequest, ApiError(cause.message ?: "Bad request"))
    }
    exception<Throwable> { call, cause ->
      call.application.log.error("Unhandled error", cause)
      call.respond(HttpStatusCode.InternalServerError, ApiError("Internal server error"))
    }
  }

  routing {
    get("/health") {
      call.respond(HealthResponse(status = "ok"))
    }
    readingRoutes(contentStore)
  }
}
