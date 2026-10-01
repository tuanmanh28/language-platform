package com.app.platform.language.shared.di

import com.app.platform.language.shared.db.LanguagePlatformDatabase
import com.app.platform.language.shared.reading.ReadingSessionViewModel
import com.app.platform.language.shared.reading.ReadingTestListViewModel
import com.app.platform.language.shared.reading.data.KtorReadingApi
import com.app.platform.language.shared.reading.data.OfflineFirstReadingRepository
import com.app.platform.language.shared.reading.data.ReadingApi
import com.app.platform.language.shared.reading.data.ReadingDao
import com.app.platform.language.shared.reading.data.ReadingRepository
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val readingModule: Module =
  module {
    single<ReadingApi> { KtorReadingApi(get()) }
    single { ReadingDao(get<LanguagePlatformDatabase>().languagePlatformQueries, get(), Dispatchers.Default) }
    single<ReadingRepository> { OfflineFirstReadingRepository(get(), get(), get()) }

    viewModelOf(::ReadingTestListViewModel)
    viewModel { (testId: String) -> ReadingSessionViewModel(testId, get(), get()) }
  }
