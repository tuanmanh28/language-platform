package com.app.platform.language.backend.reading

import com.app.platform.language.backend.database.AppDatabase
import com.app.platform.language.backend.database.PostgresTestDatabase
import com.app.platform.language.core.model.BundledReadingTests
import com.app.platform.language.core.model.ContentJson
import com.app.platform.language.core.model.ReadingTest
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.core.BooleanColumnType
import org.jetbrains.exposed.v1.core.IntegerColumnType
import org.jetbrains.exposed.v1.core.TextColumnType
import org.junit.jupiter.api.BeforeAll
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DatabaseContentStoreTest {
  private val sample = BundledReadingTests.all.first()
  private val store = DatabaseContentStore(database)

  @BeforeTest
  fun cleanTables() = runTest { PostgresTestDatabase.clean() }

  @Test
  fun publishedTestsAreListedInIdOrderWithVersions() =
    runTest {
      insert(sample.copy(id = "b-test"), version = 2, isPublished = true)
      insert(sample.copy(id = "a-test"), version = 1, isPublished = true)

      val listed = store.readingTests()

      assertEquals(
        listOf(StoredReadingTest(sample.copy(id = "a-test"), 1), StoredReadingTest(sample.copy(id = "b-test"), 2)),
        listed,
      )
    }

  @Test
  fun unpublishedTestsAreNotListed() =
    runTest {
      insert(sample, version = 1, isPublished = false)

      assertEquals(emptyList(), store.readingTests())
    }

  @Test
  fun publishedTestIsFoundById() =
    runTest {
      insert(sample, version = 5, isPublished = true)

      assertEquals(StoredReadingTest(sample, 5), store.readingTest(sample.id))
    }

  @Test
  fun unpublishedTestIsNotFound() =
    runTest {
      insert(sample, version = 1, isPublished = false)

      assertNull(store.readingTest(sample.id))
    }

  @Test
  fun unknownTestIsNotFound() =
    runTest {
      assertNull(store.readingTest("missing"))
    }

  private suspend fun insert(
    test: ReadingTest,
    version: Int,
    isPublished: Boolean,
  ) {
    database.tx {
      exec(
        "INSERT INTO reading_tests (id, module, title, time_limit_minutes, content, version, published) " +
          "VALUES (?, 'academic', ?, ?, CAST(? AS JSONB), ?, ?)",
        listOf(
          TextColumnType() to test.id,
          TextColumnType() to test.title,
          IntegerColumnType() to test.timeLimitMinutes,
          TextColumnType() to ContentJson.encodeToString(ReadingTest.serializer(), test),
          IntegerColumnType() to version,
          BooleanColumnType() to isPublished,
        ),
      )
    }
  }

  companion object {
    private lateinit var database: AppDatabase

    @JvmStatic
    @BeforeAll
    fun connectDatabase() {
      database = PostgresTestDatabase.connect()
    }
  }
}
