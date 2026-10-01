package com.app.platform.language.shared.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.app.platform.language.shared.db.LanguagePlatformDatabase
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module =
  module {
    single<HttpClientEngine> { Android.create() }
    single<SqlDriver> {
      AndroidSqliteDriver(LanguagePlatformDatabase.Schema, get<Context>(), "language_platform.db")
    }
  }
