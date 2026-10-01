package com.app.platform.language.backend.reading

import com.app.platform.language.backend.content.Visibility
import com.app.platform.language.backend.content.withoutAnswerKey
import com.app.platform.language.backend.database.AppDatabase
import com.app.platform.language.backend.database.PostgresTestDatabase
import com.app.platform.language.backend.fake.FakeAttemptStore
import com.app.platform.language.backend.fake.FakeDatabaseHealth
import com.app.platform.language.backend.fake.FakeUserStore
import com.app.platform.language.backend.module
import com.app.platform.language.core.exam.ReadingScorer
import com.app.platform.language.core.model.BundledReadingTests
import com.app.platform.language.core.model.ContentJson
import com.app.platform.language.core.model.ReadingResult
import com.app.platform.language.core.model.ReadingTest
import com.app.platform.language.core.model.ReadingTestSummary
import com.app.platform.language.core.model.SubmitAnswersRequest
import com.github.michaelbull.result.getOrElse
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import kotlin.io.path.Path
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class ReadingFromDatabaseTest {
  private val sample = BundledReadingTests.all.first()

  @BeforeTest
  fun seedContent() =
    runTest {
      PostgresTestDatabase.clean()
      ReadingContentSeeder(database).seed(readingContentDir, Visibility.PUBLIC).getOrElse { fail(it.message) }
    }

  @Test
  fun seededTestIsListedServedAndScoredLikeTheSharedEngine() =
    testApplication {
      application {
        module(FakeDatabaseHealth(), FakeUserStore(), FakeAttemptStore(), contentStore = DatabaseContentStore(database))
      }
      val client = createClient { install(ContentNegotiation) { json(ContentJson) } }
      val answers =
        sample.allQuestions().withIndex().associate { (index, question) ->
          question.id to if (index % 2 == 0) question.acceptedAnswers.first() else "wrong"
        }

      val list = client.get("/api/v1/reading/tests").body<List<ReadingTestSummary>>()
      val testResponse = client.get("/api/v1/reading/tests/${sample.id}")
      val result =
        client
          .post("/api/v1/reading/tests/${sample.id}/submit") {
            contentType(ContentType.Application.Json)
            setBody(SubmitAnswersRequest(answers))
          }.body<ReadingResult>()

      assertEquals(BundledReadingTests.all.map { it.toSummary() }.sortedBy { it.id }, list)
      assertEquals(sample.withoutAnswerKey(), testResponse.body<ReadingTest>())
      assertEquals("\"1\"", testResponse.headers[HttpHeaders.ETag])
      assertEquals(ReadingScorer.score(sample, answers), result)
    }

  companion object {
    private val readingContentDir = Path(System.getProperty("backend.readingContentDir"))
    private lateinit var database: AppDatabase

    @JvmStatic
    @BeforeAll
    fun connectDatabase() {
      database = PostgresTestDatabase.connect()
    }
  }
}
