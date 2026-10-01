package com.app.platform.language.backend.auth

import com.app.platform.language.backend.fake.FakeJwkProvider
import com.auth0.jwk.Jwk
import com.auth0.jwk.NetworkException
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.algorithms.Algorithm
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.getError
import kotlinx.coroutines.test.runTest
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FirebaseTokenVerifierTest {
  private val now = Instant.parse("2026-10-01T10:00:00Z")
  private val signingKey = rsaKeyPair()
  private val keys = FakeJwkProvider(mapOf(KEY_ID to signingKey.toJwk(KEY_ID)))
  private val verifier = FirebaseTokenVerifier(PROJECT_ID, keys, Clock.fixed(now, ZoneOffset.UTC))

  @Test
  fun validTokenYieldsIdentity() =
    runTest {
      val result = verifier.verify(token())

      assertEquals(Ok(AuthIdentity(uid = "uid-1", email = "learner@example.com", displayName = "Lan")), result)
    }

  @Test
  fun tokenWithoutProfileClaimsYieldsIdentityWithoutProfile() =
    runTest {
      val token = token { withClaim("email", null as String?).withClaim("name", null as String?) }

      assertEquals(Ok(AuthIdentity(uid = "uid-1", email = null, displayName = null)), verifier.verify(token))
    }

  @Test
  fun tokenForAnotherProjectIsInvalid() =
    runTest {
      assertInvalid(token { withAudience("other-project") })
    }

  @Test
  fun tokenFromAnotherIssuerIsInvalid() =
    runTest {
      assertInvalid(token { withIssuer("https://securetoken.google.com/other-project") })
    }

  @Test
  fun expiredTokenIsInvalid() =
    runTest {
      assertInvalid(token { withExpiresAt(now.minusSeconds(1)) })
    }

  @Test
  fun tokenIssuedInTheFutureIsInvalid() =
    runTest {
      assertInvalid(token { withIssuedAt(now.plusSeconds(60)) })
    }

  @Test
  fun authTimeInTheFutureIsInvalid() =
    runTest {
      assertInvalid(token { withClaim("auth_time", now.plusSeconds(60)) })
    }

  @Test
  fun missingAuthTimeIsInvalid() =
    runTest {
      assertInvalid(token { withClaim("auth_time", null as Instant?) })
    }

  @Test
  fun emptySubjectIsInvalid() =
    runTest {
      assertInvalid(token { withSubject("") })
    }

  @Test
  fun tokenSignedWithAnotherKeyIsInvalid() =
    runTest {
      assertInvalid(token(signedWith = rsaKeyPair()))
    }

  @Test
  fun tokenWithUnknownKeyIdIsInvalid() =
    runTest {
      assertInvalid(token { withKeyId("unknown-key") })
    }

  @Test
  fun tokenWithoutKeyIdIsInvalid() =
    runTest {
      assertInvalid(token { withKeyId(null) })
    }

  @Test
  fun tokenWithSymmetricAlgorithmIsInvalid() =
    runTest {
      val token = baseToken().sign(Algorithm.HMAC256("shared-secret"))

      assertInvalid(token)
    }

  @Test
  fun malformedTokenIsInvalid() =
    runTest {
      assertInvalid("not-a-jwt")
    }

  @Test
  fun unreachableKeysMakeTokenUnverifiable() =
    runTest {
      keys.nextError = NetworkException("Cannot fetch keys", null)

      assertIs<TokenError.Unverifiable>(verifier.verify(token()).getError())
    }

  private suspend fun assertInvalid(token: String) {
    assertIs<TokenError.Invalid>(verifier.verify(token).getError())
  }

  private fun token(
    signedWith: KeyPair = signingKey,
    customize: JWTCreator.Builder.() -> JWTCreator.Builder = { this },
  ): String =
    baseToken()
      .customize()
      .sign(Algorithm.RSA256(signedWith.public as RSAPublicKey, signedWith.private as RSAPrivateKey))

  private fun baseToken(): JWTCreator.Builder =
    JWT
      .create()
      .withKeyId(KEY_ID)
      .withIssuer("https://securetoken.google.com/$PROJECT_ID")
      .withAudience(PROJECT_ID)
      .withSubject("uid-1")
      .withIssuedAt(now.minusSeconds(60))
      .withExpiresAt(now.plusSeconds(3600))
      .withClaim("auth_time", now.minusSeconds(120))
      .withClaim("email", "learner@example.com")
      .withClaim("name", "Lan")

  private fun rsaKeyPair(): KeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

  private fun KeyPair.toJwk(keyId: String): Jwk {
    val key = public as RSAPublicKey
    return Jwk.fromValues(
      mapOf(
        "kid" to keyId,
        "kty" to "RSA",
        "alg" to "RS256",
        "use" to "sig",
        "n" to key.modulus.toBase64Url(),
        "e" to key.publicExponent.toBase64Url(),
      ),
    )
  }

  private fun BigInteger.toBase64Url(): String = Base64.getUrlEncoder().withoutPadding().encodeToString(toByteArray())

  private companion object {
    const val PROJECT_ID = "language-platform-test"
    const val KEY_ID = "key-1"
  }
}
