package com.app.platform.language.backend.config

sealed interface AllowedOrigins {
  data object All : AllowedOrigins

  data class Only(
    val origins: Set<String>,
  ) : AllowedOrigins
}
