package com.app.platform.language.backend.user

import com.app.platform.language.backend.auth.AuthIdentity
import com.app.platform.language.backend.auth.TokenError
import com.app.platform.language.backend.fake.FakeAttemptStore
import com.app.platform.language.backend.fake.FakeDatabaseHealth
import com.app.platform.language.backend.fake.FakeTokenVerifier
import com.app.platform.language.backend.fake.FakeUserStore
import com.app.platform.language.backend.module
import com.app.platform.language.core.model.ApiError
import com.app.platform.language.core.model.ContentJson
import com.app.platform.language.core.model.UserProfile
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class UserRoutesTest {
  private val identity = AuthIdentity(uid = "firebase-uid-1", email = "learner@example.com", displayName = "Lan")
  private val verifier = FakeTokenVerifier(mapOf(VALID_TOKEN to identity))
  private val store = FakeUserStore()

  private fun ApplicationTestBuilder.start() {
    application { module(FakeDatabaseHealth(), store, FakeAttemptStore(), tokenVerifier = verifier) }
  }

  private fun ApplicationTestBuilder.jsonClient() = createClient { install(ContentNegotiation) { json(ContentJson) } }

  @Test
  fun missingTokenIsUnauthorized() =
    testApplication {
      start()

      val response = client.get("/api/v1/me")

      assertEquals(HttpStatusCode.Unauthorized, response.status)
      assertEquals(emptyList(), store.users.toList())
    }

  @Test
  fun invalidTokenIsUnauthorized() =
    testApplication {
      start()

      val response = client.get("/api/v1/me") { bearerAuth("forged") }

      assertEquals(HttpStatusCode.Unauthorized, response.status)
      assertEquals(emptyList(), store.users.toList())
    }

  @Test
  fun nonBearerSchemeIsUnauthorized() =
    testApplication {
      start()

      val response = client.get("/api/v1/me") { header(HttpHeaders.Authorization, "Basic $VALID_TOKEN") }

      assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

  @Test
  fun unverifiableTokenIsUnauthorized() =
    testApplication {
      verifier.nextError = TokenError.Unverifiable(IllegalStateException("keys are down"))
      start()

      val response = client.get("/api/v1/me") { bearerAuth(VALID_TOKEN) }

      assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

  @Test
  fun validTokenReturnsProfileOfCreatedUser() =
    testApplication {
      start()

      val response = jsonClient().get("/api/v1/me") { bearerAuth(VALID_TOKEN) }

      val user = store.users.single()
      assertEquals(HttpStatusCode.OK, response.status)
      assertEquals(UserProfile(user.id.toString(), "learner@example.com", "Lan"), response.body<UserProfile>())
    }

  @Test
  fun repeatedRequestsKeepTheSameUser() =
    testApplication {
      start()
      val client = jsonClient()

      val first = client.get("/api/v1/me") { bearerAuth(VALID_TOKEN) }.body<UserProfile>()
      val second = client.get("/api/v1/me") { bearerAuth(VALID_TOKEN) }.body<UserProfile>()

      assertEquals(first, second)
      assertEquals(1, store.users.size)
    }

  @Test
  fun userStoreFailureIsInternalError() =
    testApplication {
      store.nextError = IllegalStateException("database is down")
      start()

      val response = jsonClient().get("/api/v1/me") { bearerAuth(VALID_TOKEN) }

      assertEquals(HttpStatusCode.InternalServerError, response.status)
      assertEquals(ApiError("Internal error"), response.body<ApiError>())
    }

  @Test
  fun readingEndpointsStayPublic() =
    testApplication {
      start()

      val response = client.get("/api/v1/reading/tests")

      assertEquals(HttpStatusCode.OK, response.status)
    }

  private companion object {
    const val VALID_TOKEN = "valid-token"
  }
}
