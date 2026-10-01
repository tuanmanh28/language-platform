package com.app.platform.language.core.model

import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal fun assertExplained(
  name: String,
  question: Question,
): Explanation {
  val explanation = assertNotNull(question.explanation, "$name: missing explanation")
  assertTrue(explanation.text.isNotBlank(), "$name: explanation text is blank")
  assertTrue(explanation.trap?.isNotBlank() ?: true, "$name: trap is blank")
  return explanation
}

internal fun assertParaphrasesMatch(
  name: String,
  question: Question,
  explanation: Explanation,
  source: String,
) {
  val questionWording = listOf(question.prompt) + question.options.map { it.text }
  explanation.paraphrases.forEach { paraphrase ->
    assertTrue(
      questionWording.any { paraphrase.inQuestion in it },
      "$name: '${paraphrase.inQuestion}' is not in the question or its options",
    )
    assertTrue(paraphrase.inSource in source, "$name: '${paraphrase.inSource}' is not verbatim in the source")
  }
}
