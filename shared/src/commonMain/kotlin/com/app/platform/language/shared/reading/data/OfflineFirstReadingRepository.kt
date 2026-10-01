package com.app.platform.language.shared.reading.data

import co.touchlab.kermit.Logger
import com.app.platform.language.core.model.BundledReadingTests
import com.app.platform.language.core.model.ReadingError
import com.app.platform.language.core.model.ReadingResult
import com.app.platform.language.core.model.ReadingTest
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOr
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import com.github.michaelbull.result.orElse
import com.github.michaelbull.result.toResultOr
import kotlin.time.Clock

internal class OfflineFirstReadingRepository(
  private val api: ReadingApi,
  private val dao: ReadingDao,
  private val clock: Clock,
) : ReadingRepository {
  private val log = Logger.withTag("ReadingRepository")

  override suspend fun getTests(): ReadingTestCatalog =
    api.listTests().mapBoth(
      success = { tests -> ReadingTestCatalog(tests, isOffline = false) },
      failure = { error ->
        log.w { "Listing tests failed ($error), showing offline tests" }
        ReadingTestCatalog(findOfflineTests().map { it.toSummary() }, isOffline = true)
      },
    )

  override suspend fun getTest(id: String): Result<ReadingTest, ReadingError> =
    api
      .getTest(id)
      .onOk { test -> cacheTest(test) }
      .orElse { error ->
        log.w { "Loading test $id failed ($error), trying offline tests" }
        findOfflineTest(id).toResultOr { error }
      }

  override suspend fun saveAttempt(
    result: ReadingResult,
    answers: Map<String, String>,
  ): Result<Unit, ReadingError> = dao.insertAttempt(result, answers, createdAt = clock.now())

  // A failed cache write must not hide a test that was downloaded successfully.
  private suspend fun cacheTest(test: ReadingTest) {
    dao
      .saveTest(test, updatedAt = clock.now())
      .onErr { error -> log.w { "Caching test ${test.id} failed ($error)" } }
  }

  private suspend fun findOfflineTests(): List<ReadingTest> {
    val cached =
      dao
        .findAllTests()
        .onErr { error -> log.w { "Reading cached tests failed ($error)" } }
        .getOr(emptyList())
    return (cached + BundledReadingTests.all).distinctBy { it.id }
  }

  private suspend fun findOfflineTest(id: String): ReadingTest? {
    val cached =
      dao
        .findTest(id)
        .onErr { error -> log.w { "Reading cached test $id failed ($error)" } }
        .getOr(null)
    return cached ?: BundledReadingTests.find(id)
  }
}
