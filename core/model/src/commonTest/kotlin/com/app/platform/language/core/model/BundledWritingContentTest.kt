package com.app.platform.language.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BundledWritingContentTest {
  private val prompts = BundledWritingPrompts.all

  @Test
  fun bundledPromptsCoverEveryTask() {
    assertEquals(WritingTask.entries.toSet(), prompts.map { it.task }.toSet())
  }

  @Test
  fun promptIdsAreUnique() {
    assertEquals(prompts.size, prompts.map { it.id }.toSet().size)
  }

  @Test
  fun taskOneMatchesItsModule() {
    prompts.forEach { prompt ->
      when (prompt.task) {
        WritingTask.TASK_1_ACADEMIC -> assertEquals(IeltsModule.ACADEMIC, prompt.module, prompt.id)
        WritingTask.TASK_1_GENERAL -> assertEquals(IeltsModule.GENERAL_TRAINING, prompt.module, prompt.id)
        WritingTask.TASK_2 -> Unit
      }
    }
  }

  @Test
  fun academicTaskOneHasARelativeImage() {
    prompts.filter { it.task == WritingTask.TASK_1_ACADEMIC }.forEach { prompt ->
      val imageUrl = assertNotNull(prompt.imageUrl, "${prompt.id}: academic Task 1 needs a chart or diagram")
      assertTrue(relativeImagePath.matches(imageUrl), "${prompt.id}: imageUrl must be relative to the image storage")
    }
  }

  @Test
  fun everyPromptHasInstructionsAndPositiveLimits() {
    prompts.forEach { prompt ->
      assertTrue(prompt.instructions.isNotBlank(), "${prompt.id}: missing instructions")
      assertTrue(prompt.minWords > 0, "${prompt.id}: minWords must be positive")
      assertTrue(prompt.timeLimitMinutes > 0, "${prompt.id}: timeLimitMinutes must be positive")
    }
  }

  private companion object {
    // Mirrors the imageUrl pattern in content/schema/writing-prompt.schema.json.
    val relativeImagePath = Regex("^[a-z0-9][a-z0-9._/-]*$")
  }
}
