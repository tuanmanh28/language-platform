package com.app.platform.language.backend.reading

import org.jetbrains.exposed.v1.core.Table

internal object ReadingTestsTable : Table("reading_tests") {
  val id = text("id")
  val module = text("module")
  val title = text("title")
  val timeLimitMinutes = integer("time_limit_minutes")

  // Read-only mapping of the JSONB column; writes cast explicitly in ReadingContentSeeder.
  val content = text("content")
  val version = integer("version")
  val published = bool("published")
  override val primaryKey = PrimaryKey(id)
}
