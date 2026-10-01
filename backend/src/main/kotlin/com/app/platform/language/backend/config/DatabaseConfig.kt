package com.app.platform.language.backend.config

data class DatabaseConfig(
  val url: String,
  val user: String,
  val password: String,
) {
  override fun toString(): String = "DatabaseConfig(url=$url, user=$user, password=***)"
}
