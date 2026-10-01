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

  data class RequiredBy(
    val name: String,
    val condition: String,
  ) : ConfigError {
    override val message = "$name is required when $condition"
  }

  data object WildcardOriginOutsideLocal : ConfigError {
    override val message = "CORS_ALLOWED_ORIGINS may only be '*' in the local environment"
  }

  data object DevAuthOutsideLocal : ConfigError {
    override val message = "DEV_AUTH_TOKEN may only be set in the local environment"
  }

  data object LocalAudioOutsideLocal : ConfigError {
    override val message = "AUDIO_STORAGE may only be 'local' in the local environment"
  }
}
