package com.app.platform.language.backend.writing

import com.app.platform.language.backend.common.SeedError
import com.app.platform.language.backend.content.Visibility
import com.app.platform.language.backend.database.AppDatabase
import com.app.platform.language.backend.database.PostgresTestDatabase
import com.app.platform.language.core.model.BundledWritingPrompts
import com.app.platform.language.core.model.ContentJson
import com.app.platform.language.core.model.WritingPrompt
import com.github.michaelbull.result.Err
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
import kotlin.test.assertNull

class WritingContentSeederTest {
  private val sample = BundledWritingPrompts.all.first { it.imageUrl != null }
  private val seeder = WritingContentSeeder(database)
  private val contentStore = DatabaseWritingContentStore(database)

  @TempDir
  lateinit var tempDir: Path

  @BeforeTest
  fun cleanTables() = runTest { PostgresTestDatabase.clean() }

  @Test
  fun seededPromptsAreServedByTheDatabaseStore() =
    runTest {
      assertEquals(Ok(BundledWritingPrompts.all.size), seeder.seed(writingContentDir, Visibility.PUBLIC))

      assertEquals(
        BundledWritingPrompts.all.map { StoredWritingPrompt(it, 1, Visibility.PUBLIC) }.sortedBy { it.prompt.id },
        contentStore.writingPrompts().sortedBy { it.prompt.id },
      )
    }

  @Test
  fun seedingTwiceKeepsTheVersion() =
    runTest {
      val seeded = Ok(BundledWritingPrompts.all.size)
      assertEquals(seeded, seeder.seed(writingContentDir, Visibility.PUBLIC))
      assertEquals(seeded, seeder.seed(writingContentDir, Visibility.PUBLIC))

      assertEquals(List(BundledWritingPrompts.all.size) { 1 }, contentStore.writingPrompts().map { it.version })
    }

  @Test
  fun changedPromptBumpsTheVersion() =
    runTest {
      writeContent(sample)
      assertEquals(Ok(1), seeder.seed(tempDir, Visibility.PUBLIC))
      writeContent(sample.copy(minWords = 170))

      assertEquals(Ok(1), seeder.seed(tempDir, Visibility.PUBLIC))

      assertEquals(
        StoredWritingPrompt(sample.copy(minWords = 170), 2, Visibility.PUBLIC),
        contentStore.writingPrompt(sample.id),
      )
    }

  @Test
  fun privatePromptNeedsItsImageAtPromptIdAndFileName() =
    runTest {
      writeContent(sample)

      assertEquals(
        Err(SeedError.InvalidPrivateImagePath(sample.id, checkNotNull(sample.imageUrl))),
        seeder.seed(tempDir, Visibility.PRIVATE),
      )
      assertEquals(emptyList(), contentStore.writingPrompts())
    }

  @Test
  fun privatePromptWithServableImageIsStored() =
    runTest {
      val prompt = sample.copy(imageUrl = "${sample.id}/chart.png")
      writeContent(prompt)

      assertEquals(Ok(1), seeder.seed(tempDir, Visibility.PRIVATE))
      assertEquals(StoredWritingPrompt(prompt, 1, Visibility.PRIVATE), contentStore.writingPrompt(sample.id))
    }

  @Test
  fun unknownOrUnpublishedPromptIsNotServed() =
    runTest {
      writeContent(sample)
      assertEquals(Ok(1), seeder.seed(tempDir, Visibility.PUBLIC))
      database.tx { exec("UPDATE writing_prompts SET published = FALSE") }

      assertNull(contentStore.writingPrompt("missing-prompt"))
      assertNull(contentStore.writingPrompt(sample.id))
      assertEquals(emptyList(), contentStore.writingPrompts())
    }

  @Test
  fun invalidFileIsInvalidContent() =
    runTest {
      tempDir.resolve("broken.json").writeText("{ not a writing prompt")

      assertIs<SeedError.InvalidContent>(seeder.seed(tempDir, Visibility.PUBLIC).getError())
    }

  private fun writeContent(prompt: WritingPrompt) {
    tempDir.resolve("${prompt.id}.json").writeText(ContentJson.encodeToString(WritingPrompt.serializer(), prompt))
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
