package com.app.platform.language.core.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
  val id: String,
  val email: String?,
  val displayName: String?,
)
