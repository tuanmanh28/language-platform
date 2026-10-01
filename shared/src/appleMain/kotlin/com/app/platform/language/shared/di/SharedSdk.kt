package com.app.platform.language.shared.di

import com.app.platform.language.shared.network.ApiConfig
import org.koin.mp.KoinPlatformTools

// Swift cannot call initKoin because of its default argument and lambda parameter.
object SharedSdk {
  fun start(baseUrl: String) {
    if (KoinPlatformTools.defaultContext().getOrNull() != null) return
    initKoin(ApiConfig(baseUrl = baseUrl))
  }
}
