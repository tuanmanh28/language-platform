package com.app.platform.language.backend

import com.app.platform.language.backend.config.AppConfig
import com.app.platform.language.backend.config.BuildInfo
import com.app.platform.language.backend.config.ContentSource
import com.app.platform.language.backend.database.AppDatabase
import com.app.platform.language.backend.health.DatabaseHealth
import com.app.platform.language.backend.health.healthRoutes
import com.app.platform.language.backend.plugins.configureCors
import com.app.platform.language.backend.plugins.configureMonitoring
import com.app.platform.language.backend.plugins.configureSerialization
import com.app.platform.language.backend.plugins.configureStatusPages
import com.app.platform.language.backend.reading.BundledContentStore
import com.app.platform.language.backend.reading.ContentStore
import com.app.platform.language.backend.reading.DatabaseContentStore
import com.app.platform.language.backend.reading.ReadingService
import com.app.platform.language.backend.reading.readingRoutes
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
    module(DatabaseHealth { database.isReachable() }, config, contentStore(config.contentSource, database))
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

fun Application.module(
  databaseHealth: DatabaseHealth,
  config: AppConfig = AppConfig.local,
  contentStore: ContentStore = BundledContentStore(),
) {
  configureSerialization()
  configureMonitoring()
  configureCors(config.allowedOrigins)
  configureStatusPages()

  val readingService = ReadingService(contentStore)
  routing {
    healthRoutes(BuildInfo.version, config.env, databaseHealth)
    readingRoutes(readingService)
  }
}
