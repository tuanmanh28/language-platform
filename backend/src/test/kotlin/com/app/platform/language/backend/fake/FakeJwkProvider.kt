package com.app.platform.language.backend.fake

import com.auth0.jwk.Jwk
import com.auth0.jwk.JwkException
import com.auth0.jwk.JwkProvider
import com.auth0.jwk.SigningKeyNotFoundException

class FakeJwkProvider(
  private val keys: Map<String, Jwk>,
) : JwkProvider {
  var nextError: JwkException? = null

  override fun get(keyId: String): Jwk {
    nextError?.let { throw it }
    return keys[keyId] ?: throw SigningKeyNotFoundException("No key with id $keyId", null)
  }
}
