package com.app.platform.language.backend.writing

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

internal object WritingSubmissionsTable : Table("writing_submissions") {
  val id = uuid("id")
  val userId = uuid("user_id")
  val promptId = text("prompt_id")
  val text = text("text")
  val wordCount = integer("word_count")
  val status = text("status")
  val createdAt = timestampWithTimeZone("created_at")
  val updatedAt = timestampWithTimeZone("updated_at")
  override val primaryKey = PrimaryKey(id)
}
