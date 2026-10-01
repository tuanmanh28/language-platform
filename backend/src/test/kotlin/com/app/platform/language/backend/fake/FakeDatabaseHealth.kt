package com.app.platform.language.backend.fake

import com.app.platform.language.backend.health.DatabaseHealth

class FakeDatabaseHealth(
  private val isUp: Boolean = true,
) : DatabaseHealth {
  override suspend fun isReachable(): Boolean = isUp
}
