package com.app.platform.language.backend.media

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

class PrivateMediaService(
  private val storage: LocalMediaStorage,
  private val access: ContentAccessPolicy,
) {
  // Every media file under CONTENT_DIR belongs to private content, so only owners may load it.
  suspend fun file(
    viewer: AuthIdentity?,
    contentId: String,
    fileName: String,
  ): Result<Path, MediaError> {
    if (!access.isOwner(viewer)) return Err(MediaError.NotFound)
    return runSuspendCatching { withContext(Dispatchers.IO) { storage.file(contentId, fileName) } }
      .mapError(MediaError::Unexpected)
      .andThen { file -> file.toResultOr { MediaError.NotFound } }
  }
}
