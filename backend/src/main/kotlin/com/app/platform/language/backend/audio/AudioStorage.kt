package com.app.platform.language.backend.audio

interface AudioStorage {
  fun urlFor(path: String): String
}
