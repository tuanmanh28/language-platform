package com.app.platform.language.backend.plugins

import com.app.platform.language.backend.fake.FakeAttemptStore
import com.app.platform.language.backend.fake.FakeDatabaseHealth
import com.app.platform.language.backend.fake.FakeUserStore
import com.app.platform.language.backend.module
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RequestIdTest {
  @Test
  fun requestIdIsGeneratedWhenAbsent() =
    testApplication {
      application { module(FakeDatabaseHealth(), FakeUserStore(), FakeAttemptStore()) }

      val requestId = client.get("/health").headers[HttpHeaders.XRequestId]

      UUID.fromString(assertNotNull(requestId))
    }

  @Test
  fun incomingRequestIdIsEchoed() =
    testApplication {
      application { module(FakeDatabaseHealth(), FakeUserStore(), FakeAttemptStore()) }

      val response = client.get("/health") { header(HttpHeaders.XRequestId, "client-abc_123") }

      assertEquals("client-abc_123", response.headers[HttpHeaders.XRequestId])
    }

  @Test
  fun unsafeRequestIdIsReplacedWithGeneratedOne() =
    testApplication {
      application { module(FakeDatabaseHealth(), FakeUserStore(), FakeAttemptStore()) }

      val response = client.get("/health") { header(HttpHeaders.XRequestId, "bad id\"}") }

      UUID.fromString(assertNotNull(response.headers[HttpHeaders.XRequestId]))
    }
}
