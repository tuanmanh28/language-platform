package com.app.platform.language.backend.plugins

import com.app.platform.language.backend.config.AllowedOrigins
import com.app.platform.language.backend.config.AppConfig
import com.app.platform.language.backend.fake.FakeAttemptStore
import com.app.platform.language.backend.fake.FakeDatabaseHealth
import com.app.platform.language.backend.fake.FakeUserStore
import com.app.platform.language.backend.module
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CorsTest {
  private val allowedOrigin = "https://app.example.com"

  @Test
  fun allowedOriginIsAccepted() =
    testApplication {
      useAllowedOrigins(AllowedOrigins.Only(setOf(allowedOrigin)))

      val response = client.get("/health") { header(HttpHeaders.Origin, allowedOrigin) }

      assertEquals(HttpStatusCode.OK, response.status)
      assertEquals(allowedOrigin, response.headers[HttpHeaders.AccessControlAllowOrigin])
    }

  @Test
  fun disallowedOriginIsRejected() =
    testApplication {
      useAllowedOrigins(AllowedOrigins.Only(setOf(allowedOrigin)))

      val response = client.get("/health") { header(HttpHeaders.Origin, "https://evil.example.com") }

      assertEquals(HttpStatusCode.Forbidden, response.status)
      assertNull(response.headers[HttpHeaders.AccessControlAllowOrigin])
    }

  @Test
  fun anyOriginIsAcceptedWhenAllAreAllowed() =
    testApplication {
      useAllowedOrigins(AllowedOrigins.All)

      val response = client.get("/health") { header(HttpHeaders.Origin, "https://anything.example.com") }

      assertEquals(HttpStatusCode.OK, response.status)
      assertEquals("*", response.headers[HttpHeaders.AccessControlAllowOrigin])
    }

  private fun ApplicationTestBuilder.useAllowedOrigins(allowedOrigins: AllowedOrigins) {
    application {
      module(
        FakeDatabaseHealth(),
        FakeUserStore(),
        FakeAttemptStore(),
        AppConfig.local.copy(allowedOrigins = allowedOrigins),
      )
    }
  }
}
