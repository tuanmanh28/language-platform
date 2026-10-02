package com.app.platform.language.backend.listening

import com.app.platform.language.backend.config.AppConfig
import com.app.platform.language.backend.fake.FakeAttemptStore
import com.app.platform.language.backend.fake.FakeDatabaseHealth
import com.app.platform.language.backend.fake.FakeListeningContentStore
import com.app.platform.language.backend.fake.FakeUserStore
import com.app.platform.language.backend.fake.FakeWritingSubmissionStore
import com.app.platform.language.backend.module
import com.app.platform.language.core.model.ApiError
import com.app.platform.language.core.model.BundledListeningTests
import com.app.platform.language.core.model.ContentJson
import com.app.platform.language.core.model.ListeningResult
import com.app.platform.language.core.model.ListeningTest
import com.app.platform.language.core.model.ListeningTestSummary
import com.app.platform.language.core.model.SubmitAnswersRequest
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ListeningRoutesTest {
  private val sample = BundledListeningTests.all.first()
  private val config = AppConfig.local.copy(audioBaseUrl = "https://cdn.example.com/audio")

  private fun ApplicationTestBuilder.jsonClient() =
    createClient {
      install(ContentNegotiation) { json(ContentJson) }
    }

  @Test
  fun listsBundledTests() =
    testApplication {
      application {
        module(
          FakeDatabaseHealth(),
          FakeUserStore(),
          FakeAttemptStore(),
          FakeWritingSubmissionStore(),
          config,
        )
      }

      val list = jsonClient().get("/api/v1/listening/tests").body<List<ListeningTestSummary>>()

      assertEquals(BundledListeningTests.all.map { it.toSummary() }, list)
    }

  @Test
  fun testIsServedWithAbsoluteAudioUrls() =
    testApplication {
      application {
        module(
          FakeDatabaseHealth(),
          FakeUserStore(),
          FakeAttemptStore(),
          FakeWritingSubmissionStore(),
          config,
        )
      }

      val test = jsonClient().get("/api/v1/listening/tests/${sample.id}").body<ListeningTest>()

      assertEquals(
        sample.sections.map { "https://cdn.example.com/audio/${it.audioUrl}" },
        test.sections.map { it.audioUrl },
      )
      assertEquals(sample.questionCount, test.questionCount)
    }

  @Test
  fun testResponseCarriesVersionEtagAndCacheControl() =
    testApplication {
      application {
        module(
          FakeDatabaseHealth(),
          FakeUserStore(),
          FakeAttemptStore(),
          FakeWritingSubmissionStore(),
          config,
        )
      }

      val response = jsonClient().get("/api/v1/listening/tests/${sample.id}")

      assertNotNull(response.headers[HttpHeaders.ETag])
      assertEquals("public, no-cache", response.headers[HttpHeaders.CacheControl])
    }

  @Test
  fun matchingEtagIsNotModified() =
    testApplication {
      application {
        module(
          FakeDatabaseHealth(),
          FakeUserStore(),
          FakeAttemptStore(),
          FakeWritingSubmissionStore(),
          config,
        )
      }
      val client = jsonClient()
      val etag = client.get("/api/v1/listening/tests").headers[HttpHeaders.ETag]

      val response = client.get("/api/v1/listening/tests") { header(HttpHeaders.IfNoneMatch, etag) }

      assertEquals(HttpStatusCode.NotModified, response.status)
    }

  @Test
  fun unknownTestIsNotFound() =
    testApplication {
      application {
        module(
          FakeDatabaseHealth(),
          FakeUserStore(),
          FakeAttemptStore(),
          FakeWritingSubmissionStore(),
          config,
        )
      }

      val response = jsonClient().get("/api/v1/listening/tests/nope")

      assertEquals(HttpStatusCode.NotFound, response.status)
      assertEquals(ApiError("Listening test not found"), response.body<ApiError>())
    }

  @Test
  fun storeFailureIsInternalError() =
    testApplication {
      val store = FakeListeningContentStore().apply { nextError = IllegalStateException("store is down") }
      application {
        module(
          FakeDatabaseHealth(),
          FakeUserStore(),
          FakeAttemptStore(),
          FakeWritingSubmissionStore(),
          config,
          listeningContentStore = store,
        )
      }

      val response = jsonClient().get("/api/v1/listening/tests")

      assertEquals(HttpStatusCode.InternalServerError, response.status)
      assertEquals(ApiError("Internal error"), response.body<ApiError>())
    }

  @Test
  fun submitScoresWithSharedEngine() =
    testApplication {
      application {
        module(
          FakeDatabaseHealth(),
          FakeUserStore(),
          FakeAttemptStore(),
          FakeWritingSubmissionStore(),
          config,
        )
      }
      val perfect = sample.allQuestions().associate { it.id to it.acceptedAnswers.first() }

      val result =
        jsonClient()
          .post("/api/v1/listening/tests/${sample.id}/submit") {
            contentType(ContentType.Application.Json)
            setBody(SubmitAnswersRequest(perfect))
          }.body<ListeningResult>()

      assertEquals(sample.questionCount, result.correctCount)
      assertEquals(9.0, result.band)
    }

  @Test
  fun submitResultRevealsAnswerKeyAndExplanations() =
    testApplication {
      application {
        module(
          FakeDatabaseHealth(),
          FakeUserStore(),
          FakeAttemptStore(),
          FakeWritingSubmissionStore(),
          config,
        )
      }

      val result =
        jsonClient()
          .post("/api/v1/listening/tests/${sample.id}/submit") {
            contentType(ContentType.Application.Json)
            setBody(SubmitAnswersRequest(emptyMap()))
          }.body<ListeningResult>()

      assertEquals(sample.allQuestions().map { it.acceptedAnswers }, result.questionResults.map { it.acceptedAnswers })
      assertEquals(sample.allQuestions().map { it.explanation }, result.questionResults.map { it.explanation })
    }

  @Test
  fun submitToUnknownTestIsNotFound() =
    testApplication {
      application {
        module(
          FakeDatabaseHealth(),
          FakeUserStore(),
          FakeAttemptStore(),
          FakeWritingSubmissionStore(),
          config,
        )
      }

      val response =
        jsonClient().post("/api/v1/listening/tests/nope/submit") {
          contentType(ContentType.Application.Json)
          setBody(SubmitAnswersRequest(emptyMap()))
        }

      assertEquals(HttpStatusCode.NotFound, response.status)
    }

  @Test
  fun malformedSubmitBodyIsBadRequest() =
    testApplication {
      application {
        module(
          FakeDatabaseHealth(),
          FakeUserStore(),
          FakeAttemptStore(),
          FakeWritingSubmissionStore(),
          config,
        )
      }

      val response =
        jsonClient().post("/api/v1/listening/tests/${sample.id}/submit") {
          contentType(ContentType.Application.Json)
          setBody("not json")
        }

      assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
