package com.app.platform.language.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import co.touchlab.kermit.Logger as KermitLogger

internal fun createHttpClient(
  engine: HttpClientEngine,
  json: Json,
  config: ApiConfig,
): HttpClient =
  HttpClient(engine) {
    expectSuccess = true
    install(DefaultRequest) {
      url("${config.baseUrl.trimEnd('/')}/api/v1/")
    }
    install(ContentNegotiation) {
      json(json)
    }
    // Short timeouts so the apps fall back to offline content quickly.
    install(HttpTimeout) {
      connectTimeoutMillis = 5_000
      requestTimeoutMillis = 15_000
    }
    if (config.enableNetworkLogs) {
      install(Logging) {
        level = LogLevel.INFO
        logger =
          object : Logger {
            override fun log(message: String) {
              KermitLogger.withTag("Http").d { message }
            }
          }
      }
    }
  }
