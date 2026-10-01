package com.app.platform.language.backend.config

enum class ContentSource(
  val id: String,
) {
  DB("db"),
  BUNDLED("bundled"),
  ;

  companion object {
    fun fromId(id: String): ContentSource? = entries.find { it.id == id }
  }
}
