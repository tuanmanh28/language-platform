package com.app.platform.language.shared.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.app.platform.language.core.model.BundledReadingTests
import com.app.platform.language.core.model.ContentJson
import com.app.platform.language.core.model.ReadingResult
import com.app.platform.language.core.model.ReadingTest
import com.app.platform.language.core.model.ReadingTestSummary
import com.app.platform.language.core.model.SubmitAnswersRequest
import com.app.platform.language.shared.db.LanguagePlatformDatabase
import com.app.platform.language.shared.network.ReadingApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReadingRepositoryTest {
    private val sample = BundledReadingTests.all.first()

    private fun database(): LanguagePlatformDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LanguagePlatformDatabase.Schema.create(driver)
        return LanguagePlatformDatabase(driver)
    }

    private class FakeApi(
        private val online: Boolean,
        private val test: ReadingTest,
    ) : ReadingApi {
        override suspend fun listTests(): List<ReadingTestSummary> =
            if (online) listOf(test.toSummary()) else error("offline")

        override suspend fun getTest(id: String): ReadingTest = if (online && id == test.id) test else error("offline")

        override suspend fun submit(
            id: String,
            request: SubmitAnswersRequest,
        ): ReadingResult = error("unused")
    }

    @Test
    fun onlineListComesFromNetwork() =
        runTest {
            val repo = ReadingRepository(FakeApi(online = true, test = sample), database(), ContentJson) { 0L }
            assertEquals(DataSource.NETWORK, repo.loadTests().source)
        }

    @Test
    fun offlineWithoutCacheFallsBackToBundledContent() =
        runTest {
            val repo = ReadingRepository(FakeApi(online = false, test = sample), database(), ContentJson) { 0L }
            val result = repo.loadTests()
            assertEquals(DataSource.BUNDLED, result.source)
            assertEquals(sample.id, repo.getTest(sample.id).id)
        }

    @Test
    fun testFetchedOnlineIsServedFromCacheWhenOffline() =
        runTest {
            val db = database()
            val remoteOnly = sample.copy(id = "remote-only")
            ReadingRepository(FakeApi(online = true, test = remoteOnly), db, ContentJson) { 0L }.getTest("remote-only")

            val offline = ReadingRepository(FakeApi(online = false, test = remoteOnly), db, ContentJson) { 0L }
            assertEquals("remote-only", offline.getTest("remote-only").id)
            assertEquals(DataSource.CACHE, offline.loadTests().source)
        }

    @Test
    fun unknownTestThrows() =
        runTest {
            val repo = ReadingRepository(FakeApi(online = false, test = sample), database(), ContentJson) { 0L }
            assertFailsWith<TestNotFoundException> { repo.getTest("does-not-exist") }
        }
}
