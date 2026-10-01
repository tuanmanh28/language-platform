package com.app.platform.language.backend.config

data class DevAuthConfig(
  val token: String,
  val ownerEmail: String,
) {
  override fun toString(): String = "DevAuthConfig(token=***, ownerEmail=$ownerEmail)"
}
