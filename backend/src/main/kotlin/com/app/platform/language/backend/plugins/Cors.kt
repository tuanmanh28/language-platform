package com.app.platform.language.backend.plugins

import com.app.platform.language.backend.config.AllowedOrigins
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS

fun Application.configureCors(allowedOrigins: AllowedOrigins) {
  install(CORS) {
    when (allowedOrigins) {
      AllowedOrigins.All -> anyHost()
      is AllowedOrigins.Only -> allowOrigins { it in allowedOrigins.origins }
    }
    allowMethod(HttpMethod.Options)
    allowMethod(HttpMethod.Post)
    allowHeader(HttpHeaders.ContentType)
    allowHeader(HttpHeaders.Authorization)
    allowHeader(HttpHeaders.XRequestId)
    exposeHeader(HttpHeaders.XRequestId)
  }
}
