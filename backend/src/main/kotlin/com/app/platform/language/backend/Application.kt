package com.app.platform.language.backend

import com.app.platform.language.backend.attempt.AttemptService
import com.app.platform.language.backend.attempt.AttemptStore
import com.app.platform.language.backend.attempt.DatabaseAttemptStore
import com.app.platform.language.backend.attempt.attemptRoutes
import com.app.platform.language.backend.auth.DevTokenVerifier
import com.app.platform.language.backend.auth.FirebaseTokenVerifier
import com.app.platform.language.backend.auth.TokenVerifier
import com.app.platform.language.backend.config.AppConfig
import com.app.platform.language.backend.config.AudioStorageConfig
import com.app.platform.language.backend.config.BuildInfo
import com.app.platform.language.backend.config.ContentSource
import com.app.platform.language.backend.content.ContentAccessPolicy
import com.app.platform.language.backend.database.AppDatabase
import com.app.platform.language.backend.docs.docsRoutes
import com.app.platform.language.backend.health.DatabaseHealth
import com.app.platform.language.backend.health.healthRoutes
import com.app.platform.language.backend.listening.BundledListeningContentStore
import com.app.platform.language.backend.listening.DatabaseListeningContentStore
import com.app.platform.language.backend.listening.LOCAL_AUDIO_PATH
import com.app.platform.language.backend.listening.ListeningContentStore
import com.app.platform.language.backend.listening.ListeningService
import com.app.platform.language.backend.listening.listeningAudioRoutes
import com.app.platform.language.backend.listening.listeningRoutes
import com.app.platform.language.backend.media.LocalMediaStorage
import com.app.platform.language.backend.media.MediaStorage
import com.app.platform.language.backend.media.PrivateMediaService
import com.app.platform.language.backend.media.PublicMediaStorage
import com.app.platform.language.backend.media.R2MediaStorage
import com.app.platform.language.backend.plugins.FIREBASE_AUTH
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
import com.app.platform.language.backend.writing.BundledWritingContentStore
import com.app.platform.language.backend.writing.DatabaseWritingContentStore
import com.app.platform.language.backend.writing.DatabaseWritingSubmissionStore
import com.app.platform.language.backend.writing.LOCAL_WRITING_IMAGE_PATH
import com.app.platform.language.backend.writing.WritingContentStore
import com.app.platform.language.backend.writing.WritingService
import com.app.platform.language.backend.writing.WritingSubmissionStore
import com.app.platform.language.backend.writing.writingImageRoutes
import com.app.platform.language.backend.writing.writingRoutes
import com.github.michaelbull.result.getOrElse
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.auth.authenticate
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

private const val SHUTDOWN_GRACE_PERIOD_MILLIS = 5_000L
private const val SHUTDOWN_TIMEOUT_MILLIS = 15_000L
private const val AUDIO_DIR = "audio"
private const val IMAGES_DIR = "images"

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
      writingSubmissionStore = DatabaseWritingSubmissionStore(database),
      config = config,
      contentStore = contentStore(config.contentSource, database),
      listeningContentStore = listeningContentStore(config.contentSource, database),
      writingContentStore = writingContentStore(config.contentSource, database),
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

private fun writingContentStore(
  source: ContentSource,
  database: AppDatabase,
): WritingContentStore =
  when (source) {
    ContentSource.DB -> DatabaseWritingContentStore(database)
    ContentSource.BUNDLED -> BundledWritingContentStore()
  }

private fun localAudioStorage(
  config: AppConfig,
  storage: AudioStorageConfig.Local,
): LocalMediaStorage = LocalMediaStorage(config.contentDir.resolve(AUDIO_DIR), storage.apiBaseUrl, LOCAL_AUDIO_PATH)

private fun privateAudioStorage(config: AppConfig): MediaStorage =
  when (val storage = config.audioStorage) {
    is AudioStorageConfig.Local -> localAudioStorage(config, storage)
    is AudioStorageConfig.R2 -> R2MediaStorage(storage)
  }

private fun localImageStorage(
  config: AppConfig,
  storage: AudioStorageConfig.Local,
): LocalMediaStorage =
  LocalMediaStorage(config.contentDir.resolve(IMAGES_DIR), storage.apiBaseUrl, LOCAL_WRITING_IMAGE_PATH)

// Images sit under images/ in the bucket, as in CONTENT_DIR, so their keys never collide with audio at the root.
private fun privateImageStorage(config: AppConfig): MediaStorage =
  when (val storage = config.audioStorage) {
    is AudioStorageConfig.Local -> localImageStorage(config, storage)
    is AudioStorageConfig.R2 -> R2MediaStorage(storage, keyPrefix = "$IMAGES_DIR/")
  }

private fun withDevAuth(
  verifier: TokenVerifier,
  config: AppConfig,
): TokenVerifier {
  val devAuth = config.devAuth ?: return verifier
  return DevTokenVerifier(devAuth.token, devAuth.ownerEmail, verifier)
}

fun Application.module(
  databaseHealth: DatabaseHealth,
  userStore: UserStore,
  attemptStore: AttemptStore,
  writingSubmissionStore: WritingSubmissionStore,
  config: AppConfig = AppConfig.local,
  contentStore: ContentStore = BundledContentStore(),
  listeningContentStore: ListeningContentStore = BundledListeningContentStore(),
  writingContentStore: WritingContentStore = BundledWritingContentStore(),
  tokenVerifier: TokenVerifier = FirebaseTokenVerifier(config.firebaseProjectId),
) {
  configureSerialization()
  configureMonitoring()
  configureCors(config.allowedOrigins)
  configureStatusPages()
  configureAuthentication(withDevAuth(tokenVerifier, config))

  val contentAccess = ContentAccessPolicy(config.ownerEmails)
  val readingService = ReadingService(contentStore, contentAccess)
  val listeningService =
    ListeningService(
      listeningContentStore,
      PublicMediaStorage(config.audioBaseUrl),
      privateAudioStorage(config),
      contentAccess,
    )
  val userService = UserService(userStore)
  val attemptService = AttemptService(attemptStore, contentStore, contentAccess)
  val writingService =
    WritingService(
      writingContentStore,
      writingSubmissionStore,
      PublicMediaStorage(config.audioBaseUrl),
      privateImageStorage(config),
      contentAccess,
    )
  routing {
    healthRoutes(BuildInfo.version, config.env, databaseHealth)
    authenticate(FIREBASE_AUTH, optional = true) {
      readingRoutes(readingService)
      listeningRoutes(listeningService)
    }
    authenticatedUser(userService) {
      userRoutes()
      attemptRoutes(attemptService)
      writingRoutes(writingService)
    }
    val audioStorage = config.audioStorage
    if (audioStorage is AudioStorageConfig.Local) {
      authenticate(FIREBASE_AUTH) {
        listeningAudioRoutes(PrivateMediaService(localAudioStorage(config, audioStorage), contentAccess))
        writingImageRoutes(PrivateMediaService(localImageStorage(config, audioStorage), contentAccess))
      }
    }
    docsRoutes(config.env)
  }
}
