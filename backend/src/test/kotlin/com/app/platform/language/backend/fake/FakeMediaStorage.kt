package com.app.platform.language.backend.fake

import com.app.platform.language.backend.media.MediaStorage

class FakeMediaStorage(
  private val baseUrl: String = "https://private.example.com",
) : MediaStorage {
  var signature = "1"

  override fun urlFor(path: String): String = "$baseUrl/$path?signature=$signature"
}
