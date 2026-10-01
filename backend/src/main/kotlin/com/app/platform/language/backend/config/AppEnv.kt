package com.app.platform.language.backend.config

enum class AppEnv(
  val id: String,
) {
  LOCAL("local"),
  STAGING("staging"),
  PROD("prod"),
  ;

  companion object {
    fun fromId(id: String): AppEnv? = entries.find { it.id == id }
  }
}
