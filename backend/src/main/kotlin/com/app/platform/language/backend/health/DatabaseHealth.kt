package com.app.platform.language.backend.health

fun interface DatabaseHealth {
  suspend fun isReachable(): Boolean
}
