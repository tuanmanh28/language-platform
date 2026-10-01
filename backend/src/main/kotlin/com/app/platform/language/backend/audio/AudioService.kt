package com.app.platform.language.backend.audio

import com.app.platform.language.backend.auth.AuthIdentity
import com.app.platform.language.backend.content.ContentAccessPolicy
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.toResultOr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path

class AudioService(
  private val storage: LocalAudioStorage,
  private val access: ContentAccessPolicy,
) {
  // Every file under CONTENT_DIR/audio belongs to private content, so only owners may stream it.
  suspend fun audioFile(
    viewer: AuthIdentity?,
    testId: String,
    fileName: String,
  ): Result<Path, AudioError> {
    if (!access.isOwner(viewer)) return Err(AudioError.NotFound)
    return runSuspendCatching { withContext(Dispatchers.IO) { storage.file(testId, fileName) } }
      .mapError(AudioError::Unexpected)
      .andThen { file -> file.toResultOr { AudioError.NotFound } }
  }
}
