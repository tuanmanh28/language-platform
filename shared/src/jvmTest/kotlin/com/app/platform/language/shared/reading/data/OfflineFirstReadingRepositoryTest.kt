package com.app.platform.language.shared.reading.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.app.platform.language.core.exam.ReadingScorer
import com.app.platform.language.core.model.BundledReadingTests
import com.app.platform.language.core.model.ContentJson
import com.app.platform.language.core.model.ReadingError
import com.app.platform.language.shared.db.LanguagePlatformDatabase
import com.app.platform.language.shared.reading.fake.FakeReadingApi
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.getError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock
import kotlin.time.Instant

class OfflineFirstReadingRepositoryTest {
  private val bundled = BundledReadingTests.all.first()
  private val remoteOnly = bundled.copy(id = "remote-only")
  private val api = FakeReadingApi()
  private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { LanguagePlatformDatabase.Schema.create(it) }
  private val queries = LanguagePlatformDatabase(driver).languagePlatformQueries
  private val clock =
    object : Clock {
      override fun now(): Instant = Instant.fromEpochMilliseconds(0)
    }
  private val repository =
    OfflineFirstReadingRepository(api, ReadingDao(queries, ContentJson, Dispatchers.Unconfined), clock)

  private fun dropTable(name: String) {
    driver.execute(identifier = null, sql = "DROP TABLE $name", parameters = 0)
  }

  @Test
  fun onlineCatalogComesFromTheNetwork() =
    runTest {
      api.tests = listOf(remoteOnly)

      assertEquals(ReadingTestCatalog(listOf(remoteOnly.toSummary()), isOffline = false), repository.getTests())
    }

  @Test
  fun offlineCatalogWithoutCacheFallsBackToBundledTests() =
    runTest {
      api.nextError = ReadingError.Offline

      assertEquals(
        ReadingTestCatalog(BundledReadingTests.all.map { it.toSummary() }, isOffline = true),
        repository.getTests(),
      )
    }

  @Test
  fun testFetchedOnlineIsServedFromCacheWhenOffline() =
    runTest {
      api.tests = listOf(remoteOnly)
      assertEquals(Ok(remoteOnly), repository.getTest(remoteOnly.id))
      api.nextError = ReadingError.Offline

      assertEquals(Ok(remoteOnly), repository.getTest(remoteOnly.id))
      assertEquals(remoteOnly.toSummary(), repository.getTests().tests.first())
    }

  @Test
  fun failedCacheWriteStillReturnsTheDownloadedTest() =
    runTest {
      api.tests = listOf(remoteOnly)
      dropTable("CachedReadingTest")

      assertEquals(Ok(remoteOnly), repository.getTest(remoteOnly.id))
    }

  @Test
  fun failedCacheReadStillServesTheBundledTest() =
    runTest {
      api.nextError = ReadingError.Offline
      dropTable("CachedReadingTest")

      assertEquals(Ok(bundled), repository.getTest(bundled.id))
    }

  @Test
  fun failedCacheReadStillServesTheBundledCatalog() =
    runTest {
      api.nextError = ReadingError.Offline
      dropTable("CachedReadingTest")

      assertEquals(
        ReadingTestCatalog(BundledReadingTests.all.map { it.toSummary() }, isOffline = true),
        repository.getTests(),
      )
    }

  @Test
  fun corruptedCachedTestFallsBackToBundledTests() =
    runTest {
      queries.upsertTest(id = bundled.id, json = "{not json", updatedAt = 0)
      api.nextError = ReadingError.Offline

      assertEquals(Ok(bundled), repository.getTest(bundled.id))
      assertEquals(
        ReadingTestCatalog(BundledReadingTests.all.map { it.toSummary() }, isOffline = true),
        repository.getTests(),
      )
    }

  @Test
  fun bundledTestIsServedWhenOffline() =
    runTest {
      api.nextError = ReadingError.Offline

      assertEquals(Ok(bundled), repository.getTest(bundled.id))
    }

  @Test
  fun unknownTestOfflineReturnsTheNetworkError() =
    runTest {
      api.nextError = ReadingError.Offline

      assertEquals(Err(ReadingError.Offline), repository.getTest("does-not-exist"))
    }

  @Test
  fun unknownTestOnlineReturnsNotFound() =
    runTest {
      assertEquals(Err(ReadingError.NotFound), repository.getTest("does-not-exist"))
    }

  @Test
  fun savedAttemptIsStoredLocally() =
    runTest {
      assertEquals(Ok(Unit), repository.saveAttempt(ReadingScorer.score(bundled, emptyMap()), emptyMap()))

      assertEquals(1, queries.countAttempts(bundled.id).executeAsOne())
    }

  @Test
  fun failedAttemptWriteReturnsUnexpected() =
    runTest {
      dropTable("ReadingAttempt")

      val saved = repository.saveAttempt(ReadingScorer.score(bundled, emptyMap()), emptyMap())

      assertIs<ReadingError.Unexpected>(saved.getError())
    }
}
