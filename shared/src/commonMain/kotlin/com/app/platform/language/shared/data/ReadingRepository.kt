package com.app.platform.language.shared.data

import co.touchlab.kermit.Logger
import com.app.platform.language.core.model.BundledReadingTests
import com.app.platform.language.core.model.ReadingResult
import com.app.platform.language.core.model.ReadingTest
import com.app.platform.language.core.model.ReadingTestSummary
import com.app.platform.language.shared.db.LanguagePlatformDatabase
import com.app.platform.language.shared.network.ReadingApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Where data came from — the UI uses it to show an "offline" label. */
enum class DataSource { NETWORK, CACHE, BUNDLED }

data class TestListResult(
  val tests: List<ReadingTestSummary>,
  val source: DataSource,
)

class TestNotFoundException(
  id: String,
) : Exception("Không tìm thấy đề $id")

/**
 * Offline-first: try the network, then the SQLite cache, then the bundled tests.
 */
class ReadingRepository(
  private val api: ReadingApi,
  private val database: LanguagePlatformDatabase,
  private val json: Json,
  private val now: () -> Long = ::currentTimeMillis,
) {
  private val log = Logger.withTag("ReadingRepository")
  private val queries get() = database.languagePlatformQueries

  suspend fun loadTests(): TestListResult {
    try {
      val remote = api.listTests()
      return TestListResult(remote, DataSource.NETWORK)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      log.w(e) { "listTests failed, falling back to offline data" }
    }

    val cached =
      withContext(Dispatchers.Default) {
        queries.selectAllTests().executeAsList().mapNotNull(::decodeTestOrNull)
      }
    val merged = (cached + BundledReadingTests.all).distinctBy { it.id }
    return TestListResult(
      tests = merged.map { it.toSummary() },
      source = if (cached.isNotEmpty()) DataSource.CACHE else DataSource.BUNDLED,
    )
  }

  suspend fun getTest(id: String): ReadingTest {
    try {
      val remote = api.getTest(id)
      // If caching fails, still return the downloaded test instead of falling back to offline data.
      try {
        withContext(Dispatchers.Default) {
          queries.upsertTest(remote.id, json.encodeToString(ReadingTest.serializer(), remote), now())
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        log.w(e) { "Could not cache test $id" }
      }
      return remote
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      log.w(e) { "getTest($id) failed, falling back to offline data" }
    }

    val cached =
      withContext(Dispatchers.Default) {
        queries.selectTest(id).executeAsOneOrNull()?.let(::decodeTestOrNull)
      }
    return cached ?: BundledReadingTests.find(id) ?: throw TestNotFoundException(id)
  }

  suspend fun saveAttempt(
    result: ReadingResult,
    answers: Map<String, String>,
  ) {
    withContext(Dispatchers.Default) {
      queries.insertAttempt(
        testId = result.testId,
        correctCount = result.correctCount.toLong(),
        totalQuestions = result.totalQuestions.toLong(),
        band = result.band,
        answersJson = json.encodeToString(answersSerializer, answers),
        createdAt = now(),
      )
    }
  }

  suspend fun attemptCount(testId: String): Long =
    withContext(Dispatchers.Default) {
      queries.countAttempts(testId).executeAsOne()
    }

  private fun decodeTestOrNull(raw: String): ReadingTest? =
    runCatching { json.decodeFromString(ReadingTest.serializer(), raw) }
      .onFailure { log.w(it) { "Corrupted cached test ignored" } }
      .getOrNull()

  private companion object {
    val answersSerializer = MapSerializer(String.serializer(), String.serializer())
  }
}

@OptIn(ExperimentalTime::class)
internal fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
