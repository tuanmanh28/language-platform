package com.app.platform.language.backend.user

import com.app.platform.language.backend.auth.AuthIdentity
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.mapError

class UserService(
  private val store: UserStore,
) {
  suspend fun syncUser(identity: AuthIdentity): Result<User, UserError> =
    runSuspendCatching { store.upsert(identity) }.mapError(UserError::Unexpected)
}
