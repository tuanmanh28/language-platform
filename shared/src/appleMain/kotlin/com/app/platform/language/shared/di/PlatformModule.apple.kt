package com.app.platform.language.shared.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.app.platform.language.shared.db.LanguagePlatformDatabase
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.dsl.module

// Dùng chung cho iOS và macOS.
actual fun platformModule(): Module = module {
    single<HttpClientEngine> { Darwin.create() }
    single<SqlDriver> { NativeSqliteDriver(LanguagePlatformDatabase.Schema, "language_platform.db") }
}
