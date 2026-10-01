package com.app.platform.language.core.exam

import com.app.platform.language.core.model.QuestionType

object AnswerNormalizer {
  private val whitespace = Regex("\\s+")
  private const val EDGE_PUNCTUATION = ".,;:!?\"'()[]"

  fun normalize(
    raw: String,
    type: QuestionType,
  ): String {
    val basic =
      raw
        .trim()
        .lowercase()
        .replace(whitespace, " ")
        .trim { it in EDGE_PUNCTUATION || it.isWhitespace() }

    return when (type) {
      QuestionType.TRUE_FALSE_NOT_GIVEN -> {
        when (basic) {
          "t", "true" -> "true"
          "f", "false" -> "false"
          "ng", "not given", "notgiven", "not-given" -> "not given"
          else -> basic
        }
      }

      QuestionType.YES_NO_NOT_GIVEN -> {
        when (basic) {
          "y", "yes" -> "yes"
          "n", "no" -> "no"
          "ng", "not given", "notgiven", "not-given" -> "not given"
          else -> basic
        }
      }

      QuestionType.MULTIPLE_CHOICE,
      QuestionType.SENTENCE_COMPLETION,
      -> {
        basic
      }
    }
  }

  fun wordCount(normalized: String): Int = if (normalized.isBlank()) 0 else normalized.split(' ').size
}
