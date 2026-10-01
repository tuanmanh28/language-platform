package com.app.platform.language.backend.writing

import com.app.platform.language.backend.auth.AuthIdentity
import com.app.platform.language.backend.config.AppConfig
import com.app.platform.language.backend.config.AudioStorageConfig
import com.app.platform.language.backend.content.Visibility
import com.app.platform.language.backend.fake.FakeAttemptStore
import com.app.platform.language.backend.fake.FakeDatabaseHealth
import com.app.platform.language.backend.fake.FakeTokenVerifier
import com.app.platform.language.backend.fake.FakeUserStore
import com.app.platform.language.backend.fake.FakeWritingContentStore
import com.app.platform.language.backend.fake.FakeWritingSubmissionStore
import com.app.platform.language.backend.module
import com.app.platform.language.core.model.ApiError
import com.app.platform.language.core.model.ContentJson
import com.app.platform.language.core.model.IeltsModule
import com.app.platform.language.core.model.SubmitWritingRequest
import com.app.platform.language.core.model.WritingPrompt
import com.app.platform.language.core.model.WritingSubmission
import com.app.platform.language.core.model.WritingSubmissionStatus
import com.app.platform.language.core.model.WritingTask
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
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
import kotlin.uuid.Uuid

class WritingRoutesTest {
  private val publicPrompt =
    WritingPrompt(
      id = "public-task-1",
      module = IeltsModule.ACADEMIC,
      task = WritingTask.TASK_1_ACADEMIC,
      instructions = "Summarise the chart of bicycle sales.",
      imageUrl = "writing/public-task-1/chart.png",
      minWords = 150,
      timeLimitMinutes = 20,
    )
  private val privatePrompt =
    publicPrompt.copy(
      id = "private-task-1",
      instructions = "Summarise the chart of library visits.",
      imageUrl = "private-task-1/chart.png",
    )
  private val owner = AuthIdentity("owner-uid", OWNER_EMAIL, "Owner", isEmailVerified = true)
  private val learner = AuthIdentity("learner-uid", "learner@example.com", "Lan", isEmailVerified = true)
  private val verifier = FakeTokenVerifier(mapOf(OWNER_TOKEN to owner, LEARNER_TOKEN to learner))
  private val users = FakeUserStore()
  private val prompts =
    FakeWritingContentStore(
      listOf(
        StoredWritingPrompt(publicPrompt, 1, Visibility.PUBLIC),
        StoredWritingPrompt(privatePrompt, 1, Visibility.PRIVATE),
      ),
    )
  private val submissions = FakeWritingSubmissionStore()

  @TempDir
  lateinit var contentDir: Path

  private fun ApplicationTestBuilder.start(): HttpClient {
    val config =
      AppConfig.local.copy(
        contentDir = contentDir,
        ownerEmails = setOf(OWNER_EMAIL),
        audioBaseUrl = "https://cdn.example.com/media",
        audioStorage = AudioStorageConfig.Local("http://localhost:8080"),
      )
    application {
      module(
        FakeDatabaseHealth(),
        users,
        FakeAttemptStore(),
        submissions,
        config,
        writingContentStore = prompts,
        tokenVerifier = verifier,
      )
    }
    return createClient { install(ContentNegotiation) { json(ContentJson) } }
  }

  private suspend fun HttpClient.submit(
    request: SubmitWritingRequest,
    token: String = LEARNER_TOKEN,
  ): HttpResponse =
    post("/api/v1/writing/submissions") {
      bearerAuth(token)
      contentType(ContentType.Application.Json)
      setBody(request)
    }

  private suspend fun HttpClient.history(token: String = LEARNER_TOKEN): List<WritingSubmission> =
    get("/api/v1/writing/submissions") { bearerAuth(token) }.body()

  private suspend fun HttpClient.image(
    fileName: String,
    token: String,
  ): HttpResponse = get("/api/v1/writing/images/${privatePrompt.id}/$fileName") { bearerAuth(token) }

  private fun essay(text: String = ESSAY) = SubmitWritingRequest(publicPrompt.id, text)

