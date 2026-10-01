package com.app.platform.language.backend

import com.app.platform.language.backend.attempt.AttemptService
import com.app.platform.language.backend.attempt.AttemptStore
import com.app.platform.language.backend.attempt.DatabaseAttemptStore
import com.app.platform.language.backend.attempt.attemptRoutes
import com.app.platform.language.backend.auth.FirebaseTokenVerifier
import com.app.platform.language.backend.auth.TokenVerifier
import com.app.platform.language.backend.config.AppConfig
import com.app.platform.language.backend.config.BuildInfo
import com.app.platform.language.backend.config.ContentSource
import com.app.platform.language.backend.database.AppDatabase
import com.app.platform.language.backend.docs.docsRoutes
import com.app.platform.language.backend.health.DatabaseHealth
import com.app.platform.language.backend.health.healthRoutes
import com.app.platform.language.backend.listening.BundledListeningContentStore
import com.app.platform.language.backend.listening.DatabaseListeningContentStore
import com.app.platform.language.backend.listening.ListeningContentStore
import com.app.platform.language.backend.listening.ListeningService
import com.app.platform.language.backend.listening.listeningRoutes
import com.app.platform.language.backend.plugins.configureAuthentication
import com.app.platform.language.backend.plugins.configureCors
import com.app.platform.language.backend.plugins.configureMonitoring
import com.app.platform.language.backend.plugins.configureSerialization
import com.app.platform.language.backend.plugins.configureStatusPages
import com.app.platform.language.backend.reading.BundledContentStore
import com.app.platform.language.backend.reading.ContentStore
import com.app.platform.language.backend.reading.DatabaseContentStore
import com.app.platform.language.backend.reading.ReadingService
import com.app.platform.language.backend.reading.readingRoutes
import com.app.platform.language.backend.user.DatabaseUserStore
import com.app.platform.language.backend.user.UserService
import com.app.platform.language.backend.user.UserStore
import com.app.platform.language.backend.user.authenticatedUser
import com.app.platform.language.backend.user.userRoutes
import com.github.michaelbull.result.getOrElse
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

private const val SHUTDOWN_GRACE_PERIOD_MILLIS = 5_000L
private const val SHUTDOWN_TIMEOUT_MILLIS = 15_000L

fun main() {
  val logger = LoggerFactory.getLogger("Application")
  val config =
    AppConfig.fromEnvironment().getOrElse { error ->
      logger.error("Invalid configuration: {}", error.message)
      exitProcess(1)
    }
  logger.info("Starting version {} in {} on port {}", BuildInfo.version, config.env.id, config.port)
  val database =
    AppDatabase.connect(config.database).getOrElse { error ->
      logger.error(error.message, error.cause)
      exitProcess(1)
    }

  embeddedServer(
    Netty,
    configure = {
      connector {
        host = "0.0.0.0"
        port = config.port
      }
      shutdownGracePeriod = SHUTDOWN_GRACE_PERIOD_MILLIS
      shutdownTimeout = SHUTDOWN_TIMEOUT_MILLIS
    },
  ) {
    monitor.subscribe(ApplicationStopped) { database.close() }
    module(
      databaseHealth = DatabaseHealth { database.isReachable() },
      userStore = DatabaseUserStore(database),
      attemptStore = DatabaseAttemptStore(database),
      config = config,
      contentStore = contentStore(config.contentSource, database),
      listeningContentStore = listeningContentStore(config.contentSource, database),
    )
  }.start(wait = true)
}

private fun contentStore(
  source: ContentSource,
  database: AppDatabase,
): ContentStore =
  when (source) {
    ContentSource.DB -> DatabaseContentStore(database)
    ContentSource.BUNDLED -> BundledContentStore()
  }

private fun listeningContentStore(
  source: ContentSource,
  database: AppDatabase,
): ListeningContentStore =
  when (source) {
    ContentSource.DB -> DatabaseListeningContentStore(database)
    ContentSource.BUNDLED -> BundledListeningContentStore()
  }

fun Application.module(
  databaseHealth: DatabaseHealth,
  userStore: UserStore,
  attemptStore: AttemptStore,
  config: AppConfig = AppConfig.local,
  contentStore: ContentStore = BundledContentStore(),
  listeningContentStore: ListeningContentStore = BundledListeningContentStore(),
  tokenVerifier: TokenVerifier = FirebaseTokenVerifier(config.firebaseProjectId),
) {
  configureSerialization()
  configureMonitoring()
  configureCors(config.allowedOrigins)
  configureStatusPages()
  configureAuthentication(tokenVerifier)

  val readingService = ReadingService(contentStore)
  val listeningService = ListeningService(listeningContentStore, config.audioBaseUrl)
  val userService = UserService(userStore)
  val attemptService = AttemptService(attemptStore, contentStore)
  routing {
    healthRoutes(BuildInfo.version, config.env, databaseHealth)
    readingRoutes(readingService)
    listeningRoutes(listeningService)
    authenticatedUser(userService) {
      userRoutes()
      attemptRoutes(attemptService)
    }
    docsRoutes(config.env)
  }
}
