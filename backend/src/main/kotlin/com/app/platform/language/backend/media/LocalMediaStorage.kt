package com.app.platform.language.backend.media

import java.nio.file.Path
import kotlin.io.path.isRegularFile

class LocalMediaStorage(
  private val mediaDir: Path,
  private val apiBaseUrl: String,
  private val routePath: String,
) : MediaStorage {
  override fun urlFor(path: String): String = "$apiBaseUrl$routePath/$path"

  fun file(
    contentId: String,
    fileName: String,
  ): Path? {
    if (!isPrivateMediaPath("$contentId/$fileName")) return null
    val file = mediaDir.resolve(contentId).resolve(fileName)
    return file.takeIf { it.isRegularFile() && it.toRealPath().startsWith(mediaDir.toRealPath()) }
  }
}
