package com.app.platform.language.backend.listening

import org.jetbrains.exposed.v1.core.Table

internal object ListeningTestsTable : Table("listening_tests") {
  val id = text("id")
  val title = text("title")

  // Read-only mapping of the JSONB column; writes cast explicitly in ListeningContentSeeder.
  val content = text("content")
  val version = integer("version")
  val visibility = text("visibility")
  val published = bool("published")
  override val primaryKey = PrimaryKey(id)
}
