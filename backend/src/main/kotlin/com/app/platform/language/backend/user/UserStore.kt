package com.app.platform.language.backend.user

import com.app.platform.language.backend.auth.AuthIdentity

interface UserStore {
  suspend fun upsert(identity: AuthIdentity): User
}
