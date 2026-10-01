package com.app.platform.language.backend.audio

class PublicAudioStorage(
  private val baseUrl: String,
) : AudioStorage {
  override fun urlFor(path: String): String = "$baseUrl/${path.trimStart('/')}"
}
