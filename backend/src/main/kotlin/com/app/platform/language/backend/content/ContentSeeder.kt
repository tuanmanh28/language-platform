package com.app.platform.language.backend.content

import com.app.platform.language.backend.common.SeedError
import com.app.platform.language.backend.database.AppDatabase
import com.app.platform.language.backend.listening.ListeningContentSeeder
import com.app.platform.language.backend.reading.ReadingContentSeeder
import com.app.platform.language.core.model.ListeningTest
import com.app.platform.language.core.model.ReadingTest
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

  suspend fun seed(
    publicDir: Path,
    privateDir: Path,
  ): Result<SeededContent, SeedError> =
    coroutineBinding {
      val publicReading = loadIfPresent(publicDir.resolve(READING_DIR), reading::load).bind()
      val privateReading = loadIfPresent(privateDir.resolve(READING_DIR), reading::load).bind()
      val publicListening = loadIfPresent(publicDir.resolve(LISTENING_DIR), listening::load).bind()
      val privateListening = loadIfPresent(privateDir.resolve(LISTENING_DIR), listening::load).bind()
      ensureDistinct(publicReading.map(ReadingTest::id), privateReading.map(ReadingTest::id)).bind()
      ensureDistinct(publicListening.map(ListeningTest::id), privateListening.map(ListeningTest::id)).bind()
      SeededContent(
        public =
          SeededCounts(
            reading = reading.seed(publicReading, Visibility.PUBLIC).bind(),
            listening = listening.seed(publicListening, Visibility.PUBLIC).bind(),
          ),
        private =
          SeededCounts(
            reading = reading.seed(privateReading, Visibility.PRIVATE).bind(),
            listening = listening.seed(privateListening, Visibility.PRIVATE).bind(),
          ),
      )
    }

  private fun <T> loadIfPresent(
    directory: Path,
    load: (Path) -> Result<List<T>, SeedError>,
  ): Result<List<T>, SeedError> = if (directory.isDirectory()) load(directory) else Ok(emptyList())

  // A test in both places would flip visibility and bump its version on every run.
  private fun ensureDistinct(
    publicIds: List<String>,
    privateIds: List<String>,
  ): Result<Unit, SeedError> {
    val sharedId = privateIds.intersect(publicIds.toSet()).firstOrNull()
    return if (sharedId == null) Ok(Unit) else Err(SeedError.PublicAndPrivateTest(sharedId))
  }

  private companion object {
    const val READING_DIR = "reading"
    const val LISTENING_DIR = "listening"
  }
}

data class SeededContent(
  val public: SeededCounts,
  val private: SeededCounts,
)

data class SeededCounts(
  val reading: Int,
  val listening: Int,
)
