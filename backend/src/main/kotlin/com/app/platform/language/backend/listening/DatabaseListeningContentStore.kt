package com.app.platform.language.backend.listening

import com.app.platform.language.backend.content.Visibility
import com.app.platform.language.backend.database.AppDatabase
import com.app.platform.language.core.model.ContentJson
import com.app.platform.language.core.model.ListeningTest
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll

class DatabaseListeningContentStore(
  private val database: AppDatabase,
) : ListeningContentStore {
  override suspend fun listeningTests(): List<StoredListeningTest> =
    database.tx {
      ListeningTestsTable
        .selectAll()
        .where { ListeningTestsTable.published eq true }
        .orderBy(ListeningTestsTable.id)
        .map { it.toStoredListeningTest() }
    }

  override suspend fun listeningTest(id: String): StoredListeningTest? =
    database.tx {
      ListeningTestsTable
        .selectAll()
        .where { (ListeningTestsTable.id eq id) and (ListeningTestsTable.published eq true) }
        .singleOrNull()
        ?.toStoredListeningTest()
    }

  private fun ResultRow.toStoredListeningTest() =
    StoredListeningTest(
      test = ContentJson.decodeFromString(ListeningTest.serializer(), this[ListeningTestsTable.content]),
      version = this[ListeningTestsTable.version],
      visibility = Visibility.fromId(this[ListeningTestsTable.visibility]),
    )
}
