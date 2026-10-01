package com.app.platform.language.shared.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.app.platform.language.shared.db.LanguagePlatformDatabase
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.java.Java
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

// Desktop (Windows/macOS/Linux via the JVM). The DB lives in the user's app-data directory.
actual fun platformModule(): Module =
  module {
    single<HttpClientEngine> { Java.create() }
    single<SqlDriver> {
      val file = File(appDataDirectory(), "language_platform.db")
      val isNewDatabase = !file.exists()
      JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}").also { driver ->
        // TODO: when the schema changes, add .sqm migrations and call Schema.migrate(...)
        if (isNewDatabase) LanguagePlatformDatabase.Schema.create(driver)
      }
    }
  }

private fun appDataDirectory(): File {
  val os = System.getProperty("os.name").orEmpty().lowercase()
  val base =
    when {
      os.contains("win") -> System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home")
      os.contains("mac") -> "${System.getProperty("user.home")}/Library/Application Support"
      else -> System.getenv("XDG_DATA_HOME") ?: "${System.getProperty("user.home")}/.local/share"
    }
  return File(base, "LanguagePlatform").apply { mkdirs() }
}
