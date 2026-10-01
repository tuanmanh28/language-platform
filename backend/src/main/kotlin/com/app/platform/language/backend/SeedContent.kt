package com.app.platform.language.backend

import com.app.platform.language.backend.config.AppConfig
import com.app.platform.language.backend.database.AppDatabase
import com.app.platform.language.backend.listening.ListeningContentSeeder
import com.app.platform.language.backend.reading.ReadingContentSeeder
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.getOrElse
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import kotlin.io.path.Path
import kotlin.system.exitProcess

fun main(args: Array<String>) {
  val logger = LoggerFactory.getLogger("SeedContent")
  val contentDir =
    args.singleOrNull()?.let(::Path) ?: run {
      logger.error("Usage: SeedContent <content directory with reading/ and listening/>")
      exitProcess(1)
    }
  val databaseConfig =
    AppConfig.databaseFromEnvironment().getOrElse { error ->
      logger.error("Invalid configuration: {}", error.message)
      exitProcess(1)
    }
  val database =
    AppDatabase.connect(databaseConfig).getOrElse { error ->
      logger.error(error.message, error.cause)
      exitProcess(1)
    }

  val readingDir = contentDir.resolve("reading")
  val listeningDir = contentDir.resolve("listening")
  val seeded =
    database.use { db ->
      runBlocking {
        coroutineBinding {
          ReadingContentSeeder(db).seed(readingDir).bind() to ListeningContentSeeder(db).seed(listeningDir).bind()
        }
      }
    }
  val (readingCount, listeningCount) =
    seeded.getOrElse { error ->
      logger.error(error.message, error.cause)
      exitProcess(1)
    }
  logger.info("Seeded {} reading tests from {}", readingCount, readingDir)
  logger.info("Seeded {} listening tests from {}", listeningCount, listeningDir)
}
