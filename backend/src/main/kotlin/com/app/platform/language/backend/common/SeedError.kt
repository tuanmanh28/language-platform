package com.app.platform.language.backend.common

import java.nio.file.Path

sealed interface SeedError {
  val message: String
  val cause: Throwable?

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
    override val message = "Invalid content in $file"
  }

  data class InvalidPrivateAudioPath(
    val testId: String,
    val audioUrl: String,
  ) : SeedError {
    override val cause = null
    override val message = "Private test $testId has audioUrl '$audioUrl'; expected <test-id>/<file name>"
  }

  data class InvalidPrivateImagePath(
    val promptId: String,
    val imageUrl: String,
  ) : SeedError {
    override val cause = null
    override val message = "Private prompt $promptId has imageUrl '$imageUrl'; expected <folder>/<file name>"
  }

  data class PublicAndPrivateContent(
    val contentId: String,
  ) : SeedError {
    override val cause = null
    override val message = "Content $contentId exists in both the public content and CONTENT_DIR"
  }

  data class WriteFailed(
    override val cause: Throwable,
  ) : SeedError {
    override val message = "Writing content to the database failed"
  }
}
