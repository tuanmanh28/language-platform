package com.app.platform.language.backend.fake

import com.app.platform.language.backend.audio.AudioStorage

class FakeAudioStorage(
  private val baseUrl: String = "https://private.example.com",
) : AudioStorage {
  var signature = "1"

  override fun urlFor(path: String): String = "$baseUrl/$path?signature=$signature"
}
