package com.app.platform.language.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiError(
  val message: String,
)
