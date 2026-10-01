package com.app.platform.language.ui.components

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.app.platform.language.ui.resources.Res
import com.app.platform.language.ui.resources.common_close
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LpTextButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  isEnabled: Boolean = true,
) {
  TextButton(onClick = onClick, modifier = modifier, enabled = isEnabled) { Text(text) }
}

@Preview
@Composable
private fun LpTextButtonEnabledPreview() {
  ComponentPreview { LpTextButton(text = stringResource(Res.string.common_close), onClick = {}) }
}

@Preview
@Composable
private fun LpTextButtonDisabledPreview() {
  ComponentPreview {
    LpTextButton(text = stringResource(Res.string.common_close), onClick = {}, isEnabled = false)
  }
}
