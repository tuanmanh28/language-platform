package com.app.platform.language.ui.components

import androidx.compose.runtime.Composable
import com.app.platform.language.ui.resources.Res
import com.app.platform.language.ui.resources.common_answer_correct
import com.app.platform.language.ui.resources.common_answer_wrong
import com.app.platform.language.ui.resources.common_mark_correct
import com.app.platform.language.ui.resources.common_mark_wrong
import org.jetbrains.compose.resources.stringResource

internal enum class AnswerState {
  IDLE,
  SELECTED,
  CORRECT,
  WRONG,
}

internal val AnswerState.isReviewed: Boolean
  get() = this == AnswerState.CORRECT || this == AnswerState.WRONG

// Selection is announced through the selectable semantics; only the review verdict needs words.
@Composable
internal fun AnswerState.verdict(): String? =
  when (this) {
    AnswerState.IDLE, AnswerState.SELECTED -> null
    AnswerState.CORRECT -> stringResource(Res.string.common_answer_correct)
    AnswerState.WRONG -> stringResource(Res.string.common_answer_wrong)
  }

@Composable
internal fun AnswerState.mark(): String? =
  when (this) {
    AnswerState.IDLE, AnswerState.SELECTED -> null
    AnswerState.CORRECT -> stringResource(Res.string.common_mark_correct)
    AnswerState.WRONG -> stringResource(Res.string.common_mark_wrong)
  }
