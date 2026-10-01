package com.app.platform.language.backend.fake

import com.app.platform.language.backend.auth.AuthIdentity
import com.app.platform.language.backend.user.User
import com.app.platform.language.backend.user.UserStore
import kotlin.uuid.Uuid

class FakeUserStore : UserStore {
  private val usersByUid = mutableMapOf<String, User>()
  var nextError: Throwable? = null

  val users: Collection<User> get() = usersByUid.values

  override suspend fun upsert(identity: AuthIdentity): User {
    nextError?.let { throw it }
    val id = usersByUid[identity.uid]?.id ?: Uuid.random()
    return User(id, identity.email, identity.displayName).also { usersByUid[identity.uid] = it }
  }
}
