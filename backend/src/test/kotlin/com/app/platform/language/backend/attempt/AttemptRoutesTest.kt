package com.app.platform.language.backend.attempt

import com.app.platform.language.backend.auth.AuthIdentity
import com.app.platform.language.backend.fake.FakeAttemptStore
import com.app.platform.language.backend.fake.FakeDatabaseHealth
import com.app.platform.language.backend.fake.FakeTokenVerifier
import com.app.platform.language.backend.fake.FakeUserStore
import com.app.platform.language.backend.module
import com.app.platform.language.core.exam.ReadingScorer
import com.app.platform.language.core.model.ApiError
import com.app.platform.language.core.model.Attempt
import com.app.platform.language.core.model.AttemptPage
import com.app.platform.language.core.model.BundledReadingTests
import com.app.platform.language.core.model.ContentJson
import com.app.platform.language.core.model.SubmitAttemptRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class AttemptRoutesTest {
  private val sample = BundledReadingTests.all.first()
  private val learner = AuthIdentity(uid = "firebase-uid-1", email = "learner@example.com", displayName = "Lan")
  private val otherLearner = AuthIdentity(uid = "firebase-uid-2", email = "other@example.com", displayName = "Minh")
  private val verifier = FakeTokenVerifier(mapOf(LEARNER_TOKEN to learner, OTHER_TOKEN to otherLearner))
  private val users = FakeUserStore()
  private val attempts = FakeAttemptStore()
  private val createdAt = Instant.parse("2026-09-30T08:15:00Z")
  private val answers =
    sample.allQuestions().withIndex().associate { (index, question) ->
      question.id to if (index % 2 == 0) question.acceptedAnswers.first() else "wrong"
    }

  private fun ApplicationTestBuilder.start(): HttpClient {
    application { module(FakeDatabaseHealth(), users, attempts, tokenVerifier = verifier) }
    return createClient { install(ContentNegotiation) { json(ContentJson) } }
  }

  private fun request(clientId: String = "client-1") = SubmitAttemptRequest(clientId, sample.id, answers, createdAt)

  private suspend fun HttpClient.submit(
    request: SubmitAttemptRequest,
    token: String = LEARNER_TOKEN,
  ): HttpResponse =
    post("/api/v1/attempts") {
      bearerAuth(token)
      contentType(ContentType.Application.Json)
      setBody(request)
    }

  private suspend fun HttpClient.listAttempts(
    query: String = "",
    token: String = LEARNER_TOKEN,
  ): HttpResponse = get("/api/v1/attempts$query") { bearerAuth(token) }

  @Test
  fun missingTokenIsUnauthorized() =
    testApplication {
      val client = start()

      val submitted = client.post("/api/v1/attempts") { contentType(ContentType.Application.Json) }
      val listed = client.get("/api/v1/attempts")

      assertEquals(HttpStatusCode.Unauthorized, submitted.status)
      assertEquals(HttpStatusCode.Unauthorized, listed.status)
    }

  @Test
  fun newAttemptIsScoredOnTheServerAndIgnoresClientScores() =
    testApplication {
      val client = start()
      val forged =
        ContentJson
          .encodeToString(SubmitAttemptRequest.serializer(), request())
          .replaceFirst("{", """{"correctCount":40,"totalQuestions":40,"band":9.0,""")

      val response =
        client.post("/api/v1/attempts") {
          bearerAuth(LEARNER_TOKEN)
          contentType(ContentType.Application.Json)
          setBody(forged)
        }

      val expected = ReadingScorer.score(sample, answers)
      val attempt = response.body<Attempt>()
      assertEquals(HttpStatusCode.Created, response.status)
      assertEquals(
        Triple(expected.correctCount, expected.totalQuestions, expected.band),
        Triple(attempt.correctCount, attempt.totalQuestions, attempt.band),
      )
      assertEquals(listOf("client-1", sample.id), listOf(attempt.clientId, attempt.testId))
      assertEquals(answers to createdAt, attempt.answers to attempt.createdAt)
    }

  @Test
  fun repostWithSameClientIdReturnsTheStoredAttempt() =
    testApplication {
      val client = start()

      val first = client.submit(request())
      val retried = client.submit(request().copy(answers = emptyMap()))

      assertEquals(HttpStatusCode.Created, first.status)
      assertEquals(HttpStatusCode.OK, retried.status)
      assertEquals(first.body<Attempt>(), retried.body<Attempt>())
      assertEquals(1, attempts.attemptsOf(users.users.single().id).size)
    }

  @Test
  fun unknownTestIsNotFound() =
    testApplication {
      val client = start()

      val response = client.submit(request().copy(testId = "missing-test"))

      assertEquals(HttpStatusCode.NotFound, response.status)
      assertEquals(ApiError("Reading test not found"), response.body<ApiError>())
    }

  @Test
  fun blankOrOverlongClientIdIsBadRequest() =
    testApplication {
      val client = start()

      val blank = client.submit(request(clientId = " "))
      val overlong = client.submit(request(clientId = "x".repeat(65)))

      assertEquals(HttpStatusCode.BadRequest, blank.status)
      assertEquals(HttpStatusCode.BadRequest, overlong.status)
    }

  @Test
  fun answersOutsideTheTestOrOverlongAreBadRequestAndNotStored() =
    testApplication {
      val client = start()
      val firstQuestion = sample.allQuestions().first().id

      val unknownQuestion = client.submit(request().copy(answers = answers + ("not-a-question" to "TRUE")))
      val overlongAnswer = client.submit(request().copy(answers = answers + (firstQuestion to "x".repeat(201))))

      assertEquals(
        listOf(
          ApiError("answers must only use question ids of the test"),
          ApiError("answers must be at most 200 characters"),
        ),
        listOf(unknownQuestion.body<ApiError>(), overlongAnswer.body<ApiError>()),
      )
      assertEquals(List(2) { HttpStatusCode.BadRequest }, listOf(unknownQuestion.status, overlongAnswer.status))
      assertEquals(emptyList(), attempts.attemptsOf(users.users.single().id))
    }

  @Test
  fun malformedBodyIsBadRequest() =
    testApplication {
      val client = start()

      val response =
        client.post("/api/v1/attempts") {
          bearerAuth(LEARNER_TOKEN)
          contentType(ContentType.Application.Json)
          setBody("""{"clientId":"client-1","testId":"${sample.id}","answers":{},"createdAt":"yesterday"}""")
        }

      assertEquals(HttpStatusCode.BadRequest, response.status)
    }

  @Test
  fun usersOnlySeeTheirOwnAttempts() =
    testApplication {
      val client = start()
      val own = client.submit(request()).body<Attempt>()
      val other = client.submit(request(), token = OTHER_TOKEN).body<Attempt>()

      val ownPage = client.listAttempts().body<AttemptPage>()
      val otherPage = client.listAttempts(token = OTHER_TOKEN).body<AttemptPage>()

      assertEquals(listOf(own), ownPage.attempts)
      assertEquals(listOf(other), otherPage.attempts)
    }

  @Test
  fun listIsEmptyForUserWithoutAttempts() =
    testApplication {
      val client = start()

      val response = client.listAttempts()

      assertEquals(HttpStatusCode.OK, response.status)
      assertEquals(AttemptPage(emptyList(), nextCursor = null), response.body<AttemptPage>())
    }

  @Test
  fun attemptsAreListedNewestFirstAndPagedWithNextCursor() =
    testApplication {
      val client = start()
      val (oldest, middle, newest) = listOf("a", "b", "c").map { client.submit(request(it)).body<Attempt>() }

      val firstPage = client.listAttempts("?limit=2").body<AttemptPage>()
      val secondPage = client.listAttempts("?limit=2&cursor=${firstPage.nextCursor}").body<AttemptPage>()

      assertEquals(AttemptPage(listOf(newest, middle), nextCursor = AttemptCursor.after(middle).encode()), firstPage)
      assertEquals(AttemptPage(listOf(oldest), nextCursor = null), secondPage)
    }

  @Test
  fun pagingReturnsEveryAttemptWhenSyncTimesAreEqual() =
    testApplication {
      val client = start()
      attempts.syncStep = Duration.ZERO
      val submitted = listOf("a", "b", "c", "d", "e").map { client.submit(request(it)).body<Attempt>() }

      val pages = mutableListOf(client.listAttempts("?limit=2").body<AttemptPage>())
      while (pages.last().nextCursor != null) {
        pages += client.listAttempts("?limit=2&cursor=${pages.last().nextCursor}").body<AttemptPage>()
      }

      assertEquals(3, pages.size)
      assertEquals(submitted.sortedByDescending { it.id }, pages.flatMap { it.attempts })
    }

  @Test
  fun sinceReturnsAttemptsSyncedAfterItAndWithinTheOverlapBeforeIt() =
    testApplication {
      val client = start()
      val (_, second, third) = listOf("a", "b", "c").map { client.submit(request(it)).body<Attempt>() }

      val page = client.listAttempts("?since=${second.syncedAt + 10.seconds}").body<AttemptPage>()

      assertEquals(AttemptPage(listOf(third, second), nextCursor = null), page)
    }

  @Test
  fun invalidQueryIsBadRequest() =
    testApplication {
      val client = start()

      val statuses =
        listOf("?since=yesterday", "?cursor=1", "?limit=many", "?limit=0", "?limit=101").map {
          client.listAttempts(it).status
        }

      assertEquals(List(5) { HttpStatusCode.BadRequest }, statuses)
    }

  @Test
  fun storeFailureIsInternalError() =
    testApplication {
      val client = start()
      attempts.nextError = IllegalStateException("database is down")

      val submitted = client.submit(request())
      val listed = client.listAttempts()

      assertEquals(HttpStatusCode.InternalServerError, submitted.status)
      assertEquals(HttpStatusCode.InternalServerError, listed.status)
      assertEquals(ApiError("Internal error"), listed.body<ApiError>())
    }

  private companion object {
    const val LEARNER_TOKEN = "learner-token"
    const val OTHER_TOKEN = "other-token"
  }
}
