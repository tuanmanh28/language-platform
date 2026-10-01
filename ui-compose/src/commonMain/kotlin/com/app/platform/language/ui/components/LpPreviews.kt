package com.app.platform.language.ui.components

import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_NO
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "phone-light", widthDp = 390, heightDp = 844, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "phone-dark", widthDp = 390, heightDp = 844, uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "phone-light-font2x", widthDp = 390, heightDp = 844, uiMode = UI_MODE_NIGHT_NO, fontScale = 2f)
@Preview(name = "phone-dark-font2x", widthDp = 390, heightDp = 844, uiMode = UI_MODE_NIGHT_YES, fontScale = 2f)
@Preview(name = "desktop-light", widthDp = 1280, heightDp = 800, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "desktop-dark", widthDp = 1280, heightDp = 800, uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "desktop-light-font2x", widthDp = 1280, heightDp = 800, uiMode = UI_MODE_NIGHT_NO, fontScale = 2f)
@Preview(name = "desktop-dark-font2x", widthDp = 1280, heightDp = 800, uiMode = UI_MODE_NIGHT_YES, fontScale = 2f)
// Named with the design-system Lp prefix like every other shared UI building block.
@Suppress("ktlint:compose:preview-annotation-naming")
internal annotation class LpPreviews
