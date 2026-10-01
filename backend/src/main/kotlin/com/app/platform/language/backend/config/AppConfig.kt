package com.app.platform.language.backend.config

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding

data class AppConfig(
  val port: Int,
  val env: AppEnv,
  val database: DatabaseConfig,
  val allowedOrigins: AllowedOrigins,
  val contentSource: ContentSource,
  val firebaseProjectId: String,
) {
  companion object {
    private const val DEFAULT_PORT = 8080

    // The "demo-" prefix is what the Firebase emulator expects for a project that does not exist.
    private const val LOCAL_FIREBASE_PROJECT_ID = "demo-language-platform"
    private val localDatabase =
      DatabaseConfig(url = "jdbc:postgresql://localhost:5432/language_platform", user = "app", password = "app")
    private val originPattern = Regex("https?://[a-z0-9.-]+(:\\d{1,5})?")

    val local =
      AppConfig(
        port = DEFAULT_PORT,
        env = AppEnv.LOCAL,
        database = localDatabase,
        allowedOrigins = AllowedOrigins.All,
        contentSource = ContentSource.DB,
        firebaseProjectId = LOCAL_FIREBASE_PROJECT_ID,
      )

    fun fromEnvironment(): Result<AppConfig, ConfigError> = parse(System.getenv())

    fun parse(variables: Map<String, String>): Result<AppConfig, ConfigError> {
      val values = variables.filterValues { it.isNotBlank() }
      return binding {
        val env = parseEnv(values["APP_ENV"]).bind()
        AppConfig(
          port = parsePort(values["PORT"]).bind(),
          env = env,
          database =
            DatabaseConfig(
              url = values.valueOrLocalDefault("DATABASE_URL", env, localDatabase.url).bind(),
              user = values.valueOrLocalDefault("DATABASE_USER", env, localDatabase.user).bind(),
              password = values.valueOrLocalDefault("DATABASE_PASSWORD", env, localDatabase.password).bind(),
            ),
          allowedOrigins = parseAllowedOrigins(values["CORS_ALLOWED_ORIGINS"], env).bind(),
          contentSource = parseContentSource(values["CONTENT_SOURCE"]).bind(),
          firebaseProjectId =
            values.valueOrLocalDefault("FIREBASE_PROJECT_ID", env, LOCAL_FIREBASE_PROJECT_ID).bind(),
        )
      }
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
