package com.app.platform.language.backend

import com.app.platform.language.backend.config.AppConfig
import com.app.platform.language.backend.content.ContentSeeder
import com.app.platform.language.backend.database.AppDatabase
import com.github.michaelbull.result.getOrElse
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import kotlin.io.path.Path
import kotlin.io.path.isDirectory
import kotlin.system.exitProcess

fun main(args: Array<String>) {
  val logger = LoggerFactory.getLogger("SeedContent")
  val publicDir =
    args.singleOrNull()?.let(::Path) ?: run {
      logger.error("Usage: SeedContent <public content directory with reading/, listening/ and writing/>")
      exitProcess(1)
    }
  val privateDir = AppConfig.contentDirFromEnvironment()
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

  if (!privateDir.isDirectory()) logger.warn("CONTENT_DIR {} does not exist; no private content seeded", privateDir)
  val seeded = database.use { db -> runBlocking { ContentSeeder(db).seed(publicDir, privateDir) } }
  val (public, private) =
    seeded.getOrElse { error ->
      logger.error(error.message, error.cause)
      exitProcess(1)
    }
  logger.info(
    "Seeded {} reading and {} listening tests and {} writing prompts as public from {}",
    public.reading,
    public.listening,
    public.writing,
    publicDir,
  )
  logger.info(
    "Seeded {} reading and {} listening tests and {} writing prompts as private from {}",
    private.reading,
    private.listening,
    private.writing,
    privateDir,
  )
}
