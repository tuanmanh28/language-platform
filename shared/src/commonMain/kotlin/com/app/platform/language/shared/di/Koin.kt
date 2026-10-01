package com.app.platform.language.shared.di

import app.cash.sqldelight.db.SqlDriver
import com.app.platform.language.core.model.ContentJson
import com.app.platform.language.shared.db.LanguagePlatformDatabase
import com.app.platform.language.shared.network.ApiConfig
import com.app.platform.language.shared.network.createHttpClient
import io.ktor.client.engine.HttpClientEngine
import kotlinx.serialization.json.Json
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import kotlin.time.Clock

expect fun platformModule(): Module

private fun coreModule(config: ApiConfig): Module =
  module {
    single { config }
    single<Json> { ContentJson }
    single<Clock> { Clock.System }
    single { createHttpClient(get<HttpClientEngine>(), get(), get()) }
    single { LanguagePlatformDatabase(get<SqlDriver>()) }
  }

fun initKoin(
  config: ApiConfig,
  appDeclaration: KoinAppDeclaration = {},
): KoinApplication =
  startKoin {
    appDeclaration()
    modules(coreModule(config), readingModule, platformModule())
  }
