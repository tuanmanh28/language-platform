package com.app.platform.language.core.model

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExplanationSerializationTest {
  @Test
  fun legacyStringExplanationIsReadAsText() {
    val question = decodeQuestion(""""explanation": "Paragraph A says so."""")

    assertEquals(Explanation(text = "Paragraph A says so."), question.explanation)
  }

  @Test
  fun missingExplanationIsNull() {
    val question = decodeQuestion(""""options": []""")

    assertNull(question.explanation)
  }

  @Test
  fun passageEvidenceRoundTripsWithItsType() {
    val explanation =
      Explanation(
        text = "Đoạn B nói rõ điều này.",
        evidence = PassageEvidence(paragraphId = "B", quote = "the river froze"),
        paraphrases = listOf(Paraphrase(inQuestion = "became ice", inSource = "froze")),
        trap = "Đoạn A cũng nhắc tới dòng sông.",
      )

    val encoded = ContentJson.encodeToJsonElement(Question.serializer(), question(explanation))

    assertEquals("passage", encoded.evidenceType())
    assertEquals(question(explanation), ContentJson.decodeFromJsonElement(Question.serializer(), encoded))
  }

  @Test
  fun transcriptEvidenceRoundTripsWithItsType() {
    val explanation =
      Explanation(
        text = "Người nói chốt ngày thứ Sáu.",
        evidence = TranscriptEvidence(sectionNumber = 2, startSeconds = 10, endSeconds = 20, quote = "Friday is fine"),
      )

    val encoded = ContentJson.encodeToJsonElement(Question.serializer(), question(explanation))

    assertEquals("transcript", encoded.evidenceType())
    assertEquals(question(explanation), ContentJson.decodeFromJsonElement(Question.serializer(), encoded))
  }

  private fun decodeQuestion(extraField: String): Question =
    ContentJson.decodeFromString(
      Question.serializer(),
      """{ "id": "q1", "number": 1, "prompt": "Prompt", "acceptedAnswers": ["TRUE"], $extraField }""",
    )

  private fun question(explanation: Explanation) =
    Question(id = "q1", number = 1, prompt = "Prompt", acceptedAnswers = listOf("TRUE"), explanation = explanation)

  private fun JsonElement.evidenceType(): String =
    jsonObject
      .getValue("explanation")
      .jsonObject
      .getValue("evidence")
      .jsonObject
      .getValue("type")
      .jsonPrimitive.content
}
