package com.app.platform.language.backend.plugins

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS

fun Application.configureCors() {
  install(CORS) {
    // Any origin until the production web domain exists.
    anyHost()
    allowMethod(HttpMethod.Options)
    allowMethod(HttpMethod.Post)
    allowHeader(HttpHeaders.ContentType)
    allowHeader(HttpHeaders.Authorization)
  }
}
