package com.app.platform.language.backend.attempt

import com.app.platform.language.backend.auth.AuthIdentity
import com.app.platform.language.backend.content.Visibility
import com.app.platform.language.backend.database.AppDatabase
import com.app.platform.language.backend.database.PostgresTestDatabase
import com.app.platform.language.backend.fake.FakeDatabaseHealth
import com.app.platform.language.backend.fake.FakeTokenVerifier
import com.app.platform.language.backend.fake.FakeWritingSubmissionStore
import com.app.platform.language.backend.module
import com.app.platform.language.backend.reading.DatabaseContentStore
import com.app.platform.language.backend.reading.ReadingContentSeeder
import com.app.platform.language.backend.user.DatabaseUserStore
import com.app.platform.language.core.exam.ReadingScorer
import com.app.platform.language.core.model.Attempt
import com.app.platform.language.core.model.AttemptPage
import com.app.platform.language.core.model.BundledReadingTests
import com.app.platform.language.core.model.ContentJson
import com.app.platform.language.core.model.SubmitAttemptRequest
import com.github.michaelbull.result.getOrElse
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
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
import kotlin.time.Instant

class AttemptsFromDatabaseTest {
  private val sample = BundledReadingTests.all.first()
  private val learner = AuthIdentity(uid = "firebase-uid-1", email = "learner@example.com", displayName = "Lan")

  @BeforeTest
  fun seedContent() =
    runTest {
      PostgresTestDatabase.clean()
      ReadingContentSeeder(database).seed(readingContentDir, Visibility.PUBLIC).getOrElse { fail(it.message) }
    }

  @Test
  fun repostedAttemptIsStoredOnceWithServerScore() =
    testApplication {
      application {
        module(
          FakeDatabaseHealth(),
          DatabaseUserStore(database),
          DatabaseAttemptStore(database),
          FakeWritingSubmissionStore(),
          contentStore = DatabaseContentStore(database),
          tokenVerifier = FakeTokenVerifier(mapOf(TOKEN to learner)),
        )
      }
      val client = createClient { install(ContentNegotiation) { json(ContentJson) } }
      val answers = sample.allQuestions().associate { it.id to it.acceptedAnswers.first() }
      val request = SubmitAttemptRequest("client-1", sample.id, answers, Instant.parse("2026-09-30T08:15:00Z"))

      val responses =
        List(2) {
          client.post("/api/v1/attempts") {
            bearerAuth(TOKEN)
            contentType(ContentType.Application.Json)
            setBody(request)
          }
        }
      val page = client.get("/api/v1/attempts") { bearerAuth(TOKEN) }.body<AttemptPage>()

      val attempt = responses.first().body<Attempt>()
      assertEquals(listOf(HttpStatusCode.Created, HttpStatusCode.OK), responses.map { it.status })
      assertEquals(attempt, responses.last().body<Attempt>())
      assertEquals(ReadingScorer.score(sample, answers).band, attempt.band)
      assertEquals(AttemptPage(listOf(attempt), nextCursor = null), page)
    }

  companion object {
    private const val TOKEN = "valid-token"
    private val readingContentDir = Path(System.getProperty("backend.readingContentDir"))
    private lateinit var database: AppDatabase

    @JvmStatic
    @BeforeAll
    fun connectDatabase() {
      database = PostgresTestDatabase.connect()
    }
  }
}
