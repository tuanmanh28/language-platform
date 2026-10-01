package com.app.platform.language.backend.writing

import org.jetbrains.exposed.v1.core.Table

internal object WritingPromptsTable : Table("writing_prompts") {
  val id = text("id")

  // Read-only mapping of the JSONB column; writes cast explicitly in WritingContentSeeder.
  val content = text("content")
  val version = integer("version")
  val visibility = text("visibility")
  val published = bool("published")
  override val primaryKey = PrimaryKey(id)
}
