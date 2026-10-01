package com.app.platform.language.backend.config

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.map
import kotlin.io.path.Path
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
      "CONTENT_DIR" to "/srv/content",
      "OWNER_EMAILS" to "Owner@Example.com, second@example.com",
      "AUDIO_STORAGE" to "r2",
      "R2_ACCOUNT_ID" to "0123abcd",
      "R2_BUCKET" to "private-audio",
      "R2_ACCESS_KEY_ID" to "key-id",
      "R2_SECRET_ACCESS_KEY" to "r2-secret",
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
        contentDir = Path("/srv/content"),
        ownerEmails = setOf("Owner@Example.com", "second@example.com"),
        devAuth = null,
        audioStorage =
          AudioStorageConfig.R2(
            accountId = "0123abcd",
            bucket = "private-audio",
            accessKeyId = "key-id",
            secretAccessKey = "r2-secret",
          ),
      )

    assertEquals(Ok(expected), AppConfig.parse(prodVariables))
  }

  @Test
  fun contentDirDefaultsToTheLanguagePlatformFolderInHome() {
    val home = Path(System.getProperty("user.home"))

    assertEquals(Ok(home.resolve("LanguagePlatform/content")), AppConfig.parse(emptyMap()).map { it.contentDir })
  }

  @Test
  fun contentDirExpandsHome() {
    val home = Path(System.getProperty("user.home"))

    assertEquals(
      Ok(home.resolve("ielts/content")),
      AppConfig.parse(mapOf("CONTENT_DIR" to "~/ielts/content")).map { it.contentDir },
    )
  }

  @Test
  fun malformedOwnerEmailIsInvalid() {
    assertEquals(
      Err(ConfigError.Invalid("OWNER_EMAILS", "not-an-email")),
      AppConfig.parse(mapOf("OWNER_EMAILS" to "owner@example.com, not-an-email")),
    )
  }

  @Test
  fun devAuthIsEnabledInLocalForTheFirstOwner() {
    val config =
      AppConfig.parse(
        mapOf("DEV_AUTH_TOKEN" to "dev-token", "OWNER_EMAILS" to "a@example.com,b@example.com"),
      )

    assertEquals(Ok(DevAuthConfig("dev-token", "a@example.com")), config.map { it.devAuth })
  }

  @Test
  fun devAuthIsRejectedInProd() {
    assertEquals(
      Err(ConfigError.DevAuthOutsideLocal),
      AppConfig.parse(prodVariables + ("DEV_AUTH_TOKEN" to "dev-token")),
    )
  }

  @Test
  fun devAuthIsRejectedInStaging() {
    assertEquals(
      Err(ConfigError.DevAuthOutsideLocal),
      AppConfig.parse(prodVariables + mapOf("APP_ENV" to "staging", "DEV_AUTH_TOKEN" to "dev-token")),
    )
  }

  @Test
  fun devAuthNeedsAnOwner() {
    assertEquals(
      Err(ConfigError.RequiredBy("OWNER_EMAILS", "DEV_AUTH_TOKEN is set")),
      AppConfig.parse(mapOf("DEV_AUTH_TOKEN" to "dev-token")),
    )
  }

  @Test
  fun devTokenIsHiddenFromToString() {
    assertFalse("dev-token" in DevAuthConfig("dev-token", "owner@example.com").toString())
  }

  @Test
  fun localAudioStorageUsesTheApiBaseUrl() {
    val config = AppConfig.parse(mapOf("AUDIO_STORAGE" to "local", "API_BASE_URL" to "http://10.0.2.2:8080/"))

    assertEquals(Ok(AudioStorageConfig.Local("http://10.0.2.2:8080")), config.map { it.audioStorage })
  }

  @Test
  fun localAudioStorageDefaultsToThisServer() {
    assertEquals(
      Ok(AudioStorageConfig.Local("http://localhost:9090")),
      AppConfig.parse(mapOf("PORT" to "9090")).map { it.audioStorage },
    )
  }

  @Test
  fun audioStorageIsRequiredOutsideLocal() {
    assertEquals(Err(ConfigError.Missing("AUDIO_STORAGE")), AppConfig.parse(prodVariables - "AUDIO_STORAGE"))
  }

  @Test
  fun localAudioStorageIsRejectedInProd() {
    assertEquals(
      Err(ConfigError.LocalAudioOutsideLocal),
      AppConfig.parse(prodVariables + ("AUDIO_STORAGE" to "local")),
    )
  }

  @Test
  fun localAudioStorageIsRejectedInStaging() {
    assertEquals(
      Err(ConfigError.LocalAudioOutsideLocal),
      AppConfig.parse(prodVariables + mapOf("APP_ENV" to "staging", "AUDIO_STORAGE" to "local")),
    )
  }

  @Test
  fun r2StorageNeedsEveryCredential() {
    assertEquals(
      Err(ConfigError.RequiredBy("R2_SECRET_ACCESS_KEY", "AUDIO_STORAGE is r2")),
      AppConfig.parse(prodVariables - "R2_SECRET_ACCESS_KEY"),
    )
  }

  @Test
  fun malformedR2AccountIdIsInvalid() {
    assertEquals(
      Err(ConfigError.Invalid("R2_ACCOUNT_ID", "evil.example.com/")),
      AppConfig.parse(prodVariables + ("R2_ACCOUNT_ID" to "evil.example.com/")),
    )
  }

  @Test
  fun unknownAudioStorageIsInvalid() {
    assertEquals(
      Err(ConfigError.Invalid("AUDIO_STORAGE", "s3")),
      AppConfig.parse(mapOf("AUDIO_STORAGE" to "s3")),
    )
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
