package com.app.platform.language.backend.health

import com.app.platform.language.backend.module
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class HealthRoutesTest {
  @Test
  fun healthIsOk() =
    testApplication {
      application { module() }

      assertEquals(HttpStatusCode.OK, client.get("/health").status)
    }
}
