package com.app.platform.language.backend.content

import com.app.platform.language.backend.common.SeedError
import com.app.platform.language.backend.database.AppDatabase
import com.app.platform.language.backend.database.PostgresTestDatabase
import com.app.platform.language.core.model.BundledListeningTests
import com.app.platform.language.core.model.BundledReadingTests
import com.app.platform.language.core.model.BundledWritingPrompts
import com.app.platform.language.core.model.ContentJson
import com.app.platform.language.core.model.ListeningTest
import com.app.platform.language.core.model.ReadingTest
import com.app.platform.language.core.model.WritingPrompt
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ContentSeederTest {
  private val seeder = ContentSeeder(database)
  private val ownerReading = BundledReadingTests.all.first().copy(id = "owner-reading-01", title = "Owner reading")
  private val ownerListening =
    BundledListeningTests.all.first().let { sample ->
      sample.copy(
        id = "owner-listening-01",
        title = "Owner listening",
        sections = sample.sections.map { it.copy(audioUrl = "owner-listening-01/section-${it.number}.mp3") },
      )
    }
  private val ownerWriting = BundledWritingPrompts.all.first().copy(id = "owner-writing-01", imageUrl = null)
  private val nothing = SeededCounts(reading = 0, listening = 0, writing = 0)
  private val oneOfEach = SeededCounts(reading = 1, listening = 1, writing = 1)

  @TempDir
  lateinit var tempDir: Path

  private val publicDir by lazy { tempDir.resolve("public").createDirectories() }
  private val privateDir by lazy { tempDir.resolve("private").createDirectories() }

  @BeforeTest
  fun cleanTables() = runTest { PostgresTestDatabase.clean() }

  @Test
  fun eachDirectoryIsStoredWithItsVisibility() =
    runTest {
      writeContent(
        publicDir,
        ownerReading.copy(id = "public-reading-01"),
        ownerListening.copy(id = "public-listening-01"),
        ownerWriting.copy(id = "public-writing-01"),
      )
      writeContent(privateDir, ownerReading, ownerListening)

      assertEquals(Ok(SeededContent(public = oneOfEach, private = oneOfEach)), seeder.seed(publicDir, privateDir))
      assertEquals(
        listOf(
          StoredRow("listening_tests", ownerListening.id, 1, Visibility.PRIVATE.id),
          StoredRow("listening_tests", "public-listening-01", 1, Visibility.PUBLIC.id),
          StoredRow("reading_tests", ownerReading.id, 1, Visibility.PRIVATE.id),
          StoredRow("reading_tests", "public-reading-01", 1, Visibility.PUBLIC.id),
          StoredRow("writing_prompts", ownerWriting.id, 1, Visibility.PRIVATE.id),
          StoredRow("writing_prompts", "public-writing-01", 1, Visibility.PUBLIC.id),
        ),
        storedRows(),
      )
    }

  @Test
  fun seedingTheSameDirectoriesTwiceIsIdempotent() =
    runTest {
      writeContent(privateDir, ownerReading, ownerListening)
      val seeded = Ok(SeededContent(public = nothing, private = oneOfEach))
      assertEquals(seeded, seeder.seed(publicDir, privateDir))
      val afterFirstSeed = storedRows()

      assertEquals(seeded, seeder.seed(publicDir, privateDir))

      assertEquals(afterFirstSeed, storedRows())
    }

  @Test
  fun movingATestToTheContentDirBumpsItsVersion() =
    runTest {
      writeContent(publicDir, ownerReading, ownerListening)
      assertEquals(Ok(SeededContent(public = oneOfEach, private = nothing)), seeder.seed(publicDir, privateDir))
      publicDir.toFile().deleteRecursively()
      writeContent(privateDir, ownerReading, ownerListening)

      assertEquals(Ok(SeededContent(public = nothing, private = oneOfEach)), seeder.seed(publicDir, privateDir))

      assertEquals(List(3) { 2 to "private" }, storedRows().map { it.version to it.visibility })
    }

  @Test
  fun testInBothDirectoriesIsRejected() =
    runTest {
      writeContent(publicDir, ownerReading, ownerListening.copy(id = "public-listening-01"))
      writeContent(privateDir, ownerReading, ownerListening)

      assertEquals(Err(SeedError.PublicAndPrivateContent(ownerReading.id)), seeder.seed(publicDir, privateDir))
      assertEquals(emptyList(), storedRows())
    }

  @Test
  fun privateListeningWithAnUnservableAudioPathIsRejected() =
    runTest {
      val nested = ownerListening.sections.first().copy(audioUrl = "listening/owner-listening-01/section-1.mp3")
      writeContent(privateDir, ownerReading, ownerListening.copy(sections = listOf(nested)))

      assertEquals(
        Err(SeedError.InvalidPrivateAudioPath(ownerListening.id, nested.audioUrl)),
        seeder.seed(publicDir, privateDir),
      )
      assertEquals(emptyList(), storedRows())
    }

  @Test
  fun privateWritingPromptWithAnUnservableImagePathIsRejectedBeforeAnyWrite() =
    runTest {
      val prompt = ownerWriting.copy(imageUrl = "writing/owner-writing-01/chart.png")
      writeContent(
        publicDir,
        ownerReading.copy(id = "public-reading-01"),
        ownerListening.copy(id = "public-listening-01"),
        ownerWriting.copy(id = "public-writing-01"),
      )
      writeContent(privateDir, ownerReading, ownerListening, prompt)

      assertEquals(
        Err(SeedError.InvalidPrivateImagePath(prompt.id, "writing/owner-writing-01/chart.png")),
        seeder.seed(publicDir, privateDir),
      )
      assertEquals(emptyList(), storedRows())
    }

  @Test
  fun writingPromptInBothDirectoriesIsRejected() =
    runTest {
      writeContent(
        publicDir,
        ownerReading.copy(id = "public-reading-01"),
        ownerListening.copy(id = "public-listening-01"),
        ownerWriting,
      )
      writeContent(privateDir, ownerReading, ownerListening, ownerWriting)

      assertEquals(Err(SeedError.PublicAndPrivateContent(ownerWriting.id)), seeder.seed(publicDir, privateDir))
      assertEquals(emptyList(), storedRows())
    }

  @Test
  fun missingSubfoldersSeedNothing() =
    runTest {
      val seeded = Ok(SeededContent(public = nothing, private = nothing))

      assertEquals(seeded, seeder.seed(publicDir, privateDir))
      assertEquals(seeded, seeder.seed(tempDir.resolve("missing"), tempDir.resolve("also-missing")))
    }

  private fun writeContent(
    contentDir: Path,
    reading: ReadingTest,
    listening: ListeningTest,
    writing: WritingPrompt = ownerWriting,
  ) {
    contentDir
      .resolve("reading")
      .createDirectories()
      .resolve("${reading.id}.json")
      .writeText(ContentJson.encodeToString(ReadingTest.serializer(), reading))
    contentDir
      .resolve("listening")
      .createDirectories()
      .resolve("${listening.id}.json")
      .writeText(ContentJson.encodeToString(ListeningTest.serializer(), listening))
    contentDir
      .resolve("writing")
      .createDirectories()
      .resolve("${writing.id}.json")
      .writeText(ContentJson.encodeToString(WritingPrompt.serializer(), writing))
  }

  private suspend fun storedRows(): List<StoredRow> =
    database.tx {
      exec(
        "SELECT 'listening_tests' AS t, id, version, visibility FROM listening_tests " +
          "UNION ALL SELECT 'reading_tests', id, version, visibility FROM reading_tests " +
          "UNION ALL SELECT 'writing_prompts', id, version, visibility FROM writing_prompts ORDER BY t, id",
      ) { rows ->
        buildList {
          while (rows.next()) {
            add(
              StoredRow(
                table = rows.getString("t"),
                id = rows.getString("id"),
                version = rows.getInt("version"),
                visibility = rows.getString("visibility"),
              ),
            )
          }
        }
      }.orEmpty()
    }

  private data class StoredRow(
    val table: String,
    val id: String,
    val version: Int,
    val visibility: String,
  )

  companion object {
    private lateinit var database: AppDatabase

    @JvmStatic
    @BeforeAll
    fun connectDatabase() {
      database = PostgresTestDatabase.connect()
    }
  }
}
