package com.app.platform.language.backend.reading

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
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
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
      application { module() }
      val client = jsonClient()

      val list = client.get("/api/v1/reading/tests").body<List<ReadingTestSummary>>()
      val test = client.get("/api/v1/reading/tests/${list.first().id}").body<ReadingTest>()

      assertEquals(BundledReadingTests.all.size, list.size)
      assertEquals(list.first().questionCount, test.questionCount)
    }

  @Test
  fun unknownTestIsNotFound() =
    testApplication {
      application { module() }

      val response = jsonClient().get("/api/v1/reading/tests/nope")

      assertEquals(HttpStatusCode.NotFound, response.status)
      assertEquals(ApiError("Reading test not found"), response.body<ApiError>())
    }

  @Test
  fun submitScoresWithSharedEngine() =
    testApplication {
      application { module() }
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
  fun submitToUnknownTestIsNotFound() =
    testApplication {
      application { module() }

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
      application { module() }

      val response =
        jsonClient().post("/api/v1/reading/tests/${sample.id}/submit") {
          contentType(ContentType.Application.Json)
          setBody("not json")
        }

      assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
