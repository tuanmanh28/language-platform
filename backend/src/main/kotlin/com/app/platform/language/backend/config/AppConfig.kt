package com.app.platform.language.backend.config

data class AppConfig(
  val port: Int,
) {
  companion object {
    private const val DEFAULT_PORT = 8080

    fun fromEnvironment(): AppConfig = AppConfig(port = System.getenv("PORT")?.toIntOrNull() ?: DEFAULT_PORT)
  }
}
