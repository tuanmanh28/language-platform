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
      "CONTENT_SOURCE" to "bundled",
      "AUDIO_BASE_URL" to "https://audio.example.com/content",
      "FIREBASE_PROJECT_ID" to "language-platform-prod",
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
        contentSource = ContentSource.BUNDLED,
        audioBaseUrl = "https://audio.example.com/content",
        firebaseProjectId = "language-platform-prod",
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
  fun firebaseProjectIdIsRequiredOutsideLocal() {
    assertEquals(
      Err(ConfigError.Missing("FIREBASE_PROJECT_ID")),
      AppConfig.parse(prodVariables - "FIREBASE_PROJECT_ID"),
    )
  }

  @Test
  fun firebaseProjectIdIsReadFromEnvironmentInLocal() {
    val config = AppConfig.parse(mapOf("FIREBASE_PROJECT_ID" to "language-platform-dev"))

    assertEquals(Ok("language-platform-dev"), config.map { it.firebaseProjectId })
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
  fun contentSourceDefaultsToDatabase() {
    val config = AppConfig.parse(prodVariables - "CONTENT_SOURCE")

    assertEquals(Ok(ContentSource.DB), config.map { it.contentSource })
  }

  @Test
  fun unknownContentSourceIsInvalid() {
    assertEquals(
      Err(ConfigError.Invalid("CONTENT_SOURCE", "s3")),
      AppConfig.parse(mapOf("CONTENT_SOURCE" to "s3")),
    )
  }

  @Test
  fun audioBaseUrlDefaultsToLocalStorageInLocal() {
    assertEquals(Ok("http://localhost:9000/audio"), AppConfig.parse(emptyMap()).map { it.audioBaseUrl })
  }

  @Test
  fun databaseConfigIsParsedWithoutTheOtherSettings() {
    val variables = prodVariables - "AUDIO_BASE_URL" - "CORS_ALLOWED_ORIGINS" + ("PORT" to "not-a-port")

    assertEquals(
      Ok(DatabaseConfig("jdbc:postgresql://db.internal:5432/language_platform", "api", "s3cret")),
      AppConfig.parseDatabase(variables),
    )
  }

  @Test
  fun databaseConfigIsRequiredOutsideLocal() {
    assertEquals(Err(ConfigError.Missing("DATABASE_URL")), AppConfig.parseDatabase(mapOf("APP_ENV" to "prod")))
  }

  @Test
  fun audioBaseUrlIsRequiredOutsideLocal() {
    assertEquals(Err(ConfigError.Missing("AUDIO_BASE_URL")), AppConfig.parse(prodVariables - "AUDIO_BASE_URL"))
  }

  @Test
  fun audioBaseUrlLosesTrailingSlash() {
    val config = AppConfig.parse(mapOf("AUDIO_BASE_URL" to "https://cdn.example.com/audio/"))

    assertEquals(Ok("https://cdn.example.com/audio"), config.map { it.audioBaseUrl })
  }

  @Test
  fun audioBaseUrlWithoutHttpSchemeIsInvalid() {
    assertEquals(
      Err(ConfigError.Invalid("AUDIO_BASE_URL", "s3://bucket/audio")),
      AppConfig.parse(mapOf("AUDIO_BASE_URL" to "s3://bucket/audio")),
    )
  }

  @Test
  fun audioBaseUrlWithQueryIsInvalid() {
    assertEquals(
      Err(ConfigError.Invalid("AUDIO_BASE_URL", "https://cdn.example.com/audio?token=1")),
      AppConfig.parse(mapOf("AUDIO_BASE_URL" to "https://cdn.example.com/audio?token=1")),
    )
  }

  @Test
  fun databasePasswordIsHiddenFromToString() {
    assertFalse("s3cret" in DatabaseConfig("url", "user", "s3cret").toString())
  }
}
