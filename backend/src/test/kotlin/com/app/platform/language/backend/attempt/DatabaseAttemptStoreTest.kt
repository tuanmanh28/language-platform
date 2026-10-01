package com.app.platform.language.backend.attempt

import com.app.platform.language.backend.auth.AuthIdentity
import com.app.platform.language.backend.database.AppDatabase
import com.app.platform.language.backend.database.PostgresTestDatabase
import com.app.platform.language.backend.reading.ReadingContentSeeder
import com.app.platform.language.backend.user.DatabaseUserStore
import com.app.platform.language.core.model.Attempt
import com.app.platform.language.core.model.BundledReadingTests
import com.github.michaelbull.result.getOrElse
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import kotlin.io.path.Path
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.fail
import kotlin.time.Instant

class DatabaseAttemptStoreTest {
  private val sample = BundledReadingTests.all.first()
  private val learner = AuthIdentity(uid = "firebase-uid-1", email = "learner@example.com", displayName = "Lan")
  private val users = DatabaseUserStore(database)
  private val store = DatabaseAttemptStore(database)
  private val createdAt = Instant.parse("2026-09-30T08:15:00.123456Z")

  private fun newAttempt(clientId: String = "client-1") =
    NewAttempt(
      clientId = clientId,
      testId = sample.id,
      answers = mapOf("q1" to "TRUE", "q2" to "the river"),
      correctCount = 25,
      totalQuestions = 40,
      band = 6.5,
      createdAt = createdAt,
    )

  @BeforeTest
  fun seedContent() =
    runTest {
      PostgresTestDatabase.clean()
      ReadingContentSeeder(database).seed(readingContentDir).getOrElse { fail(it.message) }
    }

  @Test
  fun savedAttemptRoundTripsAndCanBeFoundByClientId() =
    runTest {
      val userId = users.upsert(learner).id

      val saved = store.save(userId, newAttempt())

      val expected = newAttempt()
      assertEquals(
        SubmittedAttempt(
          Attempt(
            id = saved.attempt.id,
            clientId = expected.clientId,
            testId = expected.testId,
            correctCount = expected.correctCount,
            totalQuestions = expected.totalQuestions,
            band = expected.band,
            answers = expected.answers,
            createdAt = expected.createdAt,
            syncedAt = saved.attempt.syncedAt,
          ),
          isNew = true,
        ),
        saved,
      )
      assertEquals(saved.attempt, store.find(userId, "client-1"))
      assertNull(store.find(userId, "client-2"))
    }

  @Test
  fun savingTheSameClientIdAgainKeepsTheFirstAttemptAndIsNotNew() =
    runTest {
      val userId = users.upsert(learner).id

      val first = store.save(userId, newAttempt())
      val second = store.save(userId, newAttempt().copy(correctCount = 0, band = 0.0))

      assertEquals(SubmittedAttempt(first.attempt, isNew = false), second)
      assertEquals(listOf(first.attempt), store.list(userId, since = null, after = null, limit = 10))
    }

  @Test
  fun sameClientIdFromAnotherUserIsAnotherAttempt() =
    runTest {
      val userId = users.upsert(learner).id
      val otherId = users.upsert(learner.copy(uid = "firebase-uid-2")).id

      val own = store.save(userId, newAttempt()).attempt
      val other = store.save(otherId, newAttempt()).attempt

      assertNotEquals(own.id, other.id)
      assertEquals(listOf(own), store.list(userId, since = null, after = null, limit = 10))
      assertEquals(listOf(other), store.list(otherId, since = null, after = null, limit = 10))
    }

  @Test
  fun listIsNewestFirstWithinSinceAndCursorBounds() =
    runTest {
      val userId = users.upsert(learner).id
      val (a, b, c, d) = listOf("a", "b", "c", "d").map { store.save(userId, newAttempt(it)).attempt }

      val all = store.list(userId, since = null, after = null, limit = 10)
      val limited = store.list(userId, since = null, after = null, limit = 2)
      val bounded = store.list(userId, since = a.syncedAt, after = AttemptCursor.after(d), limit = 10)

      assertEquals(listOf(d, c, b, a), all)
      assertEquals(listOf(d, c), limited)
      assertEquals(listOf(c, b), bounded)
    }

  @Test
  fun cursorPagesThroughAttemptsThatShareASyncTime() =
    runTest {
      val userId = users.upsert(learner).id
      listOf("a", "b", "c", "d", "e").forEach { store.save(userId, newAttempt(it)) }
      database.tx { exec("UPDATE reading_attempts SET synced_at = '2026-10-01T10:00:00Z'") }
      val all = store.list(userId, since = null, after = null, limit = 10)

      val pages = mutableListOf(store.list(userId, since = null, after = null, limit = 2))
      while (pages.last().size == 2) {
        pages += store.list(userId, since = null, after = AttemptCursor.after(pages.last().last()), limit = 2)
      }

      assertEquals(listOf(2, 2, 1), pages.map { it.size })
      assertEquals(all, pages.flatten())
      assertEquals(all.sortedByDescending { it.id }, all)
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
