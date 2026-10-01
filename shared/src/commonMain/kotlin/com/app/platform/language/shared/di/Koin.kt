package com.app.platform.language.shared.di

import app.cash.sqldelight.db.SqlDriver
import com.app.platform.language.core.model.ContentJson
import com.app.platform.language.shared.data.ReadingRepository
import com.app.platform.language.shared.db.LanguagePlatformDatabase
import com.app.platform.language.shared.network.ApiConfig
import com.app.platform.language.shared.network.KtorReadingApi
import com.app.platform.language.shared.network.ReadingApi
import com.app.platform.language.shared.network.createHttpClient
import com.app.platform.language.shared.reading.ReadingSessionViewModel
import com.app.platform.language.shared.reading.ReadingTestListViewModel
import io.ktor.client.engine.HttpClientEngine
import org.koin.core.KoinApplication
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/** Provides the platform-specific [HttpClientEngine] and [SqlDriver]. */
expect fun platformModule(): Module

fun sharedModule(config: ApiConfig): Module =
  module {
    single { config }
    single { ContentJson }
    single { createHttpClient(get<HttpClientEngine>(), get(), get()) }
    single<ReadingApi> { KtorReadingApi(get(), get()) }
    single { LanguagePlatformDatabase(get<SqlDriver>()) }
    single { ReadingRepository(get(), get(), get()) }

    viewModel { ReadingTestListViewModel(get()) }
    viewModel { (testId: String) -> ReadingSessionViewModel(testId, get()) }
  }

/** Call once at startup (Application.onCreate, main(), or the SwiftUI App init). */
fun initKoin(
  config: ApiConfig,
  appDeclaration: KoinAppDeclaration = {},
): KoinApplication =
  startKoin {
    appDeclaration()
    modules(sharedModule(config), platformModule())
  }

/**
 * Entry point for Swift: `SharedSdk.shared.start(baseUrl: "http://localhost:8080")`.
 * Swift cannot use Kotlin default arguments/lambdas, hence a dedicated function.
 */
object SharedSdk {
  private var started = false

  fun start(baseUrl: String) {
    if (started) return
    initKoin(ApiConfig(baseUrl = baseUrl))
    started = true
  }
}

/** Resolves ViewModels from Koin for SwiftUI: `ViewModels.shared.readingSession(testId: id)`. */
object ViewModels : KoinComponent {
  fun readingTestList(): ReadingTestListViewModel = get()

  fun readingSession(testId: String): ReadingSessionViewModel = get { parametersOf(testId) }
}
