package com.app.platform.language.backend.reading

import java.nio.file.Path

sealed interface SeedError {
  val message: String
  val cause: Throwable

  data class UnreadableDirectory(
    val directory: Path,
    override val cause: Throwable,
  ) : SeedError {
    override val message = "Cannot read content directory $directory"
  }

  data class InvalidContent(
    val file: Path,
    override val cause: Throwable,
  ) : SeedError {
    override val message = "Invalid reading test in $file"
  }

  data class WriteFailed(
    override val cause: Throwable,
  ) : SeedError {
    override val message = "Writing reading tests to the database failed"
  }
}
