package com.app.platform.language.backend.attempt

import com.app.platform.language.core.model.Attempt
import kotlin.time.Instant
import kotlin.uuid.Uuid

data class AttemptCursor(
  val syncedAt: Instant,
  val id: Uuid,
) {
  fun encode(): String = "${syncedAt}$SEPARATOR$id"

  companion object {
    private const val SEPARATOR = '_'

    fun after(attempt: Attempt): AttemptCursor = AttemptCursor(attempt.syncedAt, Uuid.parse(attempt.id))

    fun decodeOrNull(value: String): AttemptCursor? {
      val syncedAt = Instant.parseOrNull(value.substringBefore(SEPARATOR))
      val id = Uuid.parseOrNull(value.substringAfter(SEPARATOR, missingDelimiterValue = ""))
      return if (syncedAt != null && id != null) AttemptCursor(syncedAt, id) else null
    }
  }
}
