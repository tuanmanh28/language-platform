package com.app.platform.language.ui.reading

import androidx.compose.runtime.Composable
import com.app.platform.language.core.model.IeltsModule
import com.app.platform.language.core.model.ReadingError
import com.app.platform.language.ui.resources.Res
import com.app.platform.language.ui.resources.reading_common_error_not_found
import com.app.platform.language.ui.resources.reading_common_error_offline
import com.app.platform.language.ui.resources.reading_common_error_unexpected
import com.app.platform.language.ui.resources.reading_common_module_academic
import com.app.platform.language.ui.resources.reading_common_module_general_training
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ReadingError.toUserMessage(): String =
  stringResource(
    when (this) {
      ReadingError.NotFound -> Res.string.reading_common_error_not_found
      ReadingError.Offline -> Res.string.reading_common_error_offline
      is ReadingError.Unexpected -> Res.string.reading_common_error_unexpected
    },
  )

@Composable
internal fun IeltsModule.label(): String =
  stringResource(
    when (this) {
      IeltsModule.ACADEMIC -> Res.string.reading_common_module_academic
      IeltsModule.GENERAL_TRAINING -> Res.string.reading_common_module_general_training
    },
  )
