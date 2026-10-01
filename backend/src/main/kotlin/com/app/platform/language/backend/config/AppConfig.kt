package com.app.platform.language.backend.config

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.map
import java.nio.file.Path
import kotlin.io.path.Path

data class AppConfig(
  val port: Int,
  val env: AppEnv,
  val database: DatabaseConfig,
  val allowedOrigins: AllowedOrigins,
  val contentSource: ContentSource,
  val audioBaseUrl: String,
  val firebaseProjectId: String,
  val contentDir: Path,
  val ownerEmails: Set<String>,
  val devAuth: DevAuthConfig?,
  val audioStorage: AudioStorageConfig,
) {
  companion object {
    private const val DEFAULT_PORT = 8080
    private const val LOCAL_AUDIO_BASE_URL = "http://localhost:9000/audio"
    private const val AUDIO_STORAGE_LOCAL = "local"
    private const val AUDIO_STORAGE_R2 = "r2"

    // The "demo-" prefix is what the Firebase emulator expects for a project that does not exist.
    private const val LOCAL_FIREBASE_PROJECT_ID = "demo-language-platform"
    private val localDatabase =
      DatabaseConfig(url = "jdbc:postgresql://localhost:5432/language_platform", user = "app", password = "app")
    private val originPattern = Regex("https?://[a-z0-9.-]+(:\\d{1,5})?")
    private val baseUrlPattern = Regex("https?://[A-Za-z0-9.-]+(:\\d{1,5})?(/[A-Za-z0-9._~-]+)*/?")
    private val emailPattern = Regex("[^@\\s]+@[^@\\s]+")
    private val r2AccountIdPattern = Regex("[a-z0-9]{1,64}")
    private val r2BucketPattern = Regex("[a-z0-9][a-z0-9-]{1,61}[a-z0-9]")
    private val homeDir = Path(System.getProperty("user.home"))
    private val defaultContentDir = homeDir.resolve("LanguagePlatform").resolve("content")

    val local =
      AppConfig(
        port = DEFAULT_PORT,
        env = AppEnv.LOCAL,
        database = localDatabase,
        allowedOrigins = AllowedOrigins.All,
        contentSource = ContentSource.DB,
        audioBaseUrl = LOCAL_AUDIO_BASE_URL,
        firebaseProjectId = LOCAL_FIREBASE_PROJECT_ID,
        contentDir = defaultContentDir,
        ownerEmails = emptySet(),
        devAuth = null,
        audioStorage = AudioStorageConfig.Local("http://localhost:$DEFAULT_PORT"),
      )

    fun fromEnvironment(): Result<AppConfig, ConfigError> = parse(System.getenv())

    fun databaseFromEnvironment(): Result<DatabaseConfig, ConfigError> = parseDatabase(System.getenv())

    fun contentDirFromEnvironment(): Path = parseContentDir(System.getenv()["CONTENT_DIR"]?.takeIf { it.isNotBlank() })

    fun parse(variables: Map<String, String>): Result<AppConfig, ConfigError> {
      val values = variables.filterValues { it.isNotBlank() }
      return binding {
        val env = parseEnv(values["APP_ENV"]).bind()
        val port = parsePort(values["PORT"]).bind()
        val ownerEmails = parseOwnerEmails(values["OWNER_EMAILS"]).bind()
        AppConfig(
          port = port,
          env = env,
          database = parseDatabase(values, env).bind(),
          allowedOrigins = parseAllowedOrigins(values["CORS_ALLOWED_ORIGINS"], env).bind(),
          contentSource = parseContentSource(values["CONTENT_SOURCE"]).bind(),
          audioBaseUrl =
            values
              .valueOrLocalDefault("AUDIO_BASE_URL", env, LOCAL_AUDIO_BASE_URL)
              .andThen { parseBaseUrl("AUDIO_BASE_URL", it) }
              .bind(),
          firebaseProjectId =
            values.valueOrLocalDefault("FIREBASE_PROJECT_ID", env, LOCAL_FIREBASE_PROJECT_ID).bind(),
          contentDir = parseContentDir(values["CONTENT_DIR"]),
          ownerEmails = ownerEmails,
          devAuth = parseDevAuth(values["DEV_AUTH_TOKEN"], env, ownerEmails).bind(),
          audioStorage =
            values
              .valueOrLocalDefault("AUDIO_STORAGE", env, AUDIO_STORAGE_LOCAL)
              .andThen { parseAudioStorage(it, values, env, port) }
              .bind(),
        )
      }
    }

    fun parseDatabase(variables: Map<String, String>): Result<DatabaseConfig, ConfigError> {
      val values = variables.filterValues { it.isNotBlank() }
      return parseEnv(values["APP_ENV"]).andThen { env -> parseDatabase(values, env) }
    }

    private fun parseDatabase(
      values: Map<String, String>,
      env: AppEnv,
    ): Result<DatabaseConfig, ConfigError> =
      binding {
        DatabaseConfig(
          url = values.valueOrLocalDefault("DATABASE_URL", env, localDatabase.url).bind(),
          user = values.valueOrLocalDefault("DATABASE_USER", env, localDatabase.user).bind(),
          password = values.valueOrLocalDefault("DATABASE_PASSWORD", env, localDatabase.password).bind(),
        )
      }

    private fun parseBaseUrl(
      name: String,
      value: String,
    ): Result<String, ConfigError> =
      if (baseUrlPattern.matches(value)) {
        Ok(value.trimEnd('/'))
      } else {
        Err(ConfigError.Invalid(name, value))
      }

    private fun parseContentDir(value: String?): Path =
      when {
        value == null -> defaultContentDir
        value.startsWith("~/") -> homeDir.resolve(value.removePrefix("~/"))
        else -> Path(value)
      }

    private fun parseOwnerEmails(value: String?): Result<Set<String>, ConfigError> {
      val emails =
        value
          .orEmpty()
          .split(',')
          .map(String::trim)
          .filter(String::isNotEmpty)
      val invalid = emails.find { !emailPattern.matches(it) }
      return if (invalid == null) Ok(emails.toSet()) else Err(ConfigError.Invalid("OWNER_EMAILS", invalid))
    }

    private fun parseDevAuth(
      token: String?,
      env: AppEnv,
      ownerEmails: Set<String>,
    ): Result<DevAuthConfig?, ConfigError> =
      when {
        token == null -> Ok(null)
        env != AppEnv.LOCAL -> Err(ConfigError.DevAuthOutsideLocal)
        ownerEmails.isEmpty() -> Err(ConfigError.RequiredBy("OWNER_EMAILS", "DEV_AUTH_TOKEN is set"))
        else -> Ok(DevAuthConfig(token, ownerEmails.first()))
      }

    private fun parseAudioStorage(
      storage: String,
      values: Map<String, String>,
      env: AppEnv,
      port: Int,
    ): Result<AudioStorageConfig, ConfigError> =
      when (storage) {
        AUDIO_STORAGE_LOCAL -> {
          if (env == AppEnv.LOCAL) {
            parseBaseUrl("API_BASE_URL", values["API_BASE_URL"] ?: "http://localhost:$port")
              .map(AudioStorageConfig::Local)
          } else {
            Err(ConfigError.LocalAudioOutsideLocal)
          }
        }

        AUDIO_STORAGE_R2 -> {
          binding {
            AudioStorageConfig.R2(
              accountId = values.requiredForR2("R2_ACCOUNT_ID", r2AccountIdPattern).bind(),
              bucket = values.requiredForR2("R2_BUCKET", r2BucketPattern).bind(),
              accessKeyId = values.requiredForR2("R2_ACCESS_KEY_ID").bind(),
              secretAccessKey = values.requiredForR2("R2_SECRET_ACCESS_KEY").bind(),
            )
          }
        }

        else -> {
          Err(ConfigError.Invalid("AUDIO_STORAGE", storage))
        }
      }

    private fun Map<String, String>.requiredForR2(
      name: String,
      pattern: Regex? = null,
    ): Result<String, ConfigError> {
      val value = this[name] ?: return Err(ConfigError.RequiredBy(name, "AUDIO_STORAGE is $AUDIO_STORAGE_R2"))
      return if (pattern == null || pattern.matches(value)) Ok(value) else Err(ConfigError.Invalid(name, value))
    }

    private fun parseContentSource(value: String?): Result<ContentSource, ConfigError> {
      if (value == null) return Ok(ContentSource.DB)
      return ContentSource.fromId(value)?.let(::Ok) ?: Err(ConfigError.Invalid("CONTENT_SOURCE", value))
    }

    private fun parseEnv(value: String?): Result<AppEnv, ConfigError> {
      if (value == null) return Ok(AppEnv.LOCAL)
      return AppEnv.fromId(value)?.let(::Ok) ?: Err(ConfigError.Invalid("APP_ENV", value))
    }

    private fun parsePort(value: String?): Result<Int, ConfigError> {
      if (value == null) return Ok(DEFAULT_PORT)
      return value.toIntOrNull()?.takeIf { it in 1..65535 }?.let(::Ok) ?: Err(ConfigError.Invalid("PORT", value))
    }

    private fun Map<String, String>.valueOrLocalDefault(
      name: String,
      env: AppEnv,
      localDefault: String,
    ): Result<String, ConfigError> =
      when {
        name in this -> Ok(getValue(name))
        env == AppEnv.LOCAL -> Ok(localDefault)
        else -> Err(ConfigError.Missing(name))
      }

    private fun parseAllowedOrigins(
      value: String?,
      env: AppEnv,
    ): Result<AllowedOrigins, ConfigError> {
      val isLocal = env == AppEnv.LOCAL
      if (value == null) return Ok(if (isLocal) AllowedOrigins.All else AllowedOrigins.Only(emptySet()))

      val origins = value.split(',').map { it.trim().lowercase() }.filter(String::isNotEmpty)
      if ("*" in origins) return if (isLocal) Ok(AllowedOrigins.All) else Err(ConfigError.WildcardOriginOutsideLocal)
      val invalid = if (origins.isEmpty()) value else origins.find { !originPattern.matches(it) }
      return if (invalid == null) {
        Ok(AllowedOrigins.Only(origins.toSet()))
      } else {
        Err(ConfigError.Invalid("CORS_ALLOWED_ORIGINS", invalid))
      }
    }
  }
}
