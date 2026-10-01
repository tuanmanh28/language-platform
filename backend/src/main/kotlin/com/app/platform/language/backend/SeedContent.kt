package com.app.platform.language.backend

import com.app.platform.language.backend.config.AppConfig
import com.app.platform.language.backend.database.AppDatabase
import com.app.platform.language.backend.reading.ReadingContentSeeder
import com.github.michaelbull.result.getOrElse
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import kotlin.io.path.Path
import kotlin.system.exitProcess

fun main(args: Array<String>) {
  val logger = LoggerFactory.getLogger("SeedContent")
  val directory =
    args.singleOrNull()?.let(::Path) ?: run {
      logger.error("Usage: SeedContent <reading content directory>")
      exitProcess(1)
    }
  val config =
    AppConfig.fromEnvironment().getOrElse { error ->
      logger.error("Invalid configuration: {}", error.message)
      exitProcess(1)
    }
  val database =
    AppDatabase.connect(config.database).getOrElse { error ->
      logger.error(error.message, error.cause)
      exitProcess(1)
    }

  val seeded = database.use { runBlocking { ReadingContentSeeder(it).seed(directory) } }
  val count =
    seeded.getOrElse { error ->
      logger.error(error.message, error.cause)
      exitProcess(1)
    }
  logger.info("Seeded {} reading tests from {}", count, directory)
}
