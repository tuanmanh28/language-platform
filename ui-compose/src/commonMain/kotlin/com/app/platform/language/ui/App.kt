package com.app.platform.language.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.app.platform.language.ui.reading.ReadingSessionScreen
import com.app.platform.language.ui.reading.ReadingTestListScreen
import com.app.platform.language.ui.theme.LanguagePlatformTheme

private sealed interface Screen {
    data object TestList : Screen
    data class Session(val testId: String) : Screen
}

/** Màn hình gốc cho Android và Desktop. Koin phải được khởi tạo trước (initKoin). */
@Composable
fun LanguagePlatformApp() {
    LanguagePlatformTheme {
        var screen by remember { mutableStateOf<Screen>(Screen.TestList) }

        when (val current = screen) {
            Screen.TestList -> ReadingTestListScreen(
                onOpenTest = { testId -> screen = Screen.Session(testId) },
            )
            is Screen.Session -> ScopedViewModels(key = current) {
                ReadingSessionScreen(
                    testId = current.testId,
                    onExit = { screen = Screen.TestList },
                )
            }
        }
    }
}

/**
 * Mỗi lần mở một màn hình có ViewModelStore riêng, bị xoá khi rời màn hình.
 * Nhờ vậy mỗi lần vào làm bài là một phiên mới (timer không thể "tạm dừng" bằng cách thoát ra rồi vào lại).
 * Khi app có nhiều màn hình hơn, thay bằng navigation-compose (mỗi back-stack entry tự có store).
 */
@Composable
private fun ScopedViewModels(key: Any, content: @Composable () -> Unit) {
    val owner = remember(key) {
        object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = ViewModelStore()
        }
    }
    DisposableEffect(owner) {
        onDispose { owner.viewModelStore.clear() }
    }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
        content()
    }
}

/** Nút Back hệ thống (Android). Desktop không có nên là no-op. */
@Composable
internal expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)
