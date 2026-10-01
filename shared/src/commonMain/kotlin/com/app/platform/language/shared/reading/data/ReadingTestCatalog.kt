package com.app.platform.language.shared.reading.data

import com.app.platform.language.core.model.ReadingTestSummary

internal data class ReadingTestCatalog(
  val tests: List<ReadingTestSummary>,
  val isOffline: Boolean,
)
