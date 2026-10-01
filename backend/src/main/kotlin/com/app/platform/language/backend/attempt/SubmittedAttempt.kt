package com.app.platform.language.backend.attempt

import com.app.platform.language.core.model.Attempt

data class SubmittedAttempt(
  val attempt: Attempt,
  val isNew: Boolean,
)
