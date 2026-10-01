package com.app.platform.language.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.app.platform.language.ui.theme.LanguagePlatformTheme
import kotlin.test.Test
import kotlin.test.assertEquals

// Every component is rendered through ComponentPreview, so each node appears once per theme (light and dark).
private const val THEME_COUNT = 2

@OptIn(ExperimentalTestApi::class)
class ComponentsTest {
  private val hasStateDescription = SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription)
  private val hasError = SemanticsMatcher.keyIsDefined(SemanticsProperties.Error)
  private val hasProgress = SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)

  private fun ComposeUiTest.setThemedContent(content: @Composable () -> Unit) {
    setContent { ComponentPreview { Column { content() } } }
  }

  private fun ComposeUiTest.assertShownInBothThemes(text: String) =
    onAllNodesWithText(text).assertCountEquals(THEME_COUNT)

  @Test
  fun buttonsRenderEnabledAndDisabled() =
    runComposeUiTest {
      setThemedContent {
        LpPrimaryButton("primary", onClick = {})
        LpPrimaryButton("primary disabled", onClick = {}, isEnabled = false)
        LpSecondaryButton("secondary", onClick = {})
        LpSecondaryButton("secondary disabled", onClick = {}, isEnabled = false)
        LpTextButton("text", onClick = {})
        LpTextButton("text disabled", onClick = {}, isEnabled = false)
      }

      listOf("primary", "secondary", "text").forEach { label ->
        assertShownInBothThemes(label).assertAll(isEnabled())
        assertShownInBothThemes("$label disabled").assertAll(isNotEnabled())
      }
    }

  @Test
  fun answerChipRendersEveryState() =
    runComposeUiTest {
      setThemedContent {
        AnswerState.entries.forEach { LpAnswerChip(it.name, it, onClick = {}) }
      }

      AnswerState.entries.forEach { state ->
        val nodes = assertShownInBothThemes(state.name)
        if (state.isReviewed) nodes.assertAll(isNotEnabled() and hasStateDescription) else nodes.assertAll(isEnabled())
      }
      onAllNodes(isSelected()).assertCountEquals(THEME_COUNT)
    }

  @Test
  fun optionRowRendersEveryState() =
    runComposeUiTest {
      setThemedContent {
        AnswerState.entries.forEach { LpOptionRow(key = "A", text = it.name, state = it, onClick = {}) }
      }

      AnswerState.entries.forEach { state ->
        val nodes = onAllNodes(hasText(state.name, substring = true)).assertCountEquals(THEME_COUNT)
        if (state.isReviewed) nodes.assertAll(isNotEnabled() and hasStateDescription) else nodes.assertAll(isEnabled())
      }
      onAllNodes(isSelected()).assertCountEquals(THEME_COUNT)
    }

  @Test
  fun answerChipReportsClicksWhileAnswerable() =
    runComposeUiTest {
      var clicks = 0
      setThemedContent { LpAnswerChip("TRUE", AnswerState.IDLE, onClick = { clicks++ }) }

      onAllNodesWithText("TRUE").onFirst().performClick()

      assertEquals(1, clicks)
    }

  @Test
  fun gapFieldRendersEveryState() =
    runComposeUiTest {
      setThemedContent {
        LpGapField(value = "", wordCount = 0, maxWords = 2, onValueChange = {})
        LpGapField(value = "solar power", wordCount = 2, maxWords = 2, onValueChange = {})
        LpGapField(value = "cheap solar power", wordCount = 3, maxWords = 2, onValueChange = {})
        LpGapField(value = "solar energy", wordCount = 2, maxWords = null, onValueChange = {})
      }

      onAllNodes(hasSetTextAction()).assertCountEquals(4 * THEME_COUNT)
      onAllNodes(hasError).assertCountEquals(THEME_COUNT)
      onAllNodes(hasText("cheap solar power") and hasError).assertCountEquals(THEME_COUNT)
    }

  @Test
  fun timerBarRendersNormalAndWarning() =
    runComposeUiTest {
      setThemedContent {
        LpTimerBar("42:15", progress = 0.7f, state = TimerBarState.NORMAL)
        LpTimerBar("00:45", progress = 0.01f, state = TimerBarState.WARNING)
      }

      onAllNodes(hasContentDescription("42:15", substring = true)).assertCountEquals(THEME_COUNT)
      onAllNodes(hasContentDescription("00:45", substring = true) and hasStateDescription)
        .assertCountEquals(THEME_COUNT)
      onAllNodes(hasStateDescription).assertCountEquals(THEME_COUNT)
    }

  @Test
  fun bandBadgeRendersEverySize() =
    runComposeUiTest {
      setThemedContent {
        BandBadgeSize.entries.forEach { LpBandBadge(band = 7.5, size = it) }
      }

      onAllNodes(hasContentDescription("7.5", substring = true))
        .assertCountEquals(BandBadgeSize.entries.size * THEME_COUNT)
    }

  @Test
  fun testCardOpensOnClick() =
    runComposeUiTest {
      var clicks = 0
      setThemedContent { LpTestCard(title = "Cambridge 18", details = "Academic", onClick = { clicks++ }) }

      assertShownInBothThemes("Cambridge 18").onFirst().performClick()

      assertEquals(1, clicks)
    }

  @Test
  fun disabledTestCardIgnoresClicks() =
    runComposeUiTest {
      var clicks = 0
      setThemedContent {
        LpTestCard(title = "Listening", details = "Sắp có", onClick = { clicks++ }, state = TestCardState.DISABLED)
      }

      onAllNodes(hasText("Listening") and isNotEnabled()).assertCountEquals(THEME_COUNT).onFirst().performClick()

      assertEquals(0, clicks)
    }

  @Test
  fun progressRingRendersIndeterminateAndDeterminate() =
    runComposeUiTest {
      setThemedContent {
        LpProgressRing(progress = null)
        LpProgressRing(progress = 0.5f, size = ProgressRingSize.LARGE, label = "50%")
      }

      onAllNodes(hasProgress).assertCountEquals(2 * THEME_COUNT)
      assertShownInBothThemes("50%")
    }

  @Test
  fun emptyStateRendersWithAndWithoutAction() =
    runComposeUiTest {
      setThemedContent {
        LpEmptyState(title = "title only")
        LpEmptyState(title = "with action", message = "message", action = { LpTextButton("action", onClick = {}) })
      }

      assertShownInBothThemes("title only")
      assertShownInBothThemes("with action")
      assertShownInBothThemes("message")
      assertShownInBothThemes("action")
    }

  @Test
  fun errorStateRetries() =
    runComposeUiTest {
      var retries = 0
      setThemedContent { LpErrorState(message = "offline", onRetry = { retries++ }) }

      assertShownInBothThemes("offline")
      onAllNodes(hasClickAction()).onFirst().performClick()

      assertEquals(1, retries)
    }

  @Test
  fun explainSheetShowsTheExplanationInBothThemes() {
    listOf(false, true).forEach { darkTheme ->
      runComposeUiTest {
        setContent {
          LanguagePlatformTheme(darkTheme = darkTheme) {
            LpExplainSheet(explanation = "because paragraph B says so", onDismiss = {})
          }
        }

        onNodeWithText("because paragraph B says so").assertExists()
      }
    }
  }
}
