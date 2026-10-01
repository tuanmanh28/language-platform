package com.app.platform.language.backend.auth

import com.github.michaelbull.result.Result

interface TokenVerifier {
  suspend fun verify(token: String): Result<AuthIdentity, TokenError>
}
