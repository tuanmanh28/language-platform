package com.app.platform.language.backend.attempt

import kotlin.time.Instant

data class AttemptQuery(
  val since: Instant?,
  val cursor: AttemptCursor?,
  val limit: Int,
) {
  companion object {
    const val DEFAULT_LIMIT = 50
    const val MAX_LIMIT = 100
  }
}
