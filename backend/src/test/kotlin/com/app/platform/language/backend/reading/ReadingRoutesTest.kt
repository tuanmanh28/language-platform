package com.app.platform.language.backend.reading

import com.app.platform.language.backend.fake.FakeAttemptStore
import com.app.platform.language.backend.fake.FakeContentStore
import com.app.platform.language.backend.fake.FakeDatabaseHealth
import com.app.platform.language.backend.fake.FakeUserStore
import com.app.platform.language.backend.module
import com.app.platform.language.core.model.ApiError
import com.app.platform.language.core.model.BundledReadingTests
import com.app.platform.language.core.model.ContentJson
import com.app.platform.language.core.model.ReadingResult
import com.app.platform.language.core.model.ReadingTest
import com.app.platform.language.core.model.ReadingTestSummary
import com.app.platform.language.core.model.SubmitAnswersRequest
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadingRoutesTest {
  private val sample = BundledReadingTests.all.first()

  private fun ApplicationTestBuilder.jsonClient() =
    createClient {
      install(ContentNegotiation) { json(ContentJson) }
    }

  @Test
  fun listsAndServesBundledTests() =
    testApplication {
      application { module(FakeDatabaseHealth(), FakeUserStore(), FakeAttemptStore()) }
      val client = jsonClient()

      val list = client.get("/api/v1/reading/tests").body<List<ReadingTestSummary>>()
      val test = client.get("/api/v1/reading/tests/${list.first().id}").body<ReadingTest>()

      assertEquals(BundledReadingTests.all.size, list.size)
      assertEquals(list.first().questionCount, test.questionCount)
    }

  @Test
  fun testResponseCarriesVersionEtagAndCacheControl() =
    testApplication {
      application { module(FakeDatabaseHealth(), FakeUserStore(), FakeAttemptStore()) }

      val response = jsonClient().get("/api/v1/reading/tests/${sample.id}")

      assertEquals("\"1\"", response.headers[HttpHeaders.ETag])
      assertEquals("public, no-cache", response.headers[HttpHeaders.CacheControl])
    }

  @Test
  fun matchingEtagIsNotModified() =
    testApplication {
      application { module(FakeDatabaseHealth(), FakeUserStore(), FakeAttemptStore()) }
      val client = jsonClient()
      val etag = client.get("/api/v1/reading/tests").headers[HttpHeaders.ETag]

      val response = client.get("/api/v1/reading/tests") { header(HttpHeaders.IfNoneMatch, etag) }

      assertEquals(HttpStatusCode.NotModified, response.status)
      assertEquals(etag, response.headers[HttpHeaders.ETag])
      assertEquals("", response.bodyAsText())
    }

  @Test
  fun weakMatchInEtagListIsNotModified() =
    testApplication {
      application { module(FakeDatabaseHealth(), FakeUserStore(), FakeAttemptStore()) }

      val response =
        jsonClient().get("/api/v1/reading/tests/${sample.id}") {
          header(HttpHeaders.IfNoneMatch, "\"0\", W/\"1\"")
        }

      assertEquals(HttpStatusCode.NotModified, response.status)
    }

  @Test
  fun staleEtagGetsFullResponse() =
    testApplication {
      application { module(FakeDatabaseHealth(), FakeUserStore(), FakeAttemptStore()) }

      val response =
        jsonClient().get("/api/v1/reading/tests/${sample.id}") { header(HttpHeaders.IfNoneMatch, "\"0\"") }

      assertEquals(HttpStatusCode.OK, response.status)
      assertEquals(sample, response.body<ReadingTest>())
    }

  @Test
  fun storeFailureIsInternalError() =
    testApplication {
      val store = FakeContentStore().apply { nextError = IllegalStateException("store is down") }
      application { module(FakeDatabaseHealth(), FakeUserStore(), FakeAttemptStore(), contentStore = store) }

      val response = jsonClient().get("/api/v1/reading/tests")

      assertEquals(HttpStatusCode.InternalServerError, response.status)
      assertEquals(ApiError("Internal error"), response.body<ApiError>())
    }

  @Test
  fun unknownTestIsNotFound() =
    testApplication {
      application { module(FakeDatabaseHealth(), FakeUserStore(), FakeAttemptStore()) }

      val response = jsonClient().get("/api/v1/reading/tests/nope")

      assertEquals(HttpStatusCode.NotFound, response.status)
      assertEquals(ApiError("Reading test not found"), response.body<ApiError>())
    }

  @Test
  fun submitScoresWithSharedEngine() =
    testApplication {
      application { module(FakeDatabaseHealth(), FakeUserStore(), FakeAttemptStore()) }
      val perfect = sample.allQuestions().associate { it.id to it.acceptedAnswers.first() }

      val result =
        jsonClient()
          .post("/api/v1/reading/tests/${sample.id}/submit") {
            contentType(ContentType.Application.Json)
            setBody(SubmitAnswersRequest(perfect))
          }.body<ReadingResult>()

      assertEquals(sample.questionCount, result.correctCount)
      assertEquals(9.0, result.band)
    }

  @Test
  fun submitResultRevealsAnswerKeyAndExplanations() =
    testApplication {
      application { module(FakeDatabaseHealth(), FakeUserStore(), FakeAttemptStore()) }

      val result =
        jsonClient()
          .post("/api/v1/reading/tests/${sample.id}/submit") {
            contentType(ContentType.Application.Json)
            setBody(SubmitAnswersRequest(emptyMap()))
          }.body<ReadingResult>()

      assertEquals(sample.allQuestions().map { it.acceptedAnswers }, result.questionResults.map { it.acceptedAnswers })
      assertEquals(sample.allQuestions().map { it.explanation }, result.questionResults.map { it.explanation })
    }

  @Test
  fun submitToUnknownTestIsNotFound() =
    testApplication {
      application { module(FakeDatabaseHealth(), FakeUserStore(), FakeAttemptStore()) }

      val response =
        jsonClient().post("/api/v1/reading/tests/nope/submit") {
          contentType(ContentType.Application.Json)
          setBody(SubmitAnswersRequest(emptyMap()))
        }

      assertEquals(HttpStatusCode.NotFound, response.status)
    }

  @Test
  fun malformedSubmitBodyIsBadRequest() =
    testApplication {
      application { module(FakeDatabaseHealth(), FakeUserStore(), FakeAttemptStore()) }

      val response =
        jsonClient().post("/api/v1/reading/tests/${sample.id}/submit") {
          contentType(ContentType.Application.Json)
          setBody("not json")
        }

      assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
