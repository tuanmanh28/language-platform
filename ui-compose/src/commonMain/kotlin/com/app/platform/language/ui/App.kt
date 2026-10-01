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

  data class Session(
    val testId: String,
  ) : Screen
}

@Composable
fun LanguagePlatformApp() {
  LanguagePlatformTheme {
    var screen by remember { mutableStateOf<Screen>(Screen.TestList) }

    when (val current = screen) {
      Screen.TestList -> {
        ReadingTestListScreen(
          onOpenTest = { testId -> screen = Screen.Session(testId) },
        )
      }

      is Screen.Session -> {
        ScopedViewModels(key = current) {
          ReadingSessionScreen(
            testId = current.testId,
            onExit = { screen = Screen.TestList },
          )
        }
      }
    }
  }
}

// Each visit gets its own ViewModelStore, so reopening a test always starts a fresh attempt.
@Composable
private fun ScopedViewModels(
  key: Any,
  content: @Composable () -> Unit,
) {
  val owner =
    remember(key) {
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

@Composable
internal expect fun PlatformBackHandler(
  enabled: Boolean = true,
  onBack: () -> Unit,
)
