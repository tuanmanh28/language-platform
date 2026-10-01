package com.app.platform.language.shared.network

import com.app.platform.language.core.model.ReadingResult
import com.app.platform.language.core.model.ReadingTest
import com.app.platform.language.core.model.ReadingTestSummary
import com.app.platform.language.core.model.SubmitAnswersRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** [baseUrl] without a trailing "/", e.g. "http://localhost:8080". */
data class ApiConfig(
  val baseUrl: String,
  val enableNetworkLogs: Boolean = false,
)

interface ReadingApi {
  suspend fun listTests(): List<ReadingTestSummary>

  suspend fun getTest(id: String): ReadingTest

  suspend fun submit(
    id: String,
    request: SubmitAnswersRequest,
  ): ReadingResult
}

class KtorReadingApi(
  private val client: HttpClient,
  private val config: ApiConfig,
) : ReadingApi {
  private val root get() = "${config.baseUrl.trimEnd('/')}/api/v1/reading/tests"

  override suspend fun listTests(): List<ReadingTestSummary> = client.get(root).body()

  override suspend fun getTest(id: String): ReadingTest = client.get("$root/$id").body()

  override suspend fun submit(
    id: String,
    request: SubmitAnswersRequest,
  ): ReadingResult =
    client
      .post("$root/$id/submit") {
        contentType(ContentType.Application.Json)
        setBody(request)
      }.body()
}

fun createHttpClient(
  engine: HttpClientEngine,
  json: Json,
  config: ApiConfig,
): HttpClient =
  HttpClient(engine) {
    // HTTP errors (4xx/5xx) throw so the repository falls back to offline data.
    expectSuccess = true

    install(ContentNegotiation) {
      json(json)
    }
    // Short timeouts so the app falls back to the cache quickly when offline.
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
              co.touchlab.kermit.Logger
                .withTag("Http")
                .d { message }
            }
          }
      }
    }
  }
