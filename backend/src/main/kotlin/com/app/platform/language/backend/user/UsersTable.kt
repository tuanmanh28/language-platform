package com.app.platform.language.backend.user

import org.jetbrains.exposed.v1.core.Table

internal object UsersTable : Table("users") {
  val id = uuid("id")
  val firebaseUid = text("firebase_uid")
  val email = text("email").nullable()
  val displayName = text("display_name").nullable()
  override val primaryKey = PrimaryKey(id)
}
