package com.app.platform.language.backend.auth

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import java.security.MessageDigest

class DevTokenVerifier(
  private val devToken: String,
  ownerEmail: String,
  private val fallback: TokenVerifier,
) : TokenVerifier {
  private val owner = AuthIdentity(uid = DEV_UID, email = ownerEmail, displayName = DEV_NAME, isEmailVerified = true)

  override suspend fun verify(token: String): Result<AuthIdentity, TokenError> =
    if (MessageDigest.isEqual(token.toByteArray(), devToken.toByteArray())) Ok(owner) else fallback.verify(token)

  private companion object {
    const val DEV_UID = "dev-owner"
    const val DEV_NAME = "Dev owner"
  }
}
