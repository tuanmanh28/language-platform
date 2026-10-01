package com.app.platform.language.ui.components

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.app.platform.language.ui.resources.Res
import com.app.platform.language.ui.resources.common_retry
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LpPrimaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  isEnabled: Boolean = true,
) {
  Button(onClick = onClick, modifier = modifier, enabled = isEnabled) { Text(text) }
}

@Preview
@Composable
private fun LpPrimaryButtonEnabledPreview() {
  ComponentPreview { LpPrimaryButton(text = stringResource(Res.string.common_retry), onClick = {}) }
}

@Preview
@Composable
private fun LpPrimaryButtonDisabledPreview() {
  ComponentPreview {
    LpPrimaryButton(text = stringResource(Res.string.common_retry), onClick = {}, isEnabled = false)
  }
}
