package com.app.platform.language.backend.plugins

import com.app.platform.language.core.model.ContentJson
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation

fun Application.configureSerialization() {
  install(ContentNegotiation) {
    json(ContentJson)
  }
}
