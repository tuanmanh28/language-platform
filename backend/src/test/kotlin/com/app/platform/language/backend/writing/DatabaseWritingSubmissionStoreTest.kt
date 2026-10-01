package com.app.platform.language.backend.writing

import com.app.platform.language.backend.auth.AuthIdentity
import com.app.platform.language.backend.content.Visibility
import com.app.platform.language.backend.database.AppDatabase
import com.app.platform.language.backend.database.PostgresTestDatabase
import com.app.platform.language.backend.user.DatabaseUserStore
import com.app.platform.language.core.model.BundledWritingPrompts
import com.app.platform.language.core.model.WritingSubmission
import com.app.platform.language.core.model.WritingSubmissionStatus
import com.github.michaelbull.result.getOrElse
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import kotlin.io.path.Path
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.fail
import kotlin.uuid.Uuid

class DatabaseWritingSubmissionStoreTest {
  private val prompt = BundledWritingPrompts.all.first()
  private val learner = AuthIdentity(uid = "firebase-uid-1", email = "learner@example.com", displayName = "Lan")
  private val users = DatabaseUserStore(database)
  private val store = DatabaseWritingSubmissionStore(database)

  private fun newSubmission(text: String = "Parks matter.") = NewWritingSubmission(prompt.id, text, wordCount = 2)

  @BeforeTest
  fun seedContent() =
    runTest {
      PostgresTestDatabase.clean()
      WritingContentSeeder(database).seed(writingContentDir, Visibility.PUBLIC).getOrElse { fail(it.message) }
    }

  @Test
  fun savedSubmissionIsPendingAndCanBeFoundById() =
    runTest {
      val userId = users.upsert(learner).id

      val saved = store.save(userId, newSubmission())

      assertEquals(
        WritingSubmission(
          id = saved.id,
          promptId = prompt.id,
          text = "Parks matter.",
          wordCount = 2,
          submittedAt = saved.submittedAt,
          status = WritingSubmissionStatus.PENDING,
        ),
        saved,
      )
      assertEquals(saved, store.find(userId, Uuid.parse(saved.id)))
      assertNull(store.find(userId, Uuid.random()))
    }

  @Test
  fun listIsNewestFirst() =
    runTest {
      val userId = users.upsert(learner).id
      val (first, second, third) = listOf("a", "b", "c").map { store.save(userId, newSubmission(it)) }

      assertEquals(listOf(third, second, first), store.list(userId))
    }

  @Test
  fun usersOnlySeeTheirOwnSubmissions() =
    runTest {
      val userId = users.upsert(learner).id
      val otherId = users.upsert(learner.copy(uid = "firebase-uid-2")).id
      val own = store.save(userId, newSubmission())
      val other = store.save(otherId, newSubmission())

      assertEquals(listOf(own), store.list(userId))
      assertEquals(listOf(other), store.list(otherId))
      assertNull(store.find(otherId, Uuid.parse(own.id)))
    }

  companion object {
    private val writingContentDir = Path(System.getProperty("backend.writingContentDir"))
    private lateinit var database: AppDatabase

    @JvmStatic
    @BeforeAll
    fun connectDatabase() {
      database = PostgresTestDatabase.connect()
    }
  }
}
