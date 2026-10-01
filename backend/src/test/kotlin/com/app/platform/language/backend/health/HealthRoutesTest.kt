package com.app.platform.language.backend.health

import com.app.platform.language.backend.config.AppConfig
import com.app.platform.language.backend.config.AppEnv
import com.app.platform.language.backend.fake.FakeDatabaseHealth
import com.app.platform.language.backend.fake.FakeUserStore
import com.app.platform.language.backend.module
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class HealthRoutesTest {
  private val expectedVersion =
    checkNotNull(System.getProperty("backend.expectedVersion")) { "Run through Gradle, which sets the version" }

  private fun ApplicationTestBuilder.startWith(databaseHealth: DatabaseHealth) =
    application { module(databaseHealth, FakeUserStore(), AppConfig.local.copy(env = AppEnv.STAGING)) }

  private fun ApplicationTestBuilder.jsonClient() = createClient { install(ContentNegotiation) { json() } }

  @Test
  fun healthReportsStatusVersionEnvAndReachableDatabase() =
    testApplication {
      startWith(FakeDatabaseHealth(isUp = true))

      val response = jsonClient().get("/health")

      assertEquals(HttpStatusCode.OK, response.status)
      assertEquals(
        HealthResponse(status = "ok", version = expectedVersion, env = "staging", database = "up"),
        response.body<HealthResponse>(),
      )
    }

  @Test
  fun unreachableDatabaseMakesHealthDegraded() =
    testApplication {
      startWith(FakeDatabaseHealth(isUp = false))

      val response = jsonClient().get("/health")

      assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
      assertEquals(
        HealthResponse(status = "degraded", version = expectedVersion, env = "staging", database = "down"),
        response.body<HealthResponse>(),
      )
    }
}
