package com.app.platform.language.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WritingPrompt(
  val id: String,
  val schemaVersion: Int = 1,
  val module: IeltsModule,
  val task: WritingTask,
  val instructions: String,
  // A path relative to the image storage in content files; the API turns it into an absolute URL.
  val imageUrl: String? = null,
  val minWords: Int,
  val timeLimitMinutes: Int,
)

@Serializable
enum class WritingTask {
  @SerialName("task_1_academic")
  TASK_1_ACADEMIC,

  @SerialName("task_1_general")
  TASK_1_GENERAL,

  @SerialName("task_2")
  TASK_2,
}
