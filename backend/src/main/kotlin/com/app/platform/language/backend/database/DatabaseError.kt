package com.app.platform.language.backend.database

sealed interface DatabaseError {
  val message: String
  val cause: Throwable

  data class Unreachable(
    override val cause: Throwable,
  ) : DatabaseError {
    override val message = "Database is unreachable"
  }

  data class MigrationFailed(
    override val cause: Throwable,
  ) : DatabaseError {
    override val message = "Database migration failed"
  }
}
