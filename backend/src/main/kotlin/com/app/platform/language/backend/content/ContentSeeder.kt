package com.app.platform.language.backend.content

import com.app.platform.language.backend.common.SeedError
import com.app.platform.language.backend.database.AppDatabase
import com.app.platform.language.backend.listening.ListeningContentSeeder
import com.app.platform.language.backend.reading.ReadingContentSeeder
import com.app.platform.language.backend.writing.WritingContentSeeder
import com.app.platform.language.core.model.ListeningTest
import com.app.platform.language.core.model.ReadingTest
import com.app.platform.language.core.model.WritingPrompt
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import java.nio.file.Path
import kotlin.io.path.isDirectory

class ContentSeeder(
  database: AppDatabase,
) {
  private val reading = ReadingContentSeeder(database)
  private val listening = ListeningContentSeeder(database)
  private val writing = WritingContentSeeder(database)

  suspend fun seed(
    publicDir: Path,
    privateDir: Path,
  ): Result<SeededContent, SeedError> =
    coroutineBinding {
      val publicReading = loadIfPresent(publicDir.resolve(READING_DIR), reading::load).bind()
      val privateReading = loadIfPresent(privateDir.resolve(READING_DIR), reading::load).bind()
      val publicListening = loadIfPresent(publicDir.resolve(LISTENING_DIR), listening::load).bind()
      val privateListening = loadIfPresent(privateDir.resolve(LISTENING_DIR), listening::load).bind()
      val publicWriting = loadIfPresent(publicDir.resolve(WRITING_DIR), writing::load).bind()
      val privateWriting = loadIfPresent(privateDir.resolve(WRITING_DIR), writing::load).bind()
      ensureDistinct(publicReading.map(ReadingTest::id), privateReading.map(ReadingTest::id)).bind()
      ensureDistinct(publicListening.map(ListeningTest::id), privateListening.map(ListeningTest::id)).bind()
      ensureDistinct(publicWriting.map(WritingPrompt::id), privateWriting.map(WritingPrompt::id)).bind()
      listening.validatePrivate(privateListening).bind()
      writing.validatePrivate(privateWriting).bind()
      SeededContent(
        public =
          SeededCounts(
            reading = reading.seed(publicReading, Visibility.PUBLIC).bind(),
            listening = listening.seed(publicListening, Visibility.PUBLIC).bind(),
            writing = writing.seed(publicWriting, Visibility.PUBLIC).bind(),
          ),
        private =
          SeededCounts(
            reading = reading.seed(privateReading, Visibility.PRIVATE).bind(),
            listening = listening.seed(privateListening, Visibility.PRIVATE).bind(),
            writing = writing.seed(privateWriting, Visibility.PRIVATE).bind(),
          ),
      )
    }

  private fun <T> loadIfPresent(
    directory: Path,
    load: (Path) -> Result<List<T>, SeedError>,
  ): Result<List<T>, SeedError> = if (directory.isDirectory()) load(directory) else Ok(emptyList())

  // Content in both places would flip visibility and bump its version on every run.
  private fun ensureDistinct(
    publicIds: List<String>,
    privateIds: List<String>,
  ): Result<Unit, SeedError> {
    val sharedId = privateIds.intersect(publicIds.toSet()).firstOrNull()
    return if (sharedId == null) Ok(Unit) else Err(SeedError.PublicAndPrivateContent(sharedId))
  }

  private companion object {
    const val READING_DIR = "reading"
    const val LISTENING_DIR = "listening"
    const val WRITING_DIR = "writing"
  }
}

data class SeededContent(
  val public: SeededCounts,
  val private: SeededCounts,
)

data class SeededCounts(
  val reading: Int,
  val listening: Int,
  val writing: Int,
)
