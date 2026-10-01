package com.app.platform.language.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.app.platform.language.shared.di.initKoin
import com.app.platform.language.shared.network.ApiConfig
import com.app.platform.language.ui.LanguagePlatformApp

fun main() {
    // Ghi đè bằng biến môi trường API_BASE_URL khi cần (vd. trỏ tới staging).
    val baseUrl = System.getenv("API_BASE_URL") ?: "http://localhost:8080"
    initKoin(ApiConfig(baseUrl = baseUrl, enableNetworkLogs = true))

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Language Platform",
            state = rememberWindowState(width = 1200.dp, height = 800.dp),
        ) {
            LanguagePlatformApp()
        }
    }
}
