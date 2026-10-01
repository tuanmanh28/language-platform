package com.app.platform.language.backend.health

import com.app.platform.language.backend.config.AppConfig
import com.app.platform.language.backend.config.AppEnv
import com.app.platform.language.backend.module
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class HealthRoutesTest {
  @Test
  fun healthReportsStatusVersionAndEnv() =
    testApplication {
      application { module(AppConfig.local.copy(env = AppEnv.STAGING)) }
      val client = createClient { install(ContentNegotiation) { json() } }

      val expectedVersion =
        checkNotNull(System.getProperty("backend.expectedVersion")) { "Run through Gradle, which sets the version" }

      val response = client.get("/health")

      assertEquals(HttpStatusCode.OK, response.status)
      assertEquals(
        HealthResponse(status = "ok", version = expectedVersion, env = "staging"),
        response.body<HealthResponse>(),
      )
    }
}
