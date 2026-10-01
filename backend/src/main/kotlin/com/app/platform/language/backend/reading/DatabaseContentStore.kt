package com.app.platform.language.backend.reading

import com.app.platform.language.backend.database.AppDatabase
import com.app.platform.language.core.model.ContentJson
import com.app.platform.language.core.model.ReadingTest
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll

class DatabaseContentStore(
  private val database: AppDatabase,
) : ContentStore {
  override suspend fun readingTests(): List<StoredReadingTest> =
    database.tx {
      ReadingTestsTable
        .selectAll()
        .where { ReadingTestsTable.published eq true }
        .orderBy(ReadingTestsTable.id)
        .map { it.toStoredReadingTest() }
    }

  override suspend fun readingTest(id: String): StoredReadingTest? =
    database.tx {
      ReadingTestsTable
        .selectAll()
        .where { (ReadingTestsTable.id eq id) and (ReadingTestsTable.published eq true) }
        .singleOrNull()
        ?.toStoredReadingTest()
    }

  private fun ResultRow.toStoredReadingTest() =
    StoredReadingTest(
      test = ContentJson.decodeFromString(ReadingTest.serializer(), this[ReadingTestsTable.content]),
      version = this[ReadingTestsTable.version],
    )
}
