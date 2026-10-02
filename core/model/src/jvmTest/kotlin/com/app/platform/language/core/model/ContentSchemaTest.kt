package com.app.platform.language.core.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class ContentSchemaTest {
  // Gradle runs JVM tests from the module directory.
  private val definitions =
    Json
      .parseToJsonElement(File("../../content/schema/reading-test.schema.json").readText())
      .jsonObject
      .getValue("\$defs")
      .jsonObject

  @Test
  fun questionSchemaDeclaresEveryModelField() {
    assertEquals(fieldsOf(Question.serializer()), propertiesOf("question").keys)
  }

  @Test
  fun explanationSchemaDeclaresEveryModelField() {
    assertEquals(fieldsOf(Explanation.serializer()), propertiesOf("explanation").keys)
    assertEquals(fieldsOf(Paraphrase.serializer()), propertiesOf("paraphrase").keys)
  }

  @Test
  fun evidenceSchemasDeclareTheirTypeAndEveryModelField() {
    listOf("passageEvidence" to PassageEvidence.serializer(), "transcriptEvidence" to TranscriptEvidence.serializer())
      .forEach { (definition, serializer) ->
        val properties = propertiesOf(definition)
        assertEquals(fieldsOf(serializer) + "type", properties.keys, definition)
        assertEquals(
          serializer.descriptor.serialName,
          properties
            .getValue("type")
            .jsonObject
            .getValue("const")
            .jsonPrimitive.content,
          definition,
        )
      }
  }

  private fun propertiesOf(definition: String): JsonObject =
    definitions
      .getValue(definition)
      .jsonObject
      .getValue("properties")
      .jsonObject

  private fun fieldsOf(serializer: KSerializer<*>): Set<String> =
    (0 until serializer.descriptor.elementsCount).map(serializer.descriptor::getElementName).toSet()
}
