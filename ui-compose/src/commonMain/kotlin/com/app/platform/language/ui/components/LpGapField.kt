package com.app.platform.language.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.app.platform.language.ui.resources.Res
import com.app.platform.language.ui.resources.common_gap_max_words
import com.app.platform.language.ui.resources.common_gap_over_limit
import com.app.platform.language.ui.resources.common_gap_placeholder
import com.app.platform.language.ui.resources.common_gap_word_count
import com.app.platform.language.ui.resources.common_gap_word_count_of_limit
import com.app.platform.language.ui.theme.LanguagePlatformTheme
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LpGapField(
  value: String,
  wordCount: Int,
  maxWords: Int?,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val isOverLimit = maxWords != null && wordCount > maxWords
  val hint = maxWords?.let { stringResource(Res.string.common_gap_max_words, it) }
  val overLimitMessage = maxWords?.let { stringResource(Res.string.common_gap_over_limit, it) }
  val counter =
    if (maxWords != null) {
      stringResource(Res.string.common_gap_word_count_of_limit, wordCount, maxWords)
    } else {
      stringResource(Res.string.common_gap_word_count, wordCount)
    }

  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    singleLine = true,
    isError = isOverLimit,
    placeholder = { Text(stringResource(Res.string.common_gap_placeholder)) },
    supportingText = {
      Row(horizontalArrangement = Arrangement.spacedBy(LanguagePlatformTheme.spacing.sm)) {
        Text(text = (if (isOverLimit) overLimitMessage else hint).orEmpty(), modifier = Modifier.weight(1f))

        Text(counter)
      }
    },
    modifier = modifier,
  )
}

@Composable
private fun GapFieldPreview(
  value: String,
  wordCount: Int,
  maxWords: Int?,
) {
  ComponentPreview {
    LpGapField(value, wordCount, maxWords, onValueChange = {}, modifier = Modifier.fillMaxWidth())
  }
}

@Preview
@Composable
private fun LpGapFieldEmptyPreview() {
  GapFieldPreview(value = "", wordCount = 0, maxWords = 2)
}

@Preview
@Composable
private fun LpGapFieldWithinLimitPreview() {
  GapFieldPreview(value = ComponentPreviewData.GAP_ANSWER, wordCount = 2, maxWords = 2)
}

@Preview
@Composable
private fun LpGapFieldOverLimitPreview() {
  GapFieldPreview(value = ComponentPreviewData.GAP_ANSWER_TOO_LONG, wordCount = 3, maxWords = 2)
}

@Preview
@Composable
private fun LpGapFieldWithoutLimitPreview() {
  GapFieldPreview(value = ComponentPreviewData.GAP_ANSWER, wordCount = 2, maxWords = null)
}
