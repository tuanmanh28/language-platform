package com.app.platform.language.backend.audio

import java.nio.file.Path
import kotlin.io.path.isRegularFile

class LocalAudioStorage(
  private val audioDir: Path,
  private val apiBaseUrl: String,
) : AudioStorage {
  override fun urlFor(path: String): String = "$apiBaseUrl$LOCAL_AUDIO_PATH/$path"

  fun file(
    testId: String,
    fileName: String,
  ): Path? {
    if (!isPrivateAudioPath("$testId/$fileName")) return null
    val file = audioDir.resolve(testId).resolve(fileName)
    return file.takeIf { it.isRegularFile() && it.toRealPath().startsWith(audioDir.toRealPath()) }
  }
}
