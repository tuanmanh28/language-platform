package com.app.platform.language.backend.plugins

import com.app.platform.language.backend.auth.TokenError
import com.app.platform.language.backend.auth.TokenVerifier
import com.github.michaelbull.result.get
import com.github.michaelbull.result.onErr
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.bearer

const val FIREBASE_AUTH = "firebase"

fun Application.configureAuthentication(verifier: TokenVerifier) {
  install(Authentication) {
    bearer(FIREBASE_AUTH) {
      realm = "language-platform"
      authenticate { credential ->
        verifier
          .verify(credential.token)
          .onErr { error ->
            when (error) {
              is TokenError.Invalid -> application.log.debug("Rejected token: {}", error.reason)
              is TokenError.Unverifiable -> application.log.error("Token could not be verified", error.cause)
            }
          }.get()
      }
    }
  }
}
