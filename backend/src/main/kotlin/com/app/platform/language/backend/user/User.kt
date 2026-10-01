package com.app.platform.language.backend.user

import com.app.platform.language.core.model.UserProfile
import kotlin.uuid.Uuid

data class User(
  val id: Uuid,
  val email: String?,
  val displayName: String?,
) {
  fun toProfile(): UserProfile = UserProfile(id = id.toString(), email = email, displayName = displayName)
}