  @Test
  fun everyWritingEndpointNeedsSignIn() =
    testApplication {
      val client = start()

      val statuses =
        listOf(
          client.get("/api/v1/writing/prompts"),
          client.post("/api/v1/writing/submissions") { contentType(ContentType.Application.Json) },
          client.get("/api/v1/writing/submissions"),
          client.get("/api/v1/writing/submissions/${Uuid.random()}"),
          client.get("/api/v1/writing/images/${privatePrompt.id}/chart.png"),
        ).map { it.status }

      assertEquals(List(5) { HttpStatusCode.Unauthorized }, statuses)
    }

  @Test
  fun learnerSeesOnlyPublicPromptsWithAbsoluteImageUrls() =
    testApplication {
      val client = start()

      val response = client.get("/api/v1/writing/prompts") { bearerAuth(LEARNER_TOKEN) }

      assertEquals(
        listOf(publicPrompt.copy(imageUrl = "https://cdn.example.com/media/writing/public-task-1/chart.png")),
        response.body<List<WritingPrompt>>(),
      )
    }

  @Test
  fun ownerSeesPrivatePromptsWithStreamedImages() =
    testApplication {
      val client = start()

      val listed = client.get("/api/v1/writing/prompts") { bearerAuth(OWNER_TOKEN) }.body<List<WritingPrompt>>()

      assertEquals(listOf(publicPrompt.id, privatePrompt.id), listed.map { it.id })
      assertEquals("http://localhost:8080/api/v1/writing/images/private-task-1/chart.png", listed.last().imageUrl)
    }

  @Test
  fun submissionIsStoredAsPendingWithTheServerWordCount() =
    testApplication {
      val client = start()
      val forged =
        ContentJson
          .encodeToString(SubmitWritingRequest.serializer(), essay())
          .replaceFirst("{", """{"wordCount":400,"status":"graded",""")

      val response =
        client.post("/api/v1/writing/submissions") {
          bearerAuth(LEARNER_TOKEN)
          contentType(ContentType.Application.Json)
          setBody(forged)
        }

      val submission = response.body<WritingSubmission>()
      assertEquals(HttpStatusCode.Created, response.status)
      assertEquals(
        WritingSubmission(
          id = submission.id,
          promptId = publicPrompt.id,
          text = ESSAY,
          wordCount = 11,
          submittedAt = submission.submittedAt,
          status = WritingSubmissionStatus.PENDING,
        ),
        submission,
      )
      assertEquals(listOf(submission), submissions.submissionsOf(users.users.single().id))
    }

  @Test
  fun unknownPromptIsNotFound() =
    testApplication {
      val client = start()

      val response = client.submit(essay().copy(promptId = "missing-prompt"))

      assertEquals(HttpStatusCode.NotFound, response.status)
      assertEquals(ApiError("Writing prompt not found"), response.body<ApiError>())
    }

  @Test
  fun privatePromptAcceptsSubmissionsOnlyFromTheOwner() =
    testApplication {
      val client = start()
      val request = essay().copy(promptId = privatePrompt.id)

      assertEquals(HttpStatusCode.NotFound, client.submit(request, LEARNER_TOKEN).status)
      assertEquals(HttpStatusCode.Created, client.submit(request, OWNER_TOKEN).status)
    }

  @Test
  fun blankOverlongOrNulTextIsBadRequestAndNotStored() =
    testApplication {
      val client = start()

      val responses = listOf(" \n ", "word ".repeat(4_001), "Parks\u0000matter").map { client.submit(essay(it)) }

      assertEquals(
        listOf(
          ApiError("text must not be blank"),
          ApiError("text must be at most 20000 characters"),
          ApiError("text must not contain NUL characters"),
        ),
        responses.map { it.body<ApiError>() },
      )
      assertEquals(List(3) { HttpStatusCode.BadRequest }, responses.map { it.status })
      assertEquals(emptyList(), submissions.submissionsOf(users.users.single().id))
    }

  @Test
  fun malformedBodyIsBadRequest() =
    testApplication {
      val client = start()

      val response =
        client.post("/api/v1/writing/submissions") {
          bearerAuth(LEARNER_TOKEN)
          contentType(ContentType.Application.Json)
          setBody("""{"promptId":"${publicPrompt.id}"}""")
        }

      assertEquals(HttpStatusCode.BadRequest, response.status)
    }

