package com.app.platform.language.backend.listening

import com.app.platform.language.backend.database.AppDatabase
import com.app.platform.language.backend.database.PostgresTestDatabase
import com.app.platform.language.core.model.BundledListeningTests
import com.app.platform.language.core.model.ContentJson
import com.app.platform.language.core.model.ListeningTest
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.core.BooleanColumnType
import org.jetbrains.exposed.v1.core.IntegerColumnType
import org.jetbrains.exposed.v1.core.TextColumnType
import org.junit.jupiter.api.BeforeAll
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DatabaseListeningContentStoreTest {
  private val sample = BundledListeningTests.all.first()
  private val store = DatabaseListeningContentStore(database)

  @BeforeTest
  fun cleanTables() = runTest { PostgresTestDatabase.clean() }

  @Test
  fun publishedTestsAreListedInIdOrderWithVersions() =
    runTest {
      insert(sample.copy(id = "b-test"), version = 2, isPublished = true)
      insert(sample.copy(id = "a-test"), version = 1, isPublished = true)

      assertEquals(
        listOf(StoredListeningTest(sample.copy(id = "a-test"), 1), StoredListeningTest(sample.copy(id = "b-test"), 2)),
        store.listeningTests(),
      )
    }

  @Test
  fun unpublishedTestsAreNotListed() =
    runTest {
      insert(sample, version = 1, isPublished = false)

      assertEquals(emptyList(), store.listeningTests())
    }

  @Test
  fun publishedTestIsFoundById() =
    runTest {
      insert(sample, version = 5, isPublished = true)

      assertEquals(StoredListeningTest(sample, 5), store.listeningTest(sample.id))
    }

  @Test
  fun unpublishedTestIsNotFound() =
    runTest {
      insert(sample, version = 1, isPublished = false)

      assertNull(store.listeningTest(sample.id))
    }

  @Test
  fun unknownTestIsNotFound() =
    runTest {
      assertNull(store.listeningTest("missing"))
    }

  private suspend fun insert(
    test: ListeningTest,
    version: Int,
    isPublished: Boolean,
  ) {
    database.tx {
      exec(
        "INSERT INTO listening_tests (id, title, content, version, published) VALUES (?, ?, CAST(? AS JSONB), ?, ?)",
        listOf(
          TextColumnType() to test.id,
          TextColumnType() to test.title,
          TextColumnType() to ContentJson.encodeToString(ListeningTest.serializer(), test),
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
