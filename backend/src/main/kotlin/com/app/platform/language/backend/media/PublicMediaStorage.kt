package com.app.platform.language.backend.media

class PublicMediaStorage(
  private val baseUrl: String,
) : MediaStorage {
  override fun urlFor(path: String): String = "$baseUrl/${path.trimStart('/')}"
}
