package com.app.platform.language.android

import android.app.Application
import com.app.platform.language.shared.di.initKoin
import com.app.platform.language.shared.network.ApiConfig
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class LanguagePlatformApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    initKoin(ApiConfig(baseUrl = BuildConfig.API_BASE_URL, enableNetworkLogs = BuildConfig.DEBUG)) {
      androidLogger()
      androidContext(this@LanguagePlatformApplication)
    }
  }
}
