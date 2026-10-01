package com.app.platform.language.shared.reading.data

import com.app.platform.language.core.model.BundledReadingTests
import com.app.platform.language.core.model.ContentJson
import com.app.platform.language.core.model.ReadingError
import com.app.platform.language.core.model.ReadingTest
import com.app.platform.language.core.model.ReadingTestSummary
import com.app.platform.language.shared.network.ApiConfig
import com.app.platform.language.shared.network.createHttpClient
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.getError
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.builtins.ListSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class KtorReadingApiTest {
  private val sample = BundledReadingTests.all.first()

  private fun api(handler: MockRequestHandler) =
    KtorReadingApi(createHttpClient(MockEngine(handler), ContentJson, ApiConfig(baseUrl = "http://test.local/")))

  private fun jsonResponse(body: String): MockRequestHandler =
    { respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")) }

  @Test
  fun listTestsCallsTheVersionedEndpoint() =
    runTest {
      var requestedUrl = ""
      val body = ContentJson.encodeToString(ListSerializer(ReadingTestSummary.serializer()), listOf(sample.toSummary()))
      val api =
        api { request ->
          requestedUrl = request.url.toString()
          jsonResponse(body)(request)
        }

      assertEquals(Ok(listOf(sample.toSummary())), api.listTests())
      assertEquals("http://test.local/api/v1/reading/tests", requestedUrl)
    }

  @Test
  fun getTestDecodesTheTest() =
    runTest {
      val api = api(jsonResponse(ContentJson.encodeToString(ReadingTest.serializer(), sample)))

      assertEquals(Ok(sample), api.getTest(sample.id))
    }

  @Test
  fun notFoundResponseMapsToNotFound() =
    runTest {
      val api = api { respondError(HttpStatusCode.NotFound) }

      assertEquals(Err(ReadingError.NotFound), api.getTest("missing"))
    }

  @Test
  fun networkFailureMapsToOffline() =
    runTest {
      val api = api { throw IOException("no route to host") }

      assertEquals(Err(ReadingError.Offline), api.listTests())
    }

  @Test
  fun serverErrorMapsToUnexpected() =
    runTest {
      val api = api { respondError(HttpStatusCode.InternalServerError) }

      assertIs<ReadingError.Unexpected>(api.listTests().getError())
    }
}
