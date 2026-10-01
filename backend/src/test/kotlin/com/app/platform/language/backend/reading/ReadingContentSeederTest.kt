package com.app.platform.language.backend.reading

import com.app.platform.language.backend.database.AppDatabase
import com.app.platform.language.backend.database.PostgresTestDatabase
import com.app.platform.language.core.model.BundledReadingTests
import com.app.platform.language.core.model.ContentJson
import com.app.platform.language.core.model.ReadingTest
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.getError
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.writeText
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ReadingContentSeederTest {
  private val sample = BundledReadingTests.all.first()
  private val seeder = ReadingContentSeeder(database)
  private val bundledRows = BundledReadingTests.all.map { StoredRow(it.id, 1, isPublished = true) }.sortedBy { it.id }

  @TempDir
  lateinit var tempDir: Path

  @BeforeTest
  fun cleanTables() = runTest { PostgresTestDatabase.clean() }

  @Test
  fun seedingInsertsEveryContentFileAsPublished() =
    runTest {
      val seeded = seeder.seed(readingContentDir)

      assertEquals(Ok(BundledReadingTests.all.size), seeded)
      assertEquals(bundledRows, storedRows())
    }

  @Test
  fun seedingTwiceCreatesNoDuplicatesAndKeepsVersion() =
    runTest {
      seeder.seed(readingContentDir)
      seeder.seed(readingContentDir)

      assertEquals(bundledRows, storedRows())
    }

  @Test
  fun changedContentBumpsVersion() =
    runTest {
      writeContent(sample)
      seeder.seed(tempDir)
      writeContent(sample.copy(title = "Rooftop Farming, Revised"))

      seeder.seed(tempDir)

      assertEquals(listOf(StoredRow(sample.id, 2, isPublished = true)), storedRows())
      assertEquals("Rooftop Farming, Revised", DatabaseContentStore(database).readingTest(sample.id)?.test?.title)
    }

  @Test
  fun invalidFileIsInvalidContent() =
    runTest {
      tempDir.resolve("broken.json").writeText("{ not a reading test")

      assertIs<SeedError.InvalidContent>(seeder.seed(tempDir).getError())
      assertEquals(emptyList(), storedRows())
    }

  @Test
  fun missingDirectoryIsUnreadable() =
    runTest {
      assertIs<SeedError.UnreadableDirectory>(seeder.seed(tempDir.resolve("missing")).getError())
    }

  private fun writeContent(test: ReadingTest) {
    tempDir.resolve("${test.id}.json").writeText(ContentJson.encodeToString(ReadingTest.serializer(), test))
  }

  private suspend fun storedRows(): List<StoredRow> =
    database.tx {
      exec("SELECT id, version, published FROM reading_tests ORDER BY id") { rows ->
        buildList {
          while (rows.next()) add(StoredRow(rows.getString("id"), rows.getInt("version"), rows.getBoolean("published")))
        }
      }.orEmpty()
    }

  private data class StoredRow(
    val id: String,
    val version: Int,
    val isPublished: Boolean,
  )

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
