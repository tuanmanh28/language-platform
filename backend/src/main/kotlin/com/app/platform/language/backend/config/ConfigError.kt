package com.app.platform.language.backend.config

sealed interface ConfigError {
  val message: String

  data class Missing(
    val name: String,
  ) : ConfigError {
    override val message = "$name is required outside the local environment"
  }

  data class Invalid(
    val name: String,
    val value: String,
  ) : ConfigError {
    override val message = "$name has an invalid value: '$value'"
  }

  data object WildcardOriginOutsideLocal : ConfigError {
    override val message = "CORS_ALLOWED_ORIGINS may only be '*' in the local environment"
  }
}
