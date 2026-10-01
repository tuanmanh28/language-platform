package com.app.platform.language.backend.fake

import com.app.platform.language.backend.auth.AuthIdentity
import com.app.platform.language.backend.auth.TokenError
import com.app.platform.language.backend.auth.TokenVerifier
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result

class FakeTokenVerifier(
  private val identities: Map<String, AuthIdentity> = emptyMap(),
) : TokenVerifier {
  var nextError: TokenError? = null

  override suspend fun verify(token: String): Result<AuthIdentity, TokenError> {
    nextError?.let { return Err(it) }
    return identities[token]?.let(::Ok) ?: Err(TokenError.Invalid("Unknown test token"))
  }
}
