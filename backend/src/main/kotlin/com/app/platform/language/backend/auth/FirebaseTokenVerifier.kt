package com.app.platform.language.backend.auth

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwk.NetworkException
import com.auth0.jwk.SigningKeyNotFoundException
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTDecodeException
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.mapError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.security.interfaces.RSAPublicKey
import java.time.Clock
import java.util.concurrent.TimeUnit

class FirebaseTokenVerifier(
  private val projectId: String,
  private val keys: JwkProvider = googleSigningKeys(),
  private val clock: Clock = Clock.systemUTC(),
) : TokenVerifier {
  override suspend fun verify(token: String): Result<AuthIdentity, TokenError> =
    runSuspendCatching { withContext(Dispatchers.IO) { verifiedJwt(token) } }
      .mapError { it.toTokenError() }
      .andThen { it.toIdentity() }

  private fun verifiedJwt(token: String): DecodedJWT {
    val keyId = JWT.decode(token).keyId ?: throw JWTDecodeException("Token has no key id")
    val publicKey = keys.get(keyId).publicKey as RSAPublicKey
    val verification =
      JWT
        .require(Algorithm.RSA256(publicKey, null))
        .withIssuer("$ISSUER_PREFIX$projectId")
        .withAudience(projectId)
        .withClaim(AUTH_TIME_CLAIM) { claim, _ -> claim.asInstant()?.let { !it.isAfter(clock.instant()) } ?: false }
    return (verification as JWTVerifier.BaseVerification).build(clock).verify(token)
  }

  private fun DecodedJWT.toIdentity(): Result<AuthIdentity, TokenError> {
    val uid = subject?.takeIf { it.isNotBlank() && it.length <= MAX_UID_LENGTH }
    return if (uid == null) {
      Err(TokenError.Invalid("Token has no valid subject"))
    } else {
      Ok(AuthIdentity(uid = uid, email = getClaim("email").asString(), displayName = getClaim("name").asString()))
    }
  }

  private fun Throwable.toTokenError(): TokenError =
    when (this) {
      is NetworkException -> TokenError.Unverifiable(this)
      is SigningKeyNotFoundException -> TokenError.Invalid("Token is signed with an unknown key")
      is JWTVerificationException -> TokenError.Invalid(message ?: "Token verification failed")
      else -> TokenError.Unverifiable(this)
    }

  private companion object {
    const val ISSUER_PREFIX = "https://securetoken.google.com/"
    const val AUTH_TIME_CLAIM = "auth_time"
    const val MAX_UID_LENGTH = 128
    const val KEYS_URL = "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"
    const val CACHED_KEYS = 10L
    const val CACHE_HOURS = 6L
    const val FETCHES_PER_MINUTE = 10L
    const val TIMEOUT_MILLIS = 5_000

    fun googleSigningKeys(): JwkProvider =
      JwkProviderBuilder(URI(KEYS_URL).toURL())
        .cached(CACHED_KEYS, CACHE_HOURS, TimeUnit.HOURS)
        .rateLimited(FETCHES_PER_MINUTE, 1, TimeUnit.MINUTES)
        .timeouts(TIMEOUT_MILLIS, TIMEOUT_MILLIS)
        .build()
  }
}
