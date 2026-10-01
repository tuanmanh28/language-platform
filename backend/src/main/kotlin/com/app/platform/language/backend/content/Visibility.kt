package com.app.platform.language.backend.content

enum class Visibility(
  val id: String,
) {
  PUBLIC("public"),
  PRIVATE("private"),
  ;

  companion object {
    fun fromId(id: String): Visibility = entries.first { it.id == id }
  }
}
