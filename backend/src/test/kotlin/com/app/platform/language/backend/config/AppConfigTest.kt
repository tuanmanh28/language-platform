package com.app.platform.language.backend.config

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.map
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AppConfigTest {
  private val prodVariables =
    mapOf(
      "APP_ENV" to "prod",
      "PORT" to "9090",
      "DATABASE_URL" to "jdbc:postgresql://db.internal:5432/language_platform",
      "DATABASE_USER" to "api",
      "DATABASE_PASSWORD" to "s3cret",
      "CORS_ALLOWED_ORIGINS" to "https://app.example.com, https://admin.example.com:8443",
    )

  @Test
  fun emptyEnvironmentUsesLocalDefaults() {
    assertEquals(Ok(AppConfig.local), AppConfig.parse(emptyMap()))
  }

  @Test
  fun blankValuesFallBackToDefaults() {
    assertEquals(Ok(AppConfig.local), AppConfig.parse(mapOf("PORT" to " ", "APP_ENV" to "")))
  }

  @Test
  fun environmentVariablesOverrideDefaults() {
    val expected =
      AppConfig(
        port = 9090,
        env = AppEnv.PROD,
        database =
          DatabaseConfig(
            url = "jdbc:postgresql://db.internal:5432/language_platform",
            user = "api",
            password = "s3cret",
          ),
        allowedOrigins = AllowedOrigins.Only(setOf("https://app.example.com", "https://admin.example.com:8443")),
      )

    assertEquals(Ok(expected), AppConfig.parse(prodVariables))
  }

  @Test
  fun nonNumericPortIsInvalid() {
    assertEquals(Err(ConfigError.Invalid("PORT", "http")), AppConfig.parse(mapOf("PORT" to "http")))
  }

  @Test
  fun outOfRangePortIsInvalid() {
    assertEquals(Err(ConfigError.Invalid("PORT", "70000")), AppConfig.parse(mapOf("PORT" to "70000")))
  }

  @Test
  fun unknownEnvIsInvalid() {
    assertEquals(Err(ConfigError.Invalid("APP_ENV", "dev")), AppConfig.parse(mapOf("APP_ENV" to "dev")))
  }

  @Test
  fun databaseSettingsAreRequiredOutsideLocal() {
    assertEquals(
      Err(ConfigError.Missing("DATABASE_PASSWORD")),
      AppConfig.parse(prodVariables - "DATABASE_PASSWORD"),
    )
  }

  @Test
  fun originsDefaultToNoneOutsideLocal() {
    val config = AppConfig.parse(prodVariables - "CORS_ALLOWED_ORIGINS")

    assertEquals(Ok(AllowedOrigins.Only(emptySet())), config.map { it.allowedOrigins })
  }

  @Test
  fun wildcardOriginIsAllowedInLocal() {
    val config = AppConfig.parse(mapOf("CORS_ALLOWED_ORIGINS" to "*"))

    assertEquals(Ok(AllowedOrigins.All), config.map { it.allowedOrigins })
  }

  @Test
  fun originsAreLowercased() {
    val config = AppConfig.parse(mapOf("CORS_ALLOWED_ORIGINS" to "https://App.Example.com"))

    assertEquals(Ok(AllowedOrigins.Only(setOf("https://app.example.com"))), config.map { it.allowedOrigins })
  }

  @Test
  fun originListWithoutOriginsIsInvalid() {
    assertEquals(
      Err(ConfigError.Invalid("CORS_ALLOWED_ORIGINS", " , ")),
      AppConfig.parse(mapOf("CORS_ALLOWED_ORIGINS" to " , ")),
    )
  }

  @Test
  fun wildcardOriginIsRejectedOutsideLocal() {
    assertEquals(
      Err(ConfigError.WildcardOriginOutsideLocal),
      AppConfig.parse(prodVariables + ("CORS_ALLOWED_ORIGINS" to "*")),
    )
  }

  @Test
  fun malformedOriginIsInvalid() {
    assertEquals(
      Err(ConfigError.Invalid("CORS_ALLOWED_ORIGINS", "app.example.com/path")),
      AppConfig.parse(mapOf("CORS_ALLOWED_ORIGINS" to "https://ok.example.com,app.example.com/path")),
    )
  }

  @Test
  fun databasePasswordIsHiddenFromToString() {
    assertFalse("s3cret" in DatabaseConfig("url", "user", "s3cret").toString())
  }
}
