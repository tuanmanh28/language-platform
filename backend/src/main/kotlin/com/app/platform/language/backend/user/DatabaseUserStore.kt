package com.app.platform.language.backend.user

import com.app.platform.language.backend.auth.AuthIdentity
import com.app.platform.language.backend.database.AppDatabase
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll

class DatabaseUserStore(
  private val database: AppDatabase,
) : UserStore {
  override suspend fun upsert(identity: AuthIdentity): User =
    database.tx {
      exec(
        UPSERT_SQL,
        listOf(
          UsersTable.firebaseUid.columnType to identity.uid,
          UsersTable.email.columnType to identity.email,
          UsersTable.displayName.columnType to identity.displayName,
        ),
      )
      UsersTable
        .selectAll()
        .where { UsersTable.firebaseUid eq identity.uid }
        .single()
        .toUser()
    }

  private fun ResultRow.toUser() =
    User(
      id = this[UsersTable.id],
      email = this[UsersTable.email],
      displayName = this[UsersTable.displayName],
    )

  private companion object {
    // Rows are only rewritten when the profile changed, so authenticated reads do not churn the table.
    val UPSERT_SQL =
      """
      INSERT INTO users (firebase_uid, email, display_name)
      VALUES (?, ?, ?)
      ON CONFLICT (firebase_uid) DO UPDATE SET
        email = EXCLUDED.email,
        display_name = EXCLUDED.display_name,
        updated_at = now()
      WHERE (users.email, users.display_name) IS DISTINCT FROM (EXCLUDED.email, EXCLUDED.display_name)
      """.trimIndent()
  }
}
