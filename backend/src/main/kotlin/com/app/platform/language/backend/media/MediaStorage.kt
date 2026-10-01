package com.app.platform.language.backend.media

interface MediaStorage {
  fun urlFor(path: String): String
}
