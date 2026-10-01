package com.app.platform.language.backend.listening

import com.app.platform.language.backend.config.AppConfig
import com.app.platform.language.backend.content.Visibility
import com.app.platform.language.backend.database.AppDatabase
import com.app.platform.language.backend.database.PostgresTestDatabase
import com.app.platform.language.backend.fake.FakeAttemptStore
import com.app.platform.language.backend.fake.FakeDatabaseHealth
import com.app.platform.language.backend.fake.FakeUserStore
import com.app.platform.language.backend.module
import com.app.platform.language.core.exam.ListeningScorer
import com.app.platform.language.core.model.BundledListeningTests
import com.app.platform.language.core.model.ContentJson
import com.app.platform.language.core.model.ListeningResult
import com.app.platform.language.core.model.ListeningTest
import com.app.platform.language.core.model.ListeningTestSummary
import com.app.platform.language.core.model.SubmitAnswersRequest
import com.github.michaelbull.result.getOrElse
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
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

class ListeningFromDatabaseTest {
  private val sample = BundledListeningTests.all.first()
  private val config = AppConfig.local.copy(audioBaseUrl = "https://cdn.example.com/audio")

  @BeforeTest
  fun seedContent() =
    runTest {
      PostgresTestDatabase.clean()
      ListeningContentSeeder(database).seed(listeningContentDir, Visibility.PUBLIC).getOrElse { fail(it.message) }
    }

  @Test
  fun seededTestIsListedServedAndScoredLikeTheSharedEngine() =
    testApplication {
      application {
        module(
          FakeDatabaseHealth(),
          FakeUserStore(),
          FakeAttemptStore(),
          config,
          listeningContentStore = DatabaseListeningContentStore(database),
        )
      }
      val client = createClient { install(ContentNegotiation) { json(ContentJson) } }
      val answers =
        sample.allQuestions().withIndex().associate { (index, question) ->
          question.id to if (index % 2 == 0) question.acceptedAnswers.first() else "wrong"
        }

      val list = client.get("/api/v1/listening/tests").body<List<ListeningTestSummary>>()
      val test = client.get("/api/v1/listening/tests/${sample.id}").body<ListeningTest>()
      val result =
        client
          .post("/api/v1/listening/tests/${sample.id}/submit") {
            contentType(ContentType.Application.Json)
            setBody(SubmitAnswersRequest(answers))
          }.body<ListeningResult>()

      assertEquals(BundledListeningTests.all.map { it.toSummary() }.sortedBy { it.id }, list)
      assertEquals("https://cdn.example.com/audio/${sample.sections.first().audioUrl}", test.sections.first().audioUrl)
      assertEquals(ListeningScorer.score(sample, answers), result)
    }

  companion object {
    private val listeningContentDir = Path(System.getProperty("backend.listeningContentDir"))
    private lateinit var database: AppDatabase

    @JvmStatic
    @BeforeAll
    fun connectDatabase() {
      database = PostgresTestDatabase.connect()
    }
  }
}
