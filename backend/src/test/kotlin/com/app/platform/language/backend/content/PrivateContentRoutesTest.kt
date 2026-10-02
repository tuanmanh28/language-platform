package com.app.platform.language.backend.content

import com.app.platform.language.backend.auth.AuthIdentity
import com.app.platform.language.backend.config.AppConfig
import com.app.platform.language.backend.config.AudioStorageConfig
import com.app.platform.language.backend.config.DevAuthConfig
import com.app.platform.language.backend.fake.FakeAttemptStore
import com.app.platform.language.backend.fake.FakeContentStore
import com.app.platform.language.backend.fake.FakeDatabaseHealth
import com.app.platform.language.backend.fake.FakeListeningContentStore
import com.app.platform.language.backend.fake.FakeTokenVerifier
import com.app.platform.language.backend.fake.FakeUserStore
import com.app.platform.language.backend.fake.FakeWritingSubmissionStore
import com.app.platform.language.backend.listening.StoredListeningTest
import com.app.platform.language.backend.module
import com.app.platform.language.backend.reading.StoredReadingTest
import com.app.platform.language.core.model.BundledListeningTests
import com.app.platform.language.core.model.BundledReadingTests
import com.app.platform.language.core.model.ContentJson
import com.app.platform.language.core.model.ListeningTest
import com.app.platform.language.core.model.ListeningTestSummary
import com.app.platform.language.core.model.ReadingTestSummary
import com.app.platform.language.core.model.SubmitAnswersRequest
import com.app.platform.language.core.model.SubmitAttemptRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class PrivateContentRoutesTest {
  private val publicReading = BundledReadingTests.all.first()
  private val privateReading = publicReading.copy(id = "private-reading", title = "Owner reading practice")
  private val publicListening = BundledListeningTests.all.first()
  private val privateListening =
    publicListening.copy(
      id = "private-listening",
      title = "Owner listening practice",
      sections = publicListening.sections.map { it.copy(audioUrl = "private-listening/section-${it.number}.mp3") },
    )
  private val owner = AuthIdentity("owner-uid", OWNER_EMAIL, "Owner", isEmailVerified = true)
  private val learner = AuthIdentity("learner-uid", "learner@example.com", "Lan", isEmailVerified = true)
  private val verifier =
    FakeTokenVerifier(
      mapOf(
        OWNER_TOKEN to owner,
        LEARNER_TOKEN to learner,
        UNVERIFIED_OWNER_TOKEN to owner.copy(uid = "impostor-uid", isEmailVerified = false),
      ),
    )

  @TempDir
  lateinit var contentDir: Path

  private fun ApplicationTestBuilder.start(): HttpClient {
    val config =
      AppConfig.local.copy(
        contentDir = contentDir,
        ownerEmails = setOf(OWNER_EMAIL),
        devAuth = DevAuthConfig(DEV_TOKEN, OWNER_EMAIL),
        audioStorage = AudioStorageConfig.Local("http://localhost:8080"),
      )
    application {
      module(
        FakeDatabaseHealth(),
        FakeUserStore(),
        FakeAttemptStore(),
        FakeWritingSubmissionStore(),
        config,
        contentStore =
          FakeContentStore(
            listOf(
              StoredReadingTest(publicReading, 1, Visibility.PUBLIC),
              StoredReadingTest(privateReading, 1, Visibility.PRIVATE),
            ),
          ),
        listeningContentStore =
          FakeListeningContentStore(
            listOf(
              StoredListeningTest(publicListening, 1, Visibility.PUBLIC),
              StoredListeningTest(privateListening, 1, Visibility.PRIVATE),
            ),
          ),
        tokenVerifier = verifier,
      )
    }
    return createClient { install(ContentNegotiation) { json(ContentJson) } }
  }

  private fun HttpRequestBuilder.signIn(token: String?) {
    if (token != null) bearerAuth(token)
  }

  private suspend fun HttpClient.readingIds(token: String?): List<String> =
    get("/api/v1/reading/tests") { signIn(token) }.body<List<ReadingTestSummary>>().map { it.id }

  private suspend fun HttpClient.listeningIds(token: String?): List<String> =
    get("/api/v1/listening/tests") { signIn(token) }.body<List<ListeningTestSummary>>().map { it.id }

  private suspend fun HttpClient.privateTestStatuses(token: String?): List<HttpStatusCode> {
    val submit = SubmitAnswersRequest(emptyMap())
    return listOf(
      get("/api/v1/reading/tests/${privateReading.id}") { signIn(token) },
      get("/api/v1/listening/tests/${privateListening.id}") { signIn(token) },
      post("/api/v1/reading/tests/${privateReading.id}/submit") { json(submit, token) },
      post("/api/v1/listening/tests/${privateListening.id}/submit") { json(submit, token) },
    ).map { it.status }
  }

  private suspend fun HttpClient.submitAttempt(
    testId: String,
    token: String,
  ): HttpResponse =
    post("/api/v1/attempts") {
      json(SubmitAttemptRequest("client-$testId", testId, emptyMap(), Instant.parse("2026-10-01T08:00:00Z")), token)
    }

  private suspend fun HttpClient.audio(
    fileName: String,
    token: String?,
    configure: HttpRequestBuilder.() -> Unit = {},
  ): HttpResponse =
    get("/api/v1/listening/audio/${privateListening.id}/$fileName") {
      signIn(token)
      configure()
    }

  private inline fun <reified T> HttpRequestBuilder.json(
    body: T,
    token: String?,
  ) {
    signIn(token)
    contentType(ContentType.Application.Json)
    setBody(body)
  }

  private fun writeAudio(content: String) {
    contentDir
      .resolve("audio/${privateListening.id}")
      .createDirectories()
      .resolve("section-1.mp3")
      .writeText(content)
  }

  @Test
  fun anonymousViewerSeesOnlyPublicTests() =
    testApplication {
      val client = start()

      assertEquals(listOf(publicReading.id), client.readingIds(token = null))
      assertEquals(listOf(publicListening.id), client.listeningIds(token = null))
      assertEquals(List(4) { HttpStatusCode.NotFound }, client.privateTestStatuses(token = null))
    }

  @Test
  fun nonOwnerNeverSeesPrivateTests() =
    testApplication {
      val client = start()

      assertEquals(listOf(publicReading.id), client.readingIds(LEARNER_TOKEN))
      assertEquals(listOf(publicListening.id), client.listeningIds(LEARNER_TOKEN))
      assertEquals(List(4) { HttpStatusCode.NotFound }, client.privateTestStatuses(LEARNER_TOKEN))
      assertEquals(HttpStatusCode.NotFound, client.submitAttempt(privateReading.id, LEARNER_TOKEN).status)
    }

  @Test
  fun ownerEmailWithoutVerificationIsNotOwner() =
    testApplication {
      val client = start()

      assertEquals(listOf(publicReading.id), client.readingIds(UNVERIFIED_OWNER_TOKEN))
      assertEquals(List(4) { HttpStatusCode.NotFound }, client.privateTestStatuses(UNVERIFIED_OWNER_TOKEN))
      assertEquals(HttpStatusCode.NotFound, client.audio("section-1.mp3", UNVERIFIED_OWNER_TOKEN).status)
    }

  @Test
  fun ownerSeesPublicAndPrivateTests() =
    testApplication {
      val client = start()

      assertEquals(listOf(publicReading.id, privateReading.id), client.readingIds(OWNER_TOKEN))
      assertEquals(listOf(publicListening.id, privateListening.id), client.listeningIds(OWNER_TOKEN))
      assertEquals(List(4) { HttpStatusCode.OK }, client.privateTestStatuses(OWNER_TOKEN))
      assertEquals(HttpStatusCode.Created, client.submitAttempt(privateReading.id, OWNER_TOKEN).status)
    }

  @Test
  fun devTokenActsAsTheOwner() =
    testApplication {
      val client = start()

      assertEquals(listOf(publicReading.id, privateReading.id), client.readingIds(DEV_TOKEN))
      assertEquals(List(4) { HttpStatusCode.OK }, client.privateTestStatuses(DEV_TOKEN))
    }

  @Test
  fun invalidTokenIsUnauthorizedEvenOnPublicRoutes() =
    testApplication {
      val client = start()

      assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/reading/tests") { bearerAuth("forged") }.status)
    }

  @Test
  fun signedInResponsesAreKeptOutOfSharedCaches() =
    testApplication {
      val client = start()

      val anonymous = client.get("/api/v1/reading/tests")
      val owned = client.get("/api/v1/reading/tests") { bearerAuth(OWNER_TOKEN) }

      assertEquals("public, no-cache", anonymous.headers[HttpHeaders.CacheControl])
      assertEquals("private, no-cache", owned.headers[HttpHeaders.CacheControl])
      assertEquals(HttpHeaders.Authorization, anonymous.headers[HttpHeaders.Vary])
    }

  @Test
  fun privateListeningAudioIsServedThroughTheStreamingEndpoint() =
    testApplication {
      val client = start()

      val test = client.get("/api/v1/listening/tests/${privateListening.id}") { bearerAuth(OWNER_TOKEN) }

      assertEquals(
        privateListening.sections.map { "http://localhost:8080/api/v1/listening/audio/${it.audioUrl}" },
        test.body<ListeningTest>().sections.map { it.audioUrl },
      )
    }

  @Test
  fun audioIsStreamedOnlyToTheOwner() =
    testApplication {
      val client = start()
      writeAudio("fake mp3 bytes")

      val anonymous = client.audio("section-1.mp3", token = null)
      val learnerResponse = client.audio("section-1.mp3", LEARNER_TOKEN)
      val ownerResponse = client.audio("section-1.mp3", OWNER_TOKEN)

      assertEquals(HttpStatusCode.Unauthorized, anonymous.status)
      assertEquals(HttpStatusCode.NotFound, learnerResponse.status)
      assertEquals(HttpStatusCode.OK, ownerResponse.status)
      assertEquals("fake mp3 bytes", ownerResponse.bodyAsText())
      assertEquals(ContentType.Audio.MPEG, ownerResponse.contentType()?.withoutParameters())
      assertEquals("private, max-age=3600", ownerResponse.headers[HttpHeaders.CacheControl])
    }

  @Test
  fun audioSupportsRangeRequestsForSeeking() =
    testApplication {
      val client = start()
      writeAudio("0123456789")

      val response = client.audio("section-1.mp3", OWNER_TOKEN) { header(HttpHeaders.Range, "bytes=2-5") }

      assertEquals(HttpStatusCode.PartialContent, response.status)
      assertEquals("2345", response.bodyAsText())
    }

  @Test
  fun missingAudioIsNotFoundForTheOwner() =
    testApplication {
      val client = start()

      assertEquals(HttpStatusCode.NotFound, client.audio("section-9.mp3", OWNER_TOKEN).status)
    }

  private companion object {
    const val OWNER_EMAIL = "owner@example.com"
    const val OWNER_TOKEN = "owner-token"
    const val LEARNER_TOKEN = "learner-token"
    const val UNVERIFIED_OWNER_TOKEN = "unverified-owner-token"
    const val DEV_TOKEN = "dev-token"
  }
}
