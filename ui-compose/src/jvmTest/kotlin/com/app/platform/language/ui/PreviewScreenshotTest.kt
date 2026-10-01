package com.app.platform.language.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.LocalSystemTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.SystemTheme
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_MASK
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import sergio.sastre.composable.preview.scanner.android.AndroidComposablePreviewScanner
import sergio.sastre.composable.preview.scanner.android.AndroidPreviewInfo
import sergio.sastre.composable.preview.scanner.core.preview.ComposablePreview

private const val SCREENSHOTS_DIR = "build/screenshots"
private const val PREVIEW_TAG = "preview"
private const val RENDER_DENSITY = 2f
private const val DEFAULT_WIDTH_DP = 390
private const val MAX_HEIGHT_DP = 2000
private const val SETTLE_TIME_MILLIS = 1_000L
private const val DEFAULT_VARIANT = "default"

@OptIn(ExperimentalTestApi::class)
@RunWith(Parameterized::class)
class PreviewScreenshotTest(
  private val screenshot: PreviewScreenshot,
) {
  @Test
  fun rendersPreview() {
    val info = screenshot.preview.previewInfo
    val widthDp = info.widthDp.takeIf { it > 0 } ?: DEFAULT_WIDTH_DP
    val hasFixedHeight = info.heightDp > 0
    val heightDp = if (hasFixedHeight) info.heightDp else MAX_HEIGHT_DP
    val size =
      if (hasFixedHeight) Modifier.requiredSize(widthDp.dp, heightDp.dp) else Modifier.requiredWidth(widthDp.dp)

    runDesktopComposeUiTest(width = widthDp.toPx(), height = heightDp.toPx()) {
      // A paused clock advanced by a fixed amount makes animations land on the same frame every run.
      mainClock.autoAdvance = false
      setContent {
        PreviewEnvironment(info) {
          Box(size.testTag(PREVIEW_TAG)) { screenshot.preview() }
        }
      }
      mainClock.advanceTimeBy(SETTLE_TIME_MILLIS)

      onNodeWithTag(PREVIEW_TAG).captureRoboImage("$SCREENSHOTS_DIR/${screenshot.name}.png")
    }
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun screenshots(): List<PreviewScreenshot> {
      val screenshots =
        AndroidComposablePreviewScanner()
          .scanPackageTrees("com.app.platform.language.ui")
          .includePrivatePreviews()
          .getPreviews()
          .map(::PreviewScreenshot)
          .sortedBy { it.name }
      val duplicates = screenshots.groupBy { it.name }.filterValues { it.size > 1 }.keys
      check(duplicates.isEmpty()) { "Preview names must be unique: $duplicates" }
      return screenshots
    }
  }
}

class PreviewScreenshot(
  val preview: ComposablePreview<AndroidPreviewInfo>,
) {
  val name = "${preview.methodName}_${preview.previewInfo.name.ifEmpty { DEFAULT_VARIANT }}"

  override fun toString() = name
}

// isSystemInDarkTheme reads LocalSystemTheme on desktop and Compose 1.12 offers no public replacement yet.
@Suppress("DEPRECATION")
@OptIn(InternalComposeUiApi::class)
@Composable
private fun PreviewEnvironment(
  info: AndroidPreviewInfo,
  content: @Composable () -> Unit,
) {
  val isDark = (info.uiMode and UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES
  CompositionLocalProvider(
    LocalDensity provides Density(RENDER_DENSITY, info.fontScale),
    LocalSystemTheme provides if (isDark) SystemTheme.Dark else SystemTheme.Light,
    content = content,
  )
}

private fun Int.toPx(): Int = (this * RENDER_DENSITY).toInt()