  @Test
  fun historyIsNewestFirstAndOnlyTheUsersOwn() =
    testApplication {
      val client = start()
      val first = client.submit(essay("First draft.")).body<WritingSubmission>()
      val second = client.submit(essay("Second draft.")).body<WritingSubmission>()
      val other = client.submit(essay("Owner essay."), OWNER_TOKEN).body<WritingSubmission>()

      assertEquals(listOf(second, first), client.history())
      assertEquals(listOf(other), client.history(OWNER_TOKEN))
    }

  @Test
  fun historyIsEmptyForUserWithoutSubmissions() =
    testApplication {
      val client = start()

      assertEquals(emptyList(), client.history())
    }

  @Test
  fun submissionIsReadableOnlyByItsAuthor() =
    testApplication {
      val client = start()
      val submission = client.submit(essay()).body<WritingSubmission>()

      val own = client.get("/api/v1/writing/submissions/${submission.id}") { bearerAuth(LEARNER_TOKEN) }
      val other = client.get("/api/v1/writing/submissions/${submission.id}") { bearerAuth(OWNER_TOKEN) }

      assertEquals(submission, own.body<WritingSubmission>())
      assertEquals(HttpStatusCode.NotFound, other.status)
      assertEquals(ApiError("Writing submission not found"), other.body<ApiError>())
    }

  @Test
  fun malformedOrUnknownSubmissionIdIsNotFound() =
    testApplication {
      val client = start()

      val statuses =
        listOf("not-a-uuid", "${Uuid.random()}").map { id ->
          client.get("/api/v1/writing/submissions/$id") { bearerAuth(LEARNER_TOKEN) }.status
        }

      assertEquals(List(2) { HttpStatusCode.NotFound }, statuses)
    }

  @Test
  fun promptStoreFailureIsInternalError() =
    testApplication {
      val client = start()
      prompts.nextError = IllegalStateException("database is down")

      val listed = client.get("/api/v1/writing/prompts") { bearerAuth(LEARNER_TOKEN) }
      val submitted = client.submit(essay())

      assertEquals(List(2) { HttpStatusCode.InternalServerError }, listOf(listed.status, submitted.status))
      assertEquals(ApiError("Internal error"), listed.body<ApiError>())
    }

  @Test
  fun submissionStoreFailureIsInternalError() =
    testApplication {
      val client = start()
      submissions.nextError = IllegalStateException("database is down")

      val responses =
        listOf(
          client.submit(essay()),
          client.get("/api/v1/writing/submissions") { bearerAuth(LEARNER_TOKEN) },
          client.get("/api/v1/writing/submissions/${Uuid.random()}") { bearerAuth(LEARNER_TOKEN) },
        )

      assertEquals(List(3) { HttpStatusCode.InternalServerError }, responses.map { it.status })
      assertEquals(ApiError("Internal error"), responses.last().body<ApiError>())
    }

  @Test
  fun privateImageIsStreamedOnlyToTheOwner() =
    testApplication {
      val client = start()
      contentDir
        .resolve("images/${privatePrompt.id}")
        .createDirectories()
        .resolve("chart.png")
        .writeText("fake png bytes")

      val learnerResponse = client.image("chart.png", LEARNER_TOKEN)
      val ownerResponse = client.image("chart.png", OWNER_TOKEN)
      val missing = client.image("table.png", OWNER_TOKEN)

      assertEquals(HttpStatusCode.NotFound, learnerResponse.status)
      assertEquals("fake png bytes", ownerResponse.bodyAsText())
      assertEquals(ContentType.Image.PNG, ownerResponse.contentType()?.withoutParameters())
      assertEquals("private, max-age=3600", ownerResponse.headers[HttpHeaders.CacheControl])
      assertEquals(ApiError("Writing image not found"), missing.body<ApiError>())
    }

  private companion object {
    const val OWNER_EMAIL = "owner@example.com"
    const val OWNER_TOKEN = "owner-token"
    const val LEARNER_TOKEN = "learner-token"
    const val ESSAY = "Parks give well-known health benefits to 1,000s of residents - every day."
  }
}
