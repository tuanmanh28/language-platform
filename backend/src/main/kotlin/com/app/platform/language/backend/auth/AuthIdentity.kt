package com.app.platform.language.backend.auth

data class AuthIdentity(
  val uid: String,
  val email: String?,
  val displayName: String?,
)
