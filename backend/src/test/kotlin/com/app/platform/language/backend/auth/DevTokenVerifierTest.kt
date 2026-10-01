package com.app.platform.language.backend.auth

import com.app.platform.language.backend.fake.FakeTokenVerifier
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.getError
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DevTokenVerifierTest {
  private val learner = AuthIdentity("learner-uid", "learner@example.com", "Lan")
  private val verifier =
    DevTokenVerifier(
      devToken = DEV_TOKEN,
      ownerEmail = "owner@example.com",
      fallback = FakeTokenVerifier(mapOf(LEARNER_TOKEN to learner)),
    )

  @Test
  fun devTokenYieldsVerifiedOwner() =
    runTest {
      assertEquals(
        Ok(AuthIdentity("dev-owner", "owner@example.com", "Dev owner", isEmailVerified = true)),
        verifier.verify(DEV_TOKEN),
      )
    }

  @Test
  fun otherTokensAreVerifiedByTheFallback() =
    runTest {
      assertEquals(Ok(learner), verifier.verify(LEARNER_TOKEN))
    }

  @Test
  fun unknownTokenIsInvalid() =
    runTest {
      assertIs<TokenError.Invalid>(verifier.verify("$DEV_TOKEN-guess").getError())
    }

  private companion object {
    const val DEV_TOKEN = "dev-token"
    const val LEARNER_TOKEN = "learner-token"
  }
}
